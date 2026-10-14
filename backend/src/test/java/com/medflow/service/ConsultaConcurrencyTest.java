package com.medflow.service;

import com.medflow.config.ConsultaProperties;
import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.exception.ConflitoAgendaException;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.TriagemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaConcurrencyTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private TriagemRepository triagemRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ConsultaProperties consultaProperties;

    @InjectMocks
    private ConsultaService consultaService;

    @Test
    @DisplayName("Garante que N threads tentando agendar o mesmo horário para o mesmo médico resultam em exatamente 1 sucesso")
    void testAgendamentoConcorrenteMultiplasThreads() throws InterruptedException {
        int totalThreads = 10;
        UUID medicoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        LocalDateTime dataHora = LocalDateTime.now().plusDays(2);

        Medico medico = new Medico();
        medico.setId(medicoId);

        Paciente paciente = new Paciente();
        paciente.setId(pacienteId);
        Usuario usuario = new Usuario();
        usuario.setNome("Paciente Concorrente");
        paciente.setUsuario(usuario);

        lenient().when(consultaProperties.duracaoMinutos()).thenReturn(60);
        lenient().when(consultaProperties.valorPadrao()).thenReturn(new BigDecimal("150.00"));
        lenient().when(medicoRepository.findByIdWithLock(medicoId)).thenReturn(Optional.of(medico));
        lenient().when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));

        // Simula a garantia atômica do banco de dados (Pessimistic lock + Unique index parcial uk_medico_data_hora_ativa)
        AtomicBoolean slotOcupado = new AtomicBoolean(false);

        when(consultaRepository.existsConflitoMedico(any(), any(), any(), any()))
                .thenAnswer(invocation -> slotOcupado.get());
        when(consultaRepository.existsConflitoPaciente(any(), any(), any(), any()))
                .thenReturn(false);

        when(consultaRepository.save(any(Consulta.class))).thenAnswer(invocation -> {
            // Se outra thread já garantiu o slot atômico no banco, lança erro de integridade de chave única
            if (!slotOcupado.compareAndSet(false, true)) {
                throw new DataIntegrityViolationException("Violação de chave única: uk_medico_data_hora_ativa");
            }
            Consulta c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger conflitos = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Todas as threads aguardam o sinal para disparar juntas
                    ConsultaDTO dto = new ConsultaDTO();
                    dto.setMedicoId(medicoId);
                    dto.setPacienteId(pacienteId);
                    dto.setDataHora(dataHora);

                    consultaService.agendar(dto);
                    sucessos.incrementAndGet();
                } catch (ConflitoAgendaException e) {
                    conflitos.incrementAndGet();
                } catch (Exception e) {
                    // Outras exceções não esperadas
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Dispara todas as 10 threads simultaneamente
        startLatch.countDown();
        boolean finished = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Asserções de integridade e concorrência
        assertEquals(true, finished, "Todas as threads devem concluir no tempo limite");
        assertEquals(1, sucessos.get(), "Exatamente 1 agendamento deve ter sucesso");
        assertEquals(totalThreads - 1, conflitos.get(), "As demais 9 tentativas devem falhar com ConflitoAgendaException");
    }
}
