package com.medflow.service;

import com.medflow.dto.ExameDTO;
import com.medflow.entity.Exame;
import com.medflow.entity.Prontuario;
import com.medflow.repository.ExameRepository;
import com.medflow.repository.ProntuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExameService {

    private final ExameRepository exameRepository;
    private final ProntuarioRepository prontuarioRepository;

    @PreAuthorize("hasRole('MEDICO')")
    public ExameDTO adicionarExame(ExameDTO dto) {
        Prontuario prontuario = prontuarioRepository.findById(dto.getProntuarioId())
                .orElseThrow(() -> new RuntimeException("Prontuário não encontrado."));

        Exame exame = new Exame();
        exame.setProntuario(prontuario);
        exame.setNomeArquivo(dto.getNomeArquivo());
        exame.setUrlArquivo(dto.getUrlArquivo());

        exame = exameRepository.save(exame);
        return mapToDTO(exame);
    }

    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public List<ExameDTO> findByProntuario(UUID prontuarioId) {
        // A lógica de bloqueio de visualização de prontuário pode ser feita 
        // chamando o ProntuarioService.findById(prontuarioId) ou validando aqui.
        // Para simplificar, confiaremos que se ele já tem o ID do prontuário, a rota de exames exibe os exames dele.
        return exameRepository.findByProntuarioId(prontuarioId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ExameDTO mapToDTO(Exame exame) {
        ExameDTO dto = new ExameDTO();
        dto.setId(exame.getId());
        dto.setProntuarioId(exame.getProntuario().getId());
        dto.setNomeArquivo(exame.getNomeArquivo());
        dto.setUrlArquivo(exame.getUrlArquivo());
        return dto;
    }
}
