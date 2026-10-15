package com.medflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String DEFAULT_DEV_PUBLIC_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1hDJ3TTMTjHQyLD8vvL1v3Byk3Fz0iUA1GRR8d7YXSFg155ve76EsUQr" +
            "GsAotLCT9uNGJiT5zHuID9IW4kxR0ZEkWdhfTRZnifFqo25QDjVJVnUMGu1oeD/rOYrOTPxMOYmXSgKtQWPPSknBJ6S/qd83MYtI" +
            "txcVYIfRt5LKP2xmStSG7AHI+InQ3p9LDb0E2OQtVvI3X5ACsVmZu7aKfFSbEJnvZ2X1LTTx0LTICG9ZONbGFmuU2Lh7re99BY5O" +
            "VIq9tzLb4tdSWln/WqRiiDvmotPDCHcJq70/Moa66xNTA95QbhvM2gLKn5FR9k1EOQIztgwb8dPBincEXM7z0QIDAQAB";

    @Value("${jwt.rsa.public-key:}")
    private String publicKeyConfig;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private RSAPublicKey rsaPublicKey;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/",
            "/auth/",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @PostConstruct
    public void init() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            String pubBase64 = (publicKeyConfig != null && !publicKeyConfig.isBlank())
                    ? cleanPem(publicKeyConfig)
                    : DEFAULT_DEV_PUBLIC_BASE64;

            byte[] pubBytes = Base64.getDecoder().decode(pubBase64);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
            this.rsaPublicKey = (RSAPublicKey) kf.generatePublic(pubSpec);
            log.info("[GATEWAY JWT] Validação assimétrica RS256 inicializada com sucesso na borda.");
        } catch (Exception e) {
            log.error("[GATEWAY JWT] Erro ao carregar chave pública RSA no Gateway: {}", e.getMessage(), e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Se for endpoint público ou OPTIONS (preflight CORS), libera
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS") || isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Tentativa de acesso não autenticado na borda (Gateway): {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = parseClaims(token);

            // Propaga o usuário autenticado para os microserviços internos no header
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Authenticated-User", claims.getSubject())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception ex) {
            log.error("Token JWT inválido ou expirado na borda do Gateway: {}", ex.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private Claims parseClaims(String token) {
        if (rsaPublicKey != null) {
            try {
                return Jwts.parser()
                        .verifyWith(rsaPublicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ex) {
                // Fallback legado durante transição se chave simétrica configurada
                if (jwtSecret != null && !jwtSecret.isBlank()) {
                    try {
                        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                        return Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                    } catch (Exception ignored) {
                    }
                }
                throw ex;
            }
        } else if (jwtSecret != null && !jwtSecret.isBlank()) {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
        throw new IllegalStateException("Nenhuma chave de validação configurada no Gateway.");
    }

    private String cleanPem(String pem) {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
