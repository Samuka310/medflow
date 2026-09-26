package com.medflow.service;

import com.medflow.dto.MedicoDTO;
import com.medflow.entity.Especialidade;
import com.medflow.entity.Medico;
import com.medflow.entity.Usuario;
import com.medflow.repository.EspecialidadeRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final UsuarioRepository usuarioRepository;

    public MedicoDTO create(MedicoDTO dto) {
        if (medicoRepository.findByCrm(dto.getCrm()).isPresent()) {
            throw new RuntimeException("Já existe um médico cadastrado com este CRM.");
        }

        Especialidade especialidade = especialidadeRepository.findById(dto.getEspecialidadeId())
                .orElseThrow(() -> new RuntimeException("Especialidade não encontrada."));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!"MEDICO".equalsIgnoreCase(usuario.getRole())) {
            throw new RuntimeException("O usuário selecionado não possui a role MEDICO.");
        }

        Medico medico = new Medico();
        medico.setCrm(dto.getCrm());
        medico.setEspecialidade(especialidade);
        medico.setUsuario(usuario);
        
        medico = medicoRepository.save(medico);
        return mapToDTO(medico);
    }

    public List<MedicoDTO> findAll() {
        return medicoRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public MedicoDTO findById(UUID id) {
        Medico medico = medicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médico não encontrado."));
        return mapToDTO(medico);
    }

    public void delete(UUID id) {
        if (!medicoRepository.existsById(id)) {
            throw new RuntimeException("Médico não encontrado.");
        }
        medicoRepository.deleteById(id);
    }

    private MedicoDTO mapToDTO(Medico medico) {
        MedicoDTO dto = new MedicoDTO();
        dto.setId(medico.getId());
        dto.setCrm(medico.getCrm());
        dto.setEspecialidadeId(medico.getEspecialidade().getId());
        dto.setUsuarioId(medico.getUsuario().getId());
        return dto;
    }
}
