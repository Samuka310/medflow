package com.medflow.logging;

import java.util.regex.Pattern;

/**
 * Utilitário de Higienização e Mascaramento de Dados Pessoais Identificáveis (PII) e Dados Sensíveis de Saúde.
 * Em conformidade com a LGPD (Lei 13.709/2018, Art. 5º II e Art. 11) e normas do CFM,
 * assegura que CPFs, e-mails, telefones, credenciais e anotações clínicas sensíveis
 * nunca sejam expostos em texto claro nos logs do sistema.
 */
public final class PiiMasker {

    private PiiMasker() {
    }

    // Padrão de CPF formatado (ex: 123.456.789-00 -> ***.***.789-**)
    private static final Pattern CPF_FORMATTED_PATTERN = Pattern.compile(
            "\\b\\d{3}\\.\\d{3}\\.(\\d{3})-(\\d{2})\\b"
    );

    // Padrão de CPF em chave-valor (ex: cpf=12345678900 ou cpf: "12345678900")
    private static final Pattern CPF_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b(cpf|documento)\\s*[:=]\\s*['\"]?\\d{3}\\.?\\d{3}\\.?(\\d{3})[-.]?\\d{2}['\"]?\\b"
    );

    // Padrão de E-mail (ex: paciente.silva@hospital.com -> p***@hospital.com)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b"
    );

    // Padrão de Telefone com DDD (ex: (11) 98765-4321 ou 11987654321)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?i)\\b(telefone|celular|phone|tel)\\s*[:=]\\s*['\"]?(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)?(?:9\\s?)?\\d{4}[-\\s]?\\d{4}['\"]?\\b"
    );

    // Padrão de Senhas, Tokens e Credenciais
    private static final Pattern CREDENTIALS_PATTERN = Pattern.compile(
            "(?i)\\b(password|senha|secret|token|bearer|authorization)\\s*[:=]\\s*['\"]?([^\\s,;'\"}{]+)['\"]?"
    );

    // Padrão de Cartão Nacional de Saúde (CNS / SUS - 15 dígitos)
    private static final Pattern CNS_PATTERN = Pattern.compile(
            "(?i)\\b(cns|cartao_sus|sus)\\s*[:=]\\s*['\"]?\\d{15}['\"]?\\b"
    );

    // Padrão de Dados Clínicos Sensíveis em anotações / payloads de log
    private static final Pattern CLINICAL_DATA_PATTERN = Pattern.compile(
            "(?i)\\b(diagnostico|hipotese|prescricao|medicamento|queixa_principal|sintoma)\\s*[:=]\\s*['\"]?([^\\r\\n,;'\"}{]+)['\"]?"
    );

    /**
     * Aplica mascaramento de segurança PII em uma string de mensagem de log.
     *
     * @param message Mensagem bruta de log
     * @return Mensagem higienizada com dados sensíveis mascarados
     */
    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = message;

        // 1. Mascarar credenciais e tokens (prioridade máxima)
        masked = CREDENTIALS_PATTERN.matcher(masked).replaceAll("$1=******");

        // 2. Mascarar CPFs (formatados e chave-valor)
        masked = CPF_FORMATTED_PATTERN.matcher(masked).replaceAll("***.***.$1-**");
        masked = CPF_KEY_VALUE_PATTERN.matcher(masked).replaceAll("$1=***.***.$2-**");

        // 3. Mascarar E-mails
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("$1***@$2");

        // 4. Mascarar Telefones
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1=(xx) xxxxx-****");

        // 5. Mascarar CNS (Cartão SUS)
        masked = CNS_PATTERN.matcher(masked).replaceAll("$1=***.***.***.***");

        // 6. Mascarar Dados Clínicos diretos
        masked = CLINICAL_DATA_PATTERN.matcher(masked).replaceAll("$1=[DADO CLINICO PROTEGIDO]");

        return masked;
    }
}
