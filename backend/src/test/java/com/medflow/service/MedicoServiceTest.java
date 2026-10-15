package com.medflow.service;

import com.medflow.dto.MedicoDTO;
import com.medflow.entity.Especialidade;
import com.medflow.entity.Medico;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.Role;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.EspecialidadeRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
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
class MedicoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private EspecialidadeRepository especialidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MedicoService medicoService;

    private Medico medico;
    private Especialidade cardio;
    private Usuario usuario;
    private UUID medicoId;
    private UUID especialidadeId;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        medicoId = UUID.randomUUID();
        especialidadeId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();

        cardio = new Especialidade();
        cardio.setId(especialidadeId);
        cardio.setNome("Cardiologia");

        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Dr. Carlos");
        usuario.setRole(Role.MEDICO);

        medico = new Medico();
        medico.setId(medicoId);
        medico.setCrm("123456-SP");
        medico.setEspecialidades(List.of(cardio));
        medico.setUsuario(usuario);
    }

    @Test
    @DisplayName("Deve cadastrar médico com sucesso quando dados válidos")
    void create_DeveCriarMedicoComSucesso() {
        MedicoDTO dto = new MedicoDTO();
        dto.setCrm("123456-SP");
        dto.setUsuarioId(usuarioId);
        dto.setEspecialidadesIds(List.of(especialidadeId));

        when(medicoRepository.findByCrm("123456-SP")).thenReturn(Optional.empty());
        when(especialidadeRepository.findAllById(List.of(especialidadeId))).thenReturn(List.of(cardio));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(medicoRepository.save(any(Medico.class))).thenReturn(medico);

        MedicoDTO resultado = medicoService.create(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCrm()).isEqualTo("123456-SP");
        verify(medicoRepository).save(any(Medico.class));
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando CRM já cadastrado")
    void create_DeveLancarExcecao_QuandoCrmDuplicado() {
        MedicoDTO dto = new MedicoDTO();
        dto.setCrm("123456-SP");

        when(medicoRepository.findByCrm("123456-SP")).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> medicoService.create(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um médico cadastrado com este CRM");

        verify(medicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando especialidade não existir")
    void create_DeveLancarExcecao_QuandoEspecialidadeNaoExiste() {
        MedicoDTO dto = new MedicoDTO();
        dto.setCrm("999999-SP");
        dto.setEspecialidadesIds(List.of(especialidadeId));

        when(medicoRepository.findByCrm("999999-SP")).thenReturn(Optional.empty());
        when(especialidadeRepository.findAllById(List.of(especialidadeId))).thenReturn(List.of());

        assertThatThrownBy(() -> medicoService.create(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Uma ou mais especialidades não foram encontradas");
    }

    @Test
    @DisplayName("Deve listar todos os médicos sem filtro de especialidade")
    void findAll_SemFiltro_DeveRetornarTodos() {
        when(medicoRepository.findAll()).thenReturn(List.of(medico));

        List<MedicoDTO> resultado = medicoService.findAll(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCrm()).isEqualTo("123456-SP");
    }

    @Test
    @DisplayName("Deve filtrar médicos por ID da especialidade")
    void findAll_ComFiltro_DeveFiltrarPorEspecialidade() {
        when(medicoRepository.findByEspecialidadesId(especialidadeId)).thenReturn(List.of(medico));

        List<MedicoDTO> resultado = medicoService.findAll(especialidadeId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEspecialidades().get(0).getNome()).isEqualTo("Cardiologia");
    }

    @Test
    @DisplayName("Deve buscar médico por ID com sucesso")
    void findById_DeveRetornarMedico() {
        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));

        MedicoDTO resultado = medicoService.findById(medicoId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(medicoId);
    }

    @Test
    @DisplayName("Deve remover médico quando ID existir")
    void delete_DeveRemoverMedico() {
        when(medicoRepository.existsById(medicoId)).thenReturn(true);

        medicoService.delete(medicoId);

        verify(medicoRepository).deleteById(medicoId);
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException ao remover médico inexistente")
    void delete_DeveLancarExcecao_QuandoNaoExiste() {
        when(medicoRepository.existsById(medicoId)).thenReturn(false);

        assertThatThrownBy(() -> medicoService.delete(medicoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
