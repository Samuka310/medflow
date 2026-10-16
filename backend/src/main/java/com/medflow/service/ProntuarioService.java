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
    public List<ProntuarioDTO> findMyHistorico(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        List<ProntuarioDTO> resultado = List.of();
        if (usuario.getRole() == com.medflow.entity.enums.Role.PACIENTE) {
            Paciente paciente = pacienteRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new RuntimeException("Paciente não encontrado para este usuário."));

            resultado = prontuarioRepository.findByConsultaPacienteId(paciente.getId()).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else if (usuario.getRole() == com.medflow.entity.enums.Role.MEDICO) {
            resultado = prontuarioRepository.findByConsultaMedicoUsuarioEmail(email).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        auditoriaService.registrarAcao(email, "LEITURA_HISTORICO_PRONTUARIOS", "Prontuario", null, "Leitura de histórico de prontuários realizada por: " + email);
        return resultado;
    }
    
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ProntuarioDTO findById(UUID id, String email) {
        Prontuario prontuario = prontuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prontuário não encontrado."));
                
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        if (usuario.getRole() == com.medflow.entity.enums.Role.PACIENTE) {
            if (prontuario.getConsulta() == null || prontuario.getConsulta().getPaciente() == null
                    || prontuario.getConsulta().getPaciente().getUsuario() == null
                    || !prontuario.getConsulta().getPaciente().getUsuario().getId().equals(usuario.getId())) {
                throw new com.medflow.exception.AcessoNegadoException("Acesso negado: Este prontuário não pertence a você.");
            }
        } else if (usuario.getRole() == com.medflow.entity.enums.Role.MEDICO) {
            if (prontuario.getConsulta() == null || prontuario.getConsulta().getPaciente() == null
                    || !consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(prontuario.getConsulta().getPaciente().getId(), email)) {
                throw new com.medflow.exception.AcessoNegadoException("Acesso negado: Médico não possui vínculo assistencial com o paciente deste prontuário.");
            }
        }
        
        auditoriaService.registrarAcao("LEITURA_PRONTUARIO", "Prontuario", prontuario.getId(), "Leitura de prontuário realizada por: " + email);
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
