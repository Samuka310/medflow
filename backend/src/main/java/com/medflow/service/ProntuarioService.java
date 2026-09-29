package com.medflow.service;

import com.medflow.dto.ProntuarioDTO;
import com.medflow.entity.Consulta;
import com.medflow.entity.Paciente;
import com.medflow.entity.Prontuario;
import com.medflow.entity.Usuario;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.ProntuarioRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final AuditoriaService auditoriaService;
    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    @PreAuthorize("hasRole('MEDICO')")
    public ProntuarioDTO salvar(ProntuarioDTO dto) {
        Consulta consulta = consultaRepository.findById(dto.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada."));

        Prontuario prontuario = prontuarioRepository.findByConsultaId(consulta.getId())
                .orElse(new Prontuario());

        prontuario.setConsulta(consulta);
        prontuario.setDiagnosticoCid(dto.getDiagnosticoCid());
        prontuario.setEvolucaoClinica(dto.getEvolucaoClinica());
        prontuario.setCondutaMedica(dto.getCondutaMedica());
        prontuario.setObservacoes(dto.getObservacoes());

        prontuario = prontuarioRepository.save(prontuario);
        auditoriaService.registrarAcao("SALVAR_PRONTUARIO", "Prontuario", prontuario.getId(), "Prontuario modificado pelo medico");
        return mapToDTO(prontuario);
    }

    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public List<ProntuarioDTO> findMyHistorico() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if ("PACIENTE".equalsIgnoreCase(usuario.getRole())) {
            Paciente paciente = pacienteRepository.findAll().stream()
                    .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Paciente não encontrado para este usuário."));

            return prontuarioRepository.findByConsultaPacienteId(paciente.getId()).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else if ("MEDICO".equalsIgnoreCase(usuario.getRole())) {
            return prontuarioRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        return List.of();
    }
    
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ProntuarioDTO findById(UUID id) {
        Prontuario prontuario = prontuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prontuário não encontrado."));
                
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).get();
        
        if ("PACIENTE".equalsIgnoreCase(usuario.getRole())) {
            if (!prontuario.getConsulta().getPaciente().getUsuario().getId().equals(usuario.getId())) {
                throw new RuntimeException("Acesso negado: Este prontuário não pertence a você.");
            }
        }
        
        auditoriaService.registrarAcao("SALVAR_PRONTUARIO", "Prontuario", prontuario.getId(), "Prontuario modificado pelo medico");
        return mapToDTO(prontuario);
    }

    private ProntuarioDTO mapToDTO(Prontuario prontuario) {
        ProntuarioDTO dto = new ProntuarioDTO();
        dto.setId(prontuario.getId());
        dto.setConsultaId(prontuario.getConsulta().getId());
        dto.setDiagnosticoCid(prontuario.getDiagnosticoCid());
        dto.setEvolucaoClinica(prontuario.getEvolucaoClinica());
        dto.setCondutaMedica(prontuario.getCondutaMedica());
        dto.setObservacoes(prontuario.getObservacoes());
        return dto;
    }
}
