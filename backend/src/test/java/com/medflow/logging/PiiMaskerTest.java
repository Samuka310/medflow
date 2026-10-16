package com.medflow.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PiiMaskerTest {

    @Test
    @DisplayName("PiiMasker: deve mascarar CPF formatado preservando apenas dígitos de verificação contextual")
    void testMascaraCpfFormatado() {
        String input = "O paciente com CPF 123.456.789-00 deu entrada na emergência.";
        String expected = "O paciente com CPF ***.***.789-** deu entrada na emergência.";

        String result = PiiMasker.mask(input);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar CPF em padrão chave-valor sem pontuação")
    void testMascaraCpfChaveValor() {
        String input = "Identificando paciente: cpf=12345678900 para atendimento.";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("cpf=***.***.789-**"));
        assertFalse(result.contains("12345678900"));
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar endereços de e-mail preservando domínio e primeira letra")
    void testMascaraEmail() {
        String input = "Notificação enviada com sucesso para paciente.silva@hospital.med.br e doutor@clinica.com";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("p***@hospital.med.br"));
        assertTrue(result.contains("d***@clinica.com"));
        assertFalse(result.contains("paciente.silva"));
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar telefones e celulares com DDD")
    void testMascaraTelefone() {
        String input = "Contato de emergência informado: telefone=(11) 98765-4321";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("telefone=(xx) xxxxx-****"));
        assertFalse(result.contains("98765-4321"));
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar senhas, tokens e credenciais")
    void testMascaraCredenciais() {
        String input = "Tentativa de login: password=SegredoSuperForte123! token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.xyz";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("password=******"));
        assertTrue(result.contains("token=******"));
        assertFalse(result.contains("SegredoSuperForte123!"));
        assertFalse(result.contains("eyJhbGciOiJSUzI1Ni"));
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar número de Cartão Nacional de Saúde (CNS)")
    void testMascaraCns() {
        String input = "Verificação SUS: cns=123456789012345 na base nacional";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("cns=***.***.***.***"));
        assertFalse(result.contains("123456789012345"));
    }

    @Test
    @DisplayName("PiiMasker: deve mascarar anotações e termos clínicos sensíveis")
    void testMascaraDadosClinicos() {
        String input = "Atualização de prontuário: diagnostico=Hipertensão Arterial Sistêmica Severa; prescricao=Morfina 10mg IV";
        String result = PiiMasker.mask(input);

        assertTrue(result.contains("diagnostico=[DADO CLINICO PROTEGIDO]"));
        assertTrue(result.contains("prescricao=[DADO CLINICO PROTEGIDO]"));
        assertFalse(result.contains("Hipertensão Arterial"));
        assertFalse(result.contains("Morfina 10mg"));
    }

    @Test
    @DisplayName("PiiMasker: deve manter intactas mensagens normais que não contenham PII")
    void testMantemMensagemNormalIntacta() {
        String input = "Serviço de agendamento inicializado com sucesso na porta 8081.";
        String result = PiiMasker.mask(input);

        assertEquals(input, result);
    }

    @Test
    @DisplayName("PiiMaskingConverter: deve delegar corretamente para o PiiMasker")
    void testPiiMaskingConverter() {
        PiiMaskingConverter converter = new PiiMaskingConverter();
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("Usuário medico@medflow.com autenticado com CPF 111.222.333-44");

        String converted = converter.convert(event);

        assertTrue(converted.contains("m***@medflow.com"));
        assertTrue(converted.contains("***.***.333-**"));
    }

    @Test
    @DisplayName("PiiJsonValueMasker: deve sanitizar valores de strings na serialização JSON")
    void testPiiJsonValueMasker() {
        PiiJsonValueMasker masker = new PiiJsonValueMasker();

        Object maskedString = masker.mask(null, "E-mail do titular: usuario@hospital.com");
        assertEquals("E-mail do titular: u***@hospital.com", maskedString);

        Object nonString = masker.mask(null, 12345);
        assertEquals(12345, nonString);
    }
}
