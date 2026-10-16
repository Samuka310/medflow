package com.medflow.service;

import com.medflow.config.ConsultaProperties;
import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Pagamento;
import com.medflow.entity.enums.ConsultaStatus;
import com.medflow.entity.enums.FormaPagamento;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.entity.enums.PrioridadeTriagem;
import com.medflow.event.ConsultaEvento;
import com.medflow.exception.ConflitoAgendaException;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.metrics.MedflowMetricsService;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.TriagemRepository;
import com.medflow.strategy.cancelamento.PoliticaCancelamentoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final PagamentoRepository pagamentoRepository;
    private final TriagemRepository triagemRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ConsultaProperties consultaProperties;
    private final PoliticaCancelamentoContext politicaCancelamentoContext;
    private final MedflowMetricsService medflowMetricsService;

    @Transactional
    public ConsultaDTO agendar(ConsultaDTO dto) {
        if (dto.getDataHora().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RegraDeNegocioException("A consulta deve ser agendada com no mínimo 30 minutos de antecedência.");
        }

        // Lock pessimista para serializar agendamentos concorrentes do mesmo médico
        Medico medico = medicoRepository.findByIdWithLock(dto.getMedicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Médico não encontrado."));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente não encontrado."));

        int duracao = consultaProperties.duracaoMinutos();
        LocalDateTime inicio = dto.getDataHora().minusMinutes(duracao - 1);
        LocalDateTime fim = dto.getDataHora().plusMinutes(duracao - 1);

        if (consultaRepository.existsConflitoMedico(medico.getId(), inicio, fim, null)) {
            throw new ConflitoAgendaException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsConflitoPaciente(paciente.getId(), inicio, fim, null)) {
            throw new ConflitoAgendaException("O paciente já possui uma consulta neste horário.");
        }

        Consulta consulta = new Consulta();
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setDataHora(dto.getDataHora());
        consulta.setStatus(ConsultaStatus.AGENDADA);

        try {
            consulta = consultaRepository.save(consulta);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoAgendaException("Conflito de integridade: horário indisponível para o médico.");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setConsulta(consulta);
        BigDecimal valor = (medico.getEspecialidades() == null || medico.getEspecialidades().isEmpty() || medico.getEspecialidades().get(0).getValorConsulta() == null)
            ? consultaProperties.valorPadrao()
            : medico.getEspecialidades().get(0).getValorConsulta();
        pagamento.setValor(valor);
        pagamento.setStatus(PagamentoStatus.PENDENTE);
        pagamento.setFormaPagamento(FormaPagamento.CARTAO);
        pagamentoRepository.save(pagamento);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
        medflowMetricsService.registrarConsultaAgendada();

        return mapToDTO(consulta);
    }

    @Transactional
    public ConsultaDTO remarcar(UUID id, LocalDateTime novaDataHora) {
        if (novaDataHora.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RegraDeNegocioException("A remarcação deve ser feita com no mínimo 30 minutos de antecedência.");
        }

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getStatus() != ConsultaStatus.AGENDADA) {
            throw new RegraDeNegocioException("Somente consultas AGENDADAS podem ser remarcadas.");
        }

        medicoRepository.findByIdWithLock(consulta.getMedico().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Médico não encontrado."));

        int duracao = consultaProperties.duracaoMinutos();
        LocalDateTime inicio = novaDataHora.minusMinutes(duracao - 1);
        LocalDateTime fim = novaDataHora.plusMinutes(duracao - 1);

        if (consultaRepository.existsConflitoMedico(consulta.getMedico().getId(), inicio, fim, consulta.getId())) {
            throw new ConflitoAgendaException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsConflitoPaciente(consulta.getPaciente().getId(), inicio, fim, consulta.getId())) {
            throw new ConflitoAgendaException("O paciente já possui uma consulta neste horário.");
        }

        consulta.setDataHora(novaDataHora);
        try {
            consulta = consultaRepository.save(consulta);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoAgendaException("Conflito de integridade: horário indisponível para o médico.");
        }

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));

        return mapToDTO(consulta);
    }

    @Transactional
    public void confirmar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getStatus() != ConsultaStatus.AGENDADA) {
            throw new RegraDeNegocioException("Somente consultas AGENDADAS podem ser confirmadas.");
        }

        consulta.confirmar();
        consultaRepository.save(consulta);
        medflowMetricsService.registrarConsultaConfirmada();

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    @Transactional
    public void checkin(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getStatus() == ConsultaStatus.CANCELADA || consulta.getStatus() == ConsultaStatus.REALIZADA) {
            throw new RegraDeNegocioException("Status inválido para check-in.");
        }

        consulta.aguardarTriagem();
        consultaRepository.save(consulta);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    @Transactional
    public void cancelar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));

        // Aplica a política de cancelamento/reembolso via Strategy Pattern
        politicaCancelamentoContext.processarCancelamento(consulta, LocalDateTime.now());

        consulta.cancelar();
        consultaRepository.save(consulta);
        medflowMetricsService.registrarConsultaCancelada("CANCELAMENTO_SOLICITADO");

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    @Transactional
    public void realizar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getStatus() == ConsultaStatus.CANCELADA) {
            throw new RegraDeNegocioException("Não é possível realizar uma consulta cancelada.");
        }

        Pagamento pagamento = pagamentoRepository.findByConsultaId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));

        if (pagamento.getStatus() != PagamentoStatus.PAGO) {
            throw new RegraDeNegocioException("A consulta não pode ser realizada sem o pagamento estar PAGO.");
        }

        consulta.realizar();
        consultaRepository.save(consulta);
        medflowMetricsService.registrarConsultaRealizada();
    }

    public List<ConsultaDTO> findAll() {
        return consultaRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ConsultaDTO> hoje() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Consulta> consultas = consultaRepository.findByDataHoraBetween(startOfDay, endOfDay);
        consultas.sort((c1, c2) -> c1.getDataHora().compareTo(c2.getDataHora()));

        return consultas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<ConsultaDTO> agendaHoje(String email) {
        Medico medico = medicoRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Médico não encontrado pelo usuário logado."));

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Consulta> consultas = consultaRepository.findByMedicoIdAndDataHoraBetween(medico.getId(), startOfDay, endOfDay);
        if (consultas.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> consultaIds = consultas.stream().map(Consulta::getId).toList();

        // Carregamento em lote de triagens (1 query única)
        Map<UUID, Integer> priorityMap = triagemRepository.findByConsultaIdIn(consultaIds).stream()
                .filter(t -> t.getConsulta() != null)
                .collect(Collectors.toMap(
                        t -> t.getConsulta().getId(),
                        t -> calculatePriorityWeight(t.getPrioridade()),
                        (p1, p2) -> Math.max(p1, p2)
                ));

        // Carregamento em lote de pagamentos (1 query única)
        Map<UUID, UUID> pagamentoMap = pagamentoRepository.findByConsultaIdIn(consultaIds).stream()
                .filter(p -> p.getConsulta() != null)
                .collect(Collectors.toMap(
                        p -> p.getConsulta().getId(),
                        Pagamento::getId,
                        (p1, p2) -> p1
                ));

        // Ordenação em memória sem qualquer query disparada dentro do comparator
        consultas.sort((c1, c2) -> {
            int p1 = priorityMap.getOrDefault(c1.getId(), 0);
            int p2 = priorityMap.getOrDefault(c2.getId(), 0);
            if (p1 != p2) {
                return Integer.compare(p2, p1);
            }
            return c1.getDataHora().compareTo(c2.getDataHora());
        });

        return consultas.stream()
                .map(c -> mapToDTO(c, true, pagamentoMap.get(c.getId())))
                .collect(Collectors.toList());
    }

    private int calculatePriorityWeight(PrioridadeTriagem prioridade) {
        if (prioridade == null) return 0;
        return switch (prioridade) {
            case EMERGENCIA -> 4;
            case ALTA -> 3;
            case MEDIA -> 2;
            case BAIXA -> 1;
            default -> 0;
        };
    }

    private ConsultaDTO mapToDTO(Consulta consulta) {
        return mapToDTO(consulta, false, null);
    }

    private ConsultaDTO mapToDTO(Consulta consulta, boolean preloaded, UUID preloadedPagamentoId) {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setId(consulta.getId());
        dto.setMedicoId(consulta.getMedico().getId());
        if (consulta.getMedico().getUsuario() != null) {
            String nome = consulta.getMedico().getUsuario().getNome();
            dto.setMedicoNome(nome != null && !nome.isBlank() ? nome : consulta.getMedico().getUsuario().getUsername());
        }
        dto.setPacienteId(consulta.getPaciente().getId());
        if (consulta.getPaciente().getUsuario() != null) {
            String nome = consulta.getPaciente().getUsuario().getNome();
            dto.setPacienteNome(nome != null && !nome.isBlank() ? nome : consulta.getPaciente().getUsuario().getUsername());
        }
        dto.setDataHora(consulta.getDataHora());
        dto.setStatus(consulta.getStatus());

        if (preloaded) {
            dto.setPagamentoId(preloadedPagamentoId);
        } else {
            pagamentoRepository.findByConsultaId(consulta.getId()).ifPresent(p -> {
                dto.setPagamentoId(p.getId());
            });
        }

        return dto;
    }
}
