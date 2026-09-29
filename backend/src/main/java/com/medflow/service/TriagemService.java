package com.medflow.service;

import com.medflow.dto.TriagemDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Funcionario;
import com.medflow.entity.Paciente;
import com.medflow.entity.Triagem;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.FuncionarioRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TriagemService {

    private final TriagemRepository triagemRepository;
    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final FuncionarioRepository funcionarioRepository;

    public TriagemDTO registrar(TriagemDTO dto) {
        Consulta consulta = consultaRepository.findById(dto.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));

        if (!consulta.getPaciente().getId().equals(paciente.getId())) {
            throw new RuntimeException("A consulta informada não pertence ao paciente informado.");
        }

        if (triagemRepository.findByConsultaId(consulta.getId()).isPresent()) {
            throw new RuntimeException("Já existe uma triagem registrada para esta consulta.");
        }

        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Funcionario funcionario = funcionarioRepository.findByUsuarioEmail(emailLogado)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado ou você não é um funcionário (ADMIN genérico não é funcionário)."));

        Triagem triagem = new Triagem();
        triagem.setPaciente(paciente);
        triagem.setConsulta(consulta);
        triagem.setRealizadoPor(funcionario);
        triagem.setPressaoArterial(dto.getPressaoArterial());
        triagem.setTemperatura(dto.getTemperatura());
        triagem.setPeso(dto.getPeso());
        triagem.setAltura(dto.getAltura());
        triagem.setQueixaPrincipal(dto.getQueixaPrincipal());
        triagem.setPrioridade(dto.getPrioridade() != null ? dto.getPrioridade() : "BAIXA");
        triagem.setDataHora(LocalDateTime.now());

        triagem = triagemRepository.save(triagem);

        return mapToDTO(triagem);
    }

    public TriagemDTO findByConsultaId(UUID consultaId) {
        Triagem triagem = triagemRepository.findByConsultaId(consultaId)
                .orElseThrow(() -> new RuntimeException("Triagem não encontrada para esta consulta."));
        return mapToDTO(triagem);
    }

    private TriagemDTO mapToDTO(Triagem triagem) {
        TriagemDTO dto = new TriagemDTO();
        dto.setId(triagem.getId());
        dto.setPacienteId(triagem.getPaciente().getId());
        dto.setConsultaId(triagem.getConsulta().getId());
        dto.setRealizadoPorId(triagem.getRealizadoPor().getId());
        dto.setPressaoArterial(triagem.getPressaoArterial());
        dto.setTemperatura(triagem.getTemperatura());
        dto.setPeso(triagem.getPeso());
        dto.setAltura(triagem.getAltura());
        dto.setQueixaPrincipal(triagem.getQueixaPrincipal());
        dto.setPrioridade(triagem.getPrioridade());
        dto.setDataHora(triagem.getDataHora());
        return dto;
    }
}
