package com.medflow.service;


import com.medflow.dto.ConsultaDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Pagamento;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.medflow.event.ConsultaEvento;
import java.time.Instant;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.medflow.repository.TriagemRepository triagemRepository;
    

    public ConsultaDTO agendar(ConsultaDTO dto) {
        if (dto.getDataHora().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new com.medflow.exception.RegraDeNegocioException("A consulta deve ser agendada com no mínimo 30 minutos de antecedência.");
        }

        Medico medico = medicoRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Médico não encontrado."));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado."));

        LocalDateTime inicio = dto.getDataHora().minusMinutes(59);
        LocalDateTime fim = dto.getDataHora().plusMinutes(59);

        if (consultaRepository.existsByMedicoIdAndDataHoraBetween(medico.getId(), inicio, fim)) {
            throw new com.medflow.exception.ConflitoAgendaException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsByPacienteIdAndDataHoraBetween(paciente.getId(), inicio, fim)) {
            throw new com.medflow.exception.ConflitoAgendaException("O paciente já possui uma consulta neste horário.");
        }

        Consulta consulta = new Consulta();
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setDataHora(dto.getDataHora());
        consulta.setStatus(com.medflow.entity.enums.ConsultaStatus.AGENDADA);
        
        consulta = consultaRepository.save(consulta);

        Pagamento pagamento = new Pagamento();
        pagamento.setConsulta(consulta);
        BigDecimal valor = (medico.getEspecialidades() == null || medico.getEspecialidades().isEmpty() || medico.getEspecialidades().get(0).getValorConsulta() == null)
            ? new BigDecimal("150.00") 
            : medico.getEspecialidades().get(0).getValorConsulta();
        pagamento.setValor(valor);
        pagamento.setStatus(com.medflow.entity.enums.PagamentoStatus.PENDENTE);
        pagamento.setFormaPagamento(com.medflow.entity.enums.FormaPagamento.CARTAO);
        pagamentoRepository.save(pagamento);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));

        return mapToDTO(consulta);
    }

    public ConsultaDTO remarcar(UUID id, LocalDateTime novaDataHora) {
        if (novaDataHora.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new com.medflow.exception.RegraDeNegocioException("A remarcação deve ser feita com no mínimo 30 minutos de antecedência.");
        }

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));

        if (consulta.getStatus() != com.medflow.entity.enums.ConsultaStatus.AGENDADA) {
            throw new com.medflow.exception.RegraDeNegocioException("Somente consultas AGENDADAS podem ser remarcadas.");
        }

        LocalDateTime inicio = novaDataHora.minusMinutes(59);
        LocalDateTime fim = novaDataHora.plusMinutes(59);

        if (consultaRepository.existsByMedicoIdAndDataHoraBetween(consulta.getMedico().getId(), inicio, fim)) {
            throw new com.medflow.exception.ConflitoAgendaException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsByPacienteIdAndDataHoraBetween(consulta.getPaciente().getId(), inicio, fim)) {
            throw new com.medflow.exception.ConflitoAgendaException("O paciente já possui uma consulta neste horário.");
        }

        consulta.setDataHora(novaDataHora);
        consulta = consultaRepository.save(consulta);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));

        return mapToDTO(consulta);
    }

    public void confirmar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getStatus() != com.medflow.entity.enums.ConsultaStatus.AGENDADA) {
            throw new com.medflow.exception.RegraDeNegocioException("Somente consultas AGENDADAS podem ser confirmadas.");
        }
        
        consulta.confirmar();
        consultaRepository.save(consulta);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    public void checkin(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if (consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.CANCELADA || consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.REALIZADA) {
            throw new com.medflow.exception.RegraDeNegocioException("Status inválido para check-in.");
        }
        
        consulta.aguardarTriagem(); // O paciente chegou e aguarda triagem
        consultaRepository.save(consulta);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    public void cancelar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Consulta não encontrada."));

        if (consulta.getDataHora().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new com.medflow.exception.RegraDeNegocioException("Cancelamento só permitido com pelo menos 2 horas de antecedência.");
        }
        
        consulta.cancelar();
        consultaRepository.save(consulta);

        applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));
    }

    public void realizar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if (consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.CANCELADA) {
            throw new com.medflow.exception.RegraDeNegocioException("Não é possível realizar uma consulta cancelada.");
        }
        
        Pagamento pagamento = pagamentoRepository.findByConsultaId(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado."));
        
        if (pagamento.getStatus() != com.medflow.entity.enums.PagamentoStatus.PAGO) {
            throw new com.medflow.exception.RegraDeNegocioException("A consulta não pode ser realizada sem o pagamento estar PAGO.");
        }

        consulta.realizar();
        consultaRepository.save(consulta);
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
        
        // Retornar ordenado pelo horário
        consultas.sort((c1, c2) -> c1.getDataHora().compareTo(c2.getDataHora()));
        
        return consultas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<ConsultaDTO> agendaHoje(String email) {
        Medico medico = medicoRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Médico não encontrado pelo usuário logado."));
        
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        List<Consulta> consultas = consultaRepository.findByMedicoIdAndDataHoraBetween(medico.getId(), startOfDay, endOfDay);
        
        consultas.sort((c1, c2) -> {
            int p1 = getPriorityWeight(c1.getId());
            int p2 = getPriorityWeight(c2.getId());
            if (p1 != p2) {
                return Integer.compare(p2, p1); // Descending priority
            }
            return c1.getDataHora().compareTo(c2.getDataHora());
        });

        return consultas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private int getPriorityWeight(UUID consultaId) {
        return triagemRepository.findByConsultaId(consultaId)
            .map(t -> switch (t.getPrioridade()) {
                case EMERGENCIA -> 4;
                case ALTA -> 3;
                case MEDIA -> 2;
                case BAIXA -> 1;
                default -> 0;
            }).orElse(0);
    }

    private ConsultaDTO mapToDTO(Consulta consulta) {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setId(consulta.getId());
        dto.setMedicoId(consulta.getMedico().getId());
        dto.setPacienteId(consulta.getPaciente().getId());
        dto.setDataHora(consulta.getDataHora());
        dto.setStatus(consulta.getStatus());
        
        pagamentoRepository.findByConsultaId(consulta.getId()).ifPresent(p -> {
            dto.setPagamentoId(p.getId());
        });
        
        return dto;
    }
}
