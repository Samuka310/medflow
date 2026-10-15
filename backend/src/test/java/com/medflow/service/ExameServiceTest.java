package com.medflow.service;

import com.medflow.dto.ExameDTO;
import com.medflow.entity.Exame;
import com.medflow.entity.Prontuario;
import com.medflow.repository.ExameRepository;
import com.medflow.repository.ProntuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExameServiceTest {

    @Mock
    private ExameRepository exameRepository;

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @InjectMocks
    private ExameService exameService;

    @Test
    @DisplayName("Deve anexar exame clínico ao prontuário com sucesso")
    void adicionarExame_DeveSalvarExameComSucesso() {
        UUID prontuarioId = UUID.randomUUID();
        Prontuario prontuario = new Prontuario();
        prontuario.setId(prontuarioId);

        ExameDTO dto = new ExameDTO();
        dto.setProntuarioId(prontuarioId);
        dto.setNomeArquivo("hemograma.pdf");
        dto.setUrlArquivo("https://storage.medflow.com/exames/hemograma.pdf");

        Exame exameSalvo = new Exame();
        exameSalvo.setId(UUID.randomUUID());
        exameSalvo.setProntuario(prontuario);
        exameSalvo.setNomeArquivo(dto.getNomeArquivo());
        exameSalvo.setUrlArquivo(dto.getUrlArquivo());

        when(prontuarioRepository.findById(prontuarioId)).thenReturn(Optional.of(prontuario));
        when(exameRepository.save(any(Exame.class))).thenReturn(exameSalvo);

        ExameDTO resultado = exameService.adicionarExame(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNomeArquivo()).isEqualTo("hemograma.pdf");
        verify(exameRepository).save(any(Exame.class));
    }

    @Test
    @DisplayName("Deve listar exames vinculados a um prontuário")
    void findByProntuario_DeveRetornarListaDeExames() {
        UUID prontuarioId = UUID.randomUUID();
        Prontuario prontuario = new Prontuario();
        prontuario.setId(prontuarioId);

        Exame exame = new Exame();
        exame.setId(UUID.randomUUID());
        exame.setProntuario(prontuario);
        exame.setNomeArquivo("raio-x.png");

        when(exameRepository.findByProntuarioId(prontuarioId)).thenReturn(List.of(exame));

        List<ExameDTO> lista = exameService.findByProntuario(prontuarioId);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNomeArquivo()).isEqualTo("raio-x.png");
    }
}
