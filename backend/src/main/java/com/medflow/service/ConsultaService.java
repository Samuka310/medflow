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

    public ConsultaDTO agendar(ConsultaDTO dto) {
        if (dto.getDataHora().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RuntimeException("A consulta deve ser agendada com no mínimo 30 minutos de antecedência.");
        }

        Medico medico = medicoRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new RuntimeException("Médico não encontrado."));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));

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

        return mapToDTO(consulta);
    }

    public void confirmar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if (!"AGENDADA".equals(consulta.getStatus())) {
            throw new RuntimeException("Somente consultas AGENDADAS podem ser confirmadas.");
        }
        
        consulta.setStatus("CONFIRMADA");
        consultaRepository.save(consulta);

        String mensagem = String.format("Consulta CONFIRMADA para paciente %s no dia %s", 
            consulta.getPaciente().getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
    }

    public void cancelar(UUID id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));
        
        if (consulta.getDataHora().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new RuntimeException("Cancelamento só permitido com pelo menos 2 horas de antecedência.");
        }
        
        consulta.setStatus("CANCELADA");
        consultaRepository.save(consulta);

        String mensagem = String.format("Consulta CANCELADA para paciente %s no dia %s", 
            consulta.getPaciente().getUsuario().getNome(), consulta.getDataHora().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NOTIFICACOES, mensagem);
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

    private ConsultaDTO mapToDTO(Consulta consulta) {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setId(consulta.getId());
        dto.setMedicoId(consulta.getMedico().getId());
        dto.setPacienteId(consulta.getPaciente().getId());
        dto.setDataHora(consulta.getDataHora());
        dto.setStatus(consulta.getStatus());
        return dto;
    }
}
