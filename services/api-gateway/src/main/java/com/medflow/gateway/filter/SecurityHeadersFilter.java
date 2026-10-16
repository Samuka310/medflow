package com.medflow.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Filtro de Borda de Hardening e Injeção de Cabeçalhos de Segurança OWASP.
 * - Injeta HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy e Permissions-Policy.
 * - Bloqueia acesso externo não autorizado a endpoints sensíveis do Actuator (/actuator/env, /beans, etc.).
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Hardening do Actuator na borda: Permite apenas /actuator/health e /actuator/info
        if (path.startsWith("/actuator") && !path.startsWith("/actuator/health") && !path.equals("/actuator/info")) {
            return blockSensitiveActuatorAccess(exchange);
        }

        // Injeção de Cabeçalhos de Segurança OWASP antes de enviar a resposta ao cliente
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // HTTP Strict Transport Security (HSTS) - Força HTTPS por 1 ano incluindo subdomínios
            headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

            // Prevenção contra MIME-sniffing
            headers.set("X-Content-Type-Options", "nosniff");

            // Prevenção contra Clickjacking
            headers.set("X-Frame-Options", "DENY");

            // Desativação de filtro XSS legado (OWASP recomenda desativar para evitar bypasses e usar CSP)
            headers.set("X-XSS-Protection", "0");

            // Política de Referência segura
            headers.set("Referrer-Policy", "strict-origin-when-cross-origin");

            // Content Security Policy (CSP) restritivo
            headers.set("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; object-src 'none';");

            // Restrição de APIs do navegador
            headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private Mono<Void> blockSensitiveActuatorAccess(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        String errorBody = """
                {
                  "type": "https://medflow.com/erros/acesso-negado",
                  "title": "Acesso Restrito",
                  "status": 403,
                  "detail": "O endpoint interno solicitado está restrito à rede de monitoramento e não é acessível externamente.",
                  "timestamp": "%s"
                }
                """.formatted(Instant.now().toString());

        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Executa imediatamente após o CorrelationIdFilter para garantir headers em todas as respostas
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
