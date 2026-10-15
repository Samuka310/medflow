package com.medflow.service;

import com.medflow.dto.EspecialidadeDTO;
import com.medflow.entity.Especialidade;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.EspecialidadeRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EspecialidadeServiceTest {

    @Mock
    private EspecialidadeRepository especialidadeRepository;

    @InjectMocks
    private EspecialidadeService especialidadeService;

    @Test
    @DisplayName("Deve criar especialidade com sucesso quando nome não duplicado")
    void create_DeveCriarEspecialidade_QuandoNomeValido() {
        EspecialidadeDTO dto = new EspecialidadeDTO();
        dto.setNome("Dermatologia");
        dto.setDescricao("Cuidados com a pele");

        Especialidade salva = new Especialidade();
        salva.setId(UUID.randomUUID());
        salva.setNome("Dermatologia");
        salva.setDescricao("Cuidados com a pele");

        when(especialidadeRepository.findByNomeIgnoreCase("Dermatologia")).thenReturn(Optional.empty());
        when(especialidadeRepository.save(any(Especialidade.class))).thenReturn(salva);

        EspecialidadeDTO resultado = especialidadeService.create(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Dermatologia");
        verify(especialidadeRepository).save(any(Especialidade.class));
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando especialidade já existir")
    void create_DeveLancarExcecao_QuandoNomeDuplicado() {
        EspecialidadeDTO dto = new EspecialidadeDTO();
        dto.setNome("Cardiologia");

        when(especialidadeRepository.findByNomeIgnoreCase("Cardiologia")).thenReturn(Optional.of(new Especialidade()));

        assertThatThrownBy(() -> especialidadeService.create(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Especialidade já cadastrada com este nome");
    }

    @Test
    @DisplayName("Deve listar todas as especialidades")
    void findAll_DeveRetornarTodasEspecialidades() {
        Especialidade e = new Especialidade();
        e.setId(UUID.randomUUID());
        e.setNome("Ortopedia");

        when(especialidadeRepository.findAll()).thenReturn(List.of(e));

        List<EspecialidadeDTO> resultado = especialidadeService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Ortopedia");
    }

    @Test
    @DisplayName("Deve buscar especialidade por ID com sucesso")
    void findById_DeveRetornarEspecialidade() {
        UUID id = UUID.randomUUID();
        Especialidade e = new Especialidade();
        e.setId(id);
        e.setNome("Pediatria");

        when(especialidadeRepository.findById(id)).thenReturn(Optional.of(e));

        EspecialidadeDTO resultado = especialidadeService.findById(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Pediatria");
    }

    @Test
    @DisplayName("Deve deletar especialidade quando existir")
    void delete_DeveDeletar_QuandoExiste() {
        UUID id = UUID.randomUUID();
        when(especialidadeRepository.existsById(id)).thenReturn(true);

        especialidadeService.delete(id);

        verify(especialidadeRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException ao deletar inexistente")
    void delete_DeveLancarExcecao_QuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(especialidadeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> especialidadeService.delete(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
