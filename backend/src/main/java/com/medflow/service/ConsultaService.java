package com.medflow.service;

import com.medflow.config.RabbitMQConfig;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;
    private final com.medflow.repository.TriagemRepository triagemRepository;
    private final AuditoriaService auditoriaService;

    public ConsultaDTO agendar(ConsultaDTO dto) {
        if (dto.getDataHora().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RuntimeException("A consulta deve ser agendada com no mínimo 30 minutos de antecedência.");
        }

        Medico medico = medicoRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico não encontrado."));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isPacienteLogado = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
            if (isPacienteLogado && !paciente.getUsuario().getEmail().equals(auth.getName())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode agendar consultas para si mesmo.");
            }
        }

        LocalDateTime inicio = dto.getDataHora().minusMinutes(59);
        LocalDateTime fim = dto.getDataHora().plusMinutes(59);

        if (consultaRepository.existsByMedicoIdAndDataHoraBetween(medico.getId(), inicio, fim)) {
            throw new RuntimeException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsByPacienteIdAndDataHoraBetween(paciente.getId(), inicio, fim)) {
            throw new RuntimeException("O paciente já possui uma consulta neste horário.");
        }

        Consulta consulta = new Consulta();
        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setDataHora(dto.getDataHora());
        consulta.setStatus("AGENDADA");
        
        consulta = consultaRepository.save(consulta);

        Pagamento pagamento = new Pagamento();
        pagamento.setConsulta(consulta);
        pagamento.setValor(new BigDecimal("150.00")); // Valor fictício
        pagamento.setStatus("PENDENTE");
        pagamento.setFormaPagamento("CARTAO");
        pagamentoRepository.save(pagamento);

        String mensagem = String.format("Consulta AGENDADA para paciente %s com médico(a) no dia %s", 
            paciente.getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
        auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Operacao de consulta: " + consulta.getStatus());

        return mapToDTO(consulta);
    }

    public ConsultaDTO remarcar(UUID id, LocalDateTime novaDataHora) {
        if (novaDataHora.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RuntimeException("A remarcação deve ser feita com no mínimo 30 minutos de antecedência.");
        }

        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));

        if (!"AGENDADA".equals(consulta.getStatus())) {
            throw new RuntimeException("Somente consultas AGENDADAS podem ser remarcadas.");
        }

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isPacienteLogado = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
            if (isPacienteLogado && !consulta.getPaciente().getUsuario().getEmail().equals(auth.getName())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode remarcar suas próprias consultas.");
            }
        }

        LocalDateTime inicio = novaDataHora.minusMinutes(59);
        LocalDateTime fim = novaDataHora.plusMinutes(59);

        if (consultaRepository.existsByMedicoIdAndDataHoraBetween(consulta.getMedico().getId(), inicio, fim)) {
            throw new RuntimeException("O médico já possui uma consulta neste horário.");
        }

        if (consultaRepository.existsByPacienteIdAndDataHoraBetween(consulta.getPaciente().getId(), inicio, fim)) {
            throw new RuntimeException("O paciente já possui uma consulta neste horário.");
        }

        consulta.setDataHora(novaDataHora);
        consulta = consultaRepository.save(consulta);

        String mensagem = String.format("Consulta REMARCADA para paciente %s com médico(a) para o dia %s", 
            consulta.getPaciente().getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
        auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Operacao de consulta: " + consulta.getStatus());

        return mapToDTO(consulta);
    }

    public void confirmar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isPacienteLogado = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
            if (isPacienteLogado && !consulta.getPaciente().getUsuario().getEmail().equals(auth.getName())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode confirmar suas próprias consultas.");
            }
        }

        if (!"AGENDADA".equals(consulta.getStatus())) {
            throw new RuntimeException("Somente consultas AGENDADAS podem ser confirmadas.");
        }
        
        consulta.setStatus("CONFIRMADA");
        consultaRepository.save(consulta);

        String mensagem = String.format("Consulta CONFIRMADA para paciente %s no dia %s", 
            consulta.getPaciente().getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
        auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Operacao de consulta: " + consulta.getStatus());
    }

    public void checkin(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if ("CANCELADA".equals(consulta.getStatus()) || "REALIZADA".equals(consulta.getStatus())) {
            throw new RuntimeException("Status inválido para check-in.");
        }
        
        consulta.setStatus("AGUARDANDO_TRIAGEM"); // O paciente chegou e aguarda triagem
        consultaRepository.save(consulta);

        String mensagem = String.format("Paciente %s realizou check-in para a consulta.", 
            consulta.getPaciente().getUsuario().getNome());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
        auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Operacao de consulta: " + consulta.getStatus());
    }

    public void cancelar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isPacienteLogado = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
            if (isPacienteLogado && !consulta.getPaciente().getUsuario().getEmail().equals(auth.getName())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode cancelar suas próprias consultas.");
            }
        }

        if (consulta.getDataHora().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new RuntimeException("Cancelamento só permitido com pelo menos 2 horas de antecedência.");
        }
        
        consulta.setStatus("CANCELADA");
        consultaRepository.save(consulta);

        String mensagem = String.format("Consulta CANCELADA para paciente %s no dia %s", 
            consulta.getPaciente().getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
        auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Operacao de consulta: " + consulta.getStatus());
    }

    public void realizar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if ("CANCELADA".equals(consulta.getStatus())) {
            throw new RuntimeException("Não é possível realizar uma consulta cancelada.");
        }
        
        Pagamento pagamento = pagamentoRepository.findByConsultaId(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado."));
        
        if (!"PAGO".equals(pagamento.getStatus())) {
            throw new RuntimeException("A consulta não pode ser realizada sem o pagamento estar PAGO.");
        }

        consulta.setStatus("REALIZADA");
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

    public List<ConsultaDTO> agendaHoje() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
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
                case "EMERGENCIA" -> 4;
                case "ALTA" -> 3;
                case "MEDIA" -> 2;
                case "BAIXA" -> 1;
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
