package com.medflow.service;

import com.medflow.dto.EspecialidadeDTO;
import com.medflow.entity.Especialidade;
import com.medflow.repository.EspecialidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EspecialidadeService {

    private final EspecialidadeRepository especialidadeRepository;

    public EspecialidadeDTO create(EspecialidadeDTO dto) {
        if (especialidadeRepository.findByNomeIgnoreCase(dto.getNome()).isPresent()) {
            throw new RuntimeException("Especialidade já cadastrada com este nome.");
        }
        
        Especialidade especialidade = new Especialidade();
        especialidade.setNome(dto.getNome());
        especialidade.setDescricao(dto.getDescricao());
        
        especialidade = especialidadeRepository.save(especialidade);
        return mapToDTO(especialidade);
    }

    public List<EspecialidadeDTO> findAll() {
        return especialidadeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public EspecialidadeDTO findById(UUID id) {
        Especialidade especialidade = especialidadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Especialidade não encontrada."));
        return mapToDTO(especialidade);
    }

    public void delete(UUID id) {
        if (!especialidadeRepository.existsById(id)) {
            throw new RuntimeException("Especialidade não encontrada.");
        }
        especialidadeRepository.deleteById(id);
    }

    private EspecialidadeDTO mapToDTO(Especialidade especialidade) {
        EspecialidadeDTO dto = new EspecialidadeDTO();
        dto.setId(especialidade.getId());
        dto.setNome(especialidade.getNome());
        dto.setDescricao(especialidade.getDescricao());
        return dto;
    }
}
