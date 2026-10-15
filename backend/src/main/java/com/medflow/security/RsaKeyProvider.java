package com.medflow.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class RsaKeyProvider {

    // Chaves padrão seguras para desenvolvimento e testes (RSA 2048 bits)
    private static final String DEFAULT_DEV_PRIVATE_BASE64 =
            "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDWEMndNMxOMdDIsPy+8vW/cHKTcXPSJQDUZFHx3thdIWDXnm97" +
            "voSxRCsawCi0sJP240YmJPnMe4gP0hbiTFHRkSRZ2F9NFmeJ8WqjblAONUlWdQwa7Wh4P+s5is5M/Ew5iZdKAq1BY89KScEnpL+p" +
            "3zcxi0i3FxVgh9G3kso/bGZK1IbsAcj4idDen0sNvQTY5C1W8jdfkAKxWZm7top8VJsQme9nZfUtNPHQtMgIb1k41sYWa5TYuHut" +
            "730Fjk5Uir23Mtvi11JaWf9apGKIO+ai08MIdwmrvT8yhrrrE1MD3lBuG8zaAsqfkVH2TUQ5AjO2DBvx08GKdwRczvPRAgMBAAEC" +
            "ggEAWmBKKZdB7n1/rUtz2dZPxJxf8zoB8SXoPMOCCUDRTjhqX34niTH9aa3jGO8qAGdQgPqhQjd4M0mwPyWuvaoFHRpPI5ODYNNl" +
            "sUp4gDlxmR4MMXFHRBMZeTNbRzcANGaMc/pQIlIQ/bt6591NrEQVG03/Sm6Iq6wWPWP6OavCsZV3Ych67NigQIWcPzA/Arc5z+xj" +
            "a6yZvFqyrU+clmwDBStDWN/DppiZ7GuD62CIO7/JjYneRJefgyBzhxu4BFFFzetJTl1YLvKgVaPFX4GzquJyHnf5uV+42sgVpZ+F" +
            "lCyrA24DyIKcrkO2rPdyduukkxdYkh1UHCGET7oTIBCdxQKBgQD5t+jLMbJOLPlmGc8Xf2uLzbJl8L4uVPWK7vA8+clcDUwmeSzu" +
            "HcO5APDFk1ZiXvggbspmisCIZvvSMG/OUGMrLZtNOLnaA9HRPTvqt79bxFUR+NqswPe22Z12iVnlxb3DwtEzP43HjUMGS7mTsNwb" +
            "YjsOWVPKLz63haaH8LyPMwKBgQDbc0nqGy98CviP9JP3r2IvzTr7UBvWQZ9Vjcg+slvTIYr/6QxG//cSvcDq4gko792hyjFF3cq+" +
            "7BH/pE9BTxhfZp4Z2aD/6DE1k3iYVtmPsV5QYrYgeNXbhvDq1QBa8faqvdhWWLap+z4WktSvnx93jNgUpf1GMMeblPhH/2qA6wKB" +
            "gQDyUZcRjgDzQUsl24eMyrWmrQ0fRLSXqxO2JNR0pwLRU4oAj3ec/L8PSC1b2/RocrWea0P2q0/XTO+x8BSehH0myhO4pIIcX7Z7" +
            "r0r0IigLuYG/dddk6b+27IaTHwdgIzlYvDRa4aIH8TkH4/WJroZ4ACNEckQPnzbhyUxm/6FJ5wKBgQCLmwA+qiS/KMuP6Ovo+e3g" +
            "sxL+pavP1ddjOdtV52l7Gy+uNbfVwyiPB6AezMU/AblhH5vQ8pP0eYHJu+Gfjo3WwxV5JHXD1xbXGoXxg+7L9u4k/WcXqUeDbIj/" +
            "G5oqQRvG6xchdzZdVLPe8HHVd8kw6KzeFerWKFoTo+V/CAHdGQKBgQCV85/PvfZohUDTIEKwGRKG8jKtgM+agmBgxuPTbiKSg+Li" +
            "UDozS3vp2vNbpObBXCHDRNNAQsnUXIY0IqWqr2kQYQZOB2FWi9RXQBXcvN253i9MENdjmn11AB1Jw3ozTfZYk6v4gJIIO+xxy2BI" +
            "5+SrTSOx+cs/IstCNp3wgg5obg==";

    private static final String DEFAULT_DEV_PUBLIC_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1hDJ3TTMTjHQyLD8vvL1v3Byk3Fz0iUA1GRR8d7YXSFg155ve76EsUQr" +
            "GsAotLCT9uNGJiT5zHuID9IW4kxR0ZEkWdhfTRZnifFqo25QDjVJVnUMGu1oeD/rOYrOTPxMOYmXSgKtQWPPSknBJ6S/qd83MYtI" +
            "txcVYIfRt5LKP2xmStSG7AHI+InQ3p9LDb0E2OQtVvI3X5ACsVmZu7aKfFSbEJnvZ2X1LTTx0LTICG9ZONbGFmuU2Lh7re99BY5O" +
            "VIq9tzLb4tdSWln/WqRiiDvmotPDCHcJq70/Moa66xNTA95QbhvM2gLKn5FR9k1EOQIztgwb8dPBincEXM7z0QIDAQAB";

    @Value("${jwt.rsa.private-key:}")
    private String privateKeyConfig;

    @Value("${jwt.rsa.public-key:}")
    private String publicKeyConfig;

    @Getter
    @Value("${jwt.rsa.kid:medflow-key-2026-v1}")
    private String keyId = "medflow-key-2026-v1";

    @Getter
    private RSAPrivateKey privateKey;

    @Getter
    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            String privBase64 = (privateKeyConfig != null && !privateKeyConfig.isBlank())
                    ? cleanPem(privateKeyConfig)
                    : DEFAULT_DEV_PRIVATE_BASE64;

            String pubBase64 = (publicKeyConfig != null && !publicKeyConfig.isBlank())
                    ? cleanPem(publicKeyConfig)
                    : DEFAULT_DEV_PUBLIC_BASE64;

            byte[] privBytes = Base64.getDecoder().decode(privBase64);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
            this.privateKey = (RSAPrivateKey) kf.generatePrivate(privSpec);

            byte[] pubBytes = Base64.getDecoder().decode(pubBase64);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
            this.publicKey = (RSAPublicKey) kf.generatePublic(pubSpec);

            log.info("[RSA KEY PROVIDER] Par de chaves RSA 2048 carregado com sucesso (kid: {})", keyId);
        } catch (Exception e) {
            log.error("[RSA KEY PROVIDER] Falha crítica ao inicializar chaves RSA: {}", e.getMessage(), e);
            throw new IllegalStateException("Não foi possível carregar o par de chaves RSA para JWT", e);
        }
    }

    public String getPublicKeyPem() {
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" +
                base64.replaceAll("(.{64})", "$1\n") +
                "\n-----END PUBLIC KEY-----\n";
    }

    private String cleanPem(String pem) {
        return pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
