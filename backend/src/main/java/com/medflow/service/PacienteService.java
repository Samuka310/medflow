package com.medflow.service;

import com.medflow.dto.PacienteDTO;
import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.medflow.repository.ConsultaRepository consultaRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.medflow.repository.ReceitaRepository receitaRepository;
    private final com.medflow.repository.AtestadoRepository atestadoRepository;
    private final com.medflow.repository.ProntuarioRepository prontuarioRepository;
    private final com.medflow.repository.MedicoRepository medicoRepository;

    public PacienteDTO create(PacienteDTO dto, String emailLogado, boolean isRecepcionista) {
        if (!isRecepcionista) {
            // Must be PACIENTE
            String email = emailLogado;
            Usuario logado = usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuário logado não encontrado"));
            if (dto.getUsuarioId() != null && !dto.getUsuarioId().equals(logado.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode criar um registro de paciente para si mesmo.");
            }
            if (dto.getUsuarioId() == null) {
                // Forcing the usuarioId to be the logged in user
                dto.setUsuarioId(logado.getId());
            }
        }

        if (dto.getUsuarioId() != null && pacienteRepository.existsByUsuarioId(dto.getUsuarioId())) {
            throw new com.medflow.exception.RegraDeNegocioException("Já existe um registro de paciente vinculado a este usuário.");
        }

        if (pacienteRepository.findByCpf(dto.getCpf()).isPresent()) {
            throw new com.medflow.exception.RegraDeNegocioException("Já existe um paciente cadastrado com este CPF.");
        }

        Usuario usuario;
        if (dto.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        } else {
            if (dto.getEmail() == null || dto.getSenha() == null || dto.getNome() == null) {
                throw new com.medflow.exception.RegraDeNegocioException("Para criar um paciente sem usuarioId, informe nome, email e senha.");
            }
            if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new com.medflow.exception.RegraDeNegocioException("E-mail já está em uso.");
            }
            usuario = new Usuario();
            usuario.setNome(dto.getNome());
            usuario.setEmail(dto.getEmail());
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
            usuario.setRole(com.medflow.entity.enums.Role.PACIENTE);
            usuario = usuarioRepository.save(usuario);
        }

        if (usuario.getRole() != com.medflow.entity.enums.Role.PACIENTE) {
            throw new com.medflow.exception.RegraDeNegocioException("O usuário selecionado não possui a role PACIENTE.");
        }

        Paciente paciente = new Paciente();
        paciente.setCpf(dto.getCpf());
        paciente.setDataNascimento(dto.getDataNascimento());
        paciente.setUsuario(usuario);
        
        paciente = pacienteRepository.save(paciente);
        return mapToDTO(paciente);
    }

    public List<PacienteDTO> findAll() {
        return pacienteRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PacienteDTO findById(UUID id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));
        return mapToDTO(paciente);
    }

    public PacienteDTO findByUsuarioId(UUID usuarioId) {
        Paciente paciente = pacienteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado para este usuário."));
        return mapToDTO(paciente);
    }

    public com.medflow.dto.PacienteResumoDTO findResumoById(UUID id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));
        return mapToResumoDTO(paciente);
    }

    public com.medflow.dto.PacienteResumoDTO findMe(String email) {
        Paciente paciente = pacienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Registro de paciente não encontrado para este usuário."));
        return mapToResumoDTO(paciente);
    }

    public com.medflow.dto.PacienteClinicoDTO findClinicoById(UUID id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));
        

        return mapToClinicoDTO(paciente);
    }

    public void delete(UUID id) {
        if (!pacienteRepository.existsById(id)) {
            throw new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado.");
        }
        pacienteRepository.deleteById(id);
    }

    private PacienteDTO mapToDTO(Paciente paciente) {
        PacienteDTO dto = new PacienteDTO();
        dto.setId(paciente.getId());
        dto.setCpf(paciente.getCpf());
        dto.setDataNascimento(paciente.getDataNascimento());
        dto.setUsuarioId(paciente.getUsuario().getId());
        return dto;
    }

    private com.medflow.dto.PacienteResumoDTO mapToResumoDTO(Paciente paciente) {
        com.medflow.dto.PacienteResumoDTO dto = new com.medflow.dto.PacienteResumoDTO();
        dto.setId(paciente.getId());
        dto.setNome(paciente.getUsuario().getNome());
        dto.setEmail(paciente.getUsuario().getEmail());
        dto.setCpf(paciente.getCpf());
        dto.setDataNascimento(paciente.getDataNascimento());
        return dto;
    }

    public com.medflow.dto.ReceitaDTO prescreverReceita(UUID pacienteId, com.medflow.dto.ReceitaDTO dto, String emailLogado) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));
        
        
        String email = emailLogado;
        com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Médico não encontrado."));

        com.medflow.entity.Receita receita = new com.medflow.entity.Receita();
        receita.setPaciente(paciente);
        receita.setMedico(medico);
        if (dto.getConsultaId() != null) {
            com.medflow.entity.Consulta consulta = consultaRepository.findById(dto.getConsultaId()).orElse(null);
            receita.setConsulta(consulta);
        }
        receita.setMedicamento(dto.getMedicamento());
        receita.setDosagem(dto.getDosagem());
        receita.setValidadeData(dto.getValidadeData());
        
        receita = receitaRepository.save(receita);
        
        dto.setId(receita.getId());
        dto.setPacienteId(paciente.getId());
        dto.setMedicoId(medico.getId());
        dto.setDataEmissao(receita.getDataEmissao());
        return dto;
    }

    public com.medflow.dto.AtestadoDTO emitirAtestado(UUID pacienteId, com.medflow.dto.AtestadoDTO dto, String emailLogado) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado."));
        
        
        String email = emailLogado;
        com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new RuntimeException("Médico não encontrado."));

        com.medflow.entity.Atestado atestado = new com.medflow.entity.Atestado();
        atestado.setPaciente(paciente);
        atestado.setMedico(medico);
        if (dto.getConsultaId() != null) {
            com.medflow.entity.Consulta consulta = consultaRepository.findById(dto.getConsultaId()).orElse(null);
            atestado.setConsulta(consulta);
        }
        atestado.setDiasAfastamento(dto.getDiasAfastamento());
        atestado.setCidOpcional(dto.getCidOpcional());
        
        atestado = atestadoRepository.save(atestado);
        
        dto.setId(atestado.getId());
        dto.setPacienteId(paciente.getId());
        dto.setMedicoId(medico.getId());
        dto.setDataEmissao(atestado.getDataEmissao());
        return dto;
    }

    private com.medflow.dto.PacienteClinicoDTO mapToClinicoDTO(Paciente paciente) {
        com.medflow.dto.PacienteClinicoDTO dto = new com.medflow.dto.PacienteClinicoDTO();
        dto.setId(paciente.getId());
        dto.setNome(paciente.getUsuario().getNome());
        dto.setEmail(paciente.getUsuario().getEmail());
        dto.setCpf(paciente.getCpf());
        dto.setDataNascimento(paciente.getDataNascimento());
        dto.setHistoricoClinicoResumo("Dados clínicos completos.");
        
        List<com.medflow.dto.ReceitaDTO> receitas = receitaRepository.findByPacienteId(paciente.getId()).stream().map(r -> {
            com.medflow.dto.ReceitaDTO rDto = new com.medflow.dto.ReceitaDTO();
            rDto.setId(r.getId());
            rDto.setPacienteId(r.getPaciente().getId());
            rDto.setMedicoId(r.getMedico().getId());
            if(r.getConsulta() != null) rDto.setConsultaId(r.getConsulta().getId());
            rDto.setMedicamento(r.getMedicamento());
            rDto.setDosagem(r.getDosagem());
            rDto.setValidadeData(r.getValidadeData());
            rDto.setDataEmissao(r.getDataEmissao());
            return rDto;
        }).collect(Collectors.toList());
        dto.setReceitas(receitas);

        List<com.medflow.dto.AtestadoDTO> atestados = atestadoRepository.findByPacienteId(paciente.getId()).stream().map(a -> {
            com.medflow.dto.AtestadoDTO aDto = new com.medflow.dto.AtestadoDTO();
            aDto.setId(a.getId());
            aDto.setPacienteId(a.getPaciente().getId());
            aDto.setMedicoId(a.getMedico().getId());
            if(a.getConsulta() != null) aDto.setConsultaId(a.getConsulta().getId());
            aDto.setDiasAfastamento(a.getDiasAfastamento());
            aDto.setCidOpcional(a.getCidOpcional());
            aDto.setDataEmissao(a.getDataEmissao());
            return aDto;
        }).collect(Collectors.toList());
        dto.setAtestados(atestados);
        
        List<com.medflow.dto.ProntuarioDTO> prontuarios = prontuarioRepository.findByConsultaPacienteId(paciente.getId()).stream().map(p -> {
            com.medflow.dto.ProntuarioDTO pDto = new com.medflow.dto.ProntuarioDTO();
            pDto.setId(p.getId());
            pDto.setConsultaId(p.getConsulta().getId());
            pDto.setDiagnosticoCid(p.getDiagnosticoCid());
            pDto.setEvolucaoClinica(p.getEvolucaoClinica());
            pDto.setCondutaMedica(p.getCondutaMedica());
            pDto.setObservacoes(p.getObservacoes());
            return pDto;
        }).collect(Collectors.toList());
        dto.setProntuarios(prontuarios);

        return dto;
    }
}
