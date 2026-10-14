package com.medflow.service;

import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.entity.Especialidade;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.repository.EspecialidadeRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportacaoCsvServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EspecialidadeRepository especialidadeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ImportacaoCsvService importacaoCsvService;

    @Test
    @DisplayName("Importação de médicos com CSV válido deve ter 100% de sucesso")
    void testImportarMedicosCsvValido() {
        String csv = """
                nome,email,crm,especialidade
                Dr. Teste Silva,teste.silva@medflow.com,CRM/SP 123456,Cardiologia
                Dra. Ana Santos,ana.santos@medflow.com,CRM/SP 234567,Pediatria
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(medicoRepository.findByCrm(any())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(especialidadeRepository.findByNomeIgnoreCase(any())).thenReturn(Optional.of(new Especialidade()));
        when(medicoRepository.save(any(Medico.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        ImportacaoResultadoDTO resultado = importacaoCsvService.importarMedicosCsv(stream);

        assertNotNull(resultado);
        assertEquals(2, resultado.processadasComSucesso());
        assertEquals(0, resultado.totalFalhas());
        assertTrue(resultado.erros().isEmpty());
    }

    @Test
    @DisplayName("Importação de médicos com linhas malformadas deve gerar relatório de erros detalhado por linha")
    void testImportarMedicosCsvComErros() {
        String csv = """
                nome,email,crm,especialidade
                Dr. Valido,valido@medflow.com,CRM/SP 111111,Neurologia
                Linha Malformada Sem Colunas
                Dr. Email Invalido,email_sem_arroba,CRM/SP 222222,Clinica
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(medicoRepository.findByCrm("CRM/SP 111111")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("valido@medflow.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(especialidadeRepository.findByNomeIgnoreCase(any())).thenReturn(Optional.of(new Especialidade()));
        when(medicoRepository.save(any(Medico.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        ImportacaoResultadoDTO resultado = importacaoCsvService.importarMedicosCsv(stream);

        assertNotNull(resultado);
        assertEquals(1, resultado.processadasComSucesso());
        assertEquals(2, resultado.totalFalhas());
        assertEquals(2, resultado.erros().size());
        assertEquals(3, resultado.erros().get(0).linha()); // Linha 3 (1-indexed considerando cabeçalho na linha 1)
    }

    @Test
    @DisplayName("Importação de pacientes com CSV válido")
    void testImportarPacientesCsvValido() {
        String csv = """
                nome,email,cpf,dataNascimento
                Fernanda Alves,fernanda@email.com,12345678901,1990-01-01
                Lucas Santos,lucas@email.com,23456789012,15/05/1985
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(pacienteRepository.findByCpf(any())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        ImportacaoResultadoDTO resultado = importacaoCsvService.importarPacientesCsv(stream);

        assertNotNull(resultado);
        assertEquals(2, resultado.processadasComSucesso());
        assertEquals(0, resultado.totalFalhas());
    }

    @Test
    @DisplayName("Importação de pacientes com CPF inválido e data incorreta")
    void testImportarPacientesCsvComErros() {
        String csv = """
                nome,email,cpf,dataNascimento
                Paciente CPF Curto,curto@email.com,1234,1990-01-01
                Paciente Data Errada,data@email.com,12345678901,data-invalida
                """;
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ImportacaoResultadoDTO resultado = importacaoCsvService.importarPacientesCsv(stream);

        assertNotNull(resultado);
        assertEquals(0, resultado.processadasComSucesso());
        assertEquals(2, resultado.totalFalhas());
    }
}
