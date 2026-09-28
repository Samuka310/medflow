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

        List<Especialidade> especialidades = especialidadeRepository.findAllById(dto.getEspecialidadesIds());
        if (especialidades.isEmpty() || especialidades.size() != dto.getEspecialidadesIds().size()) {
            throw new RuntimeException("Uma ou mais especialidades não foram encontradas.");
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!"MEDICO".equalsIgnoreCase(usuario.getRole())) {
            throw new RuntimeException("O usuário selecionado não possui a role MEDICO.");
        }

        Medico medico = new Medico();
        medico.setCrm(dto.getCrm());
        medico.setEspecialidades(especialidades);
        medico.setUsuario(usuario);
        
        medico = medicoRepository.save(medico);
        return mapToDTO(medico);
    }

    public List<MedicoDTO> findAll(UUID especialidadeId) {
        if (especialidadeId != null) {
            return medicoRepository.findByEspecialidadesId(especialidadeId).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }
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
        dto.setEspecialidadesIds(medico.getEspecialidades().stream().map(Especialidade::getId).collect(Collectors.toList()));
        dto.setUsuarioId(medico.getUsuario().getId());
        
        dto.setEspecialidades(medico.getEspecialidades().stream().map(e -> {
            com.medflow.dto.EspecialidadeDTO edto = new com.medflow.dto.EspecialidadeDTO();
            edto.setId(e.getId());
            edto.setNome(e.getNome());
            edto.setDescricao(e.getDescricao());
            return edto;
        }).collect(Collectors.toList()));
        
        MedicoDTO.UsuarioResumoDTO usu = new MedicoDTO.UsuarioResumoDTO();
        usu.setId(medico.getUsuario().getId());
        usu.setUsername(medico.getUsuario().getNome()); // Frontend chama de username mas mostra o nome ou email
        usu.setNome(medico.getUsuario().getNome());
        dto.setUsuario(usu);

        return dto;
    }
}
