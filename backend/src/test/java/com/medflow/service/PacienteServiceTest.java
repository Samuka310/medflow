package com.medflow.service;

import com.medflow.dto.AtestadoDTO;
import com.medflow.dto.PacienteDTO;
import com.medflow.dto.PacienteResumoDTO;
import com.medflow.dto.ReceitaDTO;
import com.medflow.entity.*;
import com.medflow.entity.enums.Role;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private AtestadoRepository atestadoRepository;

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private Usuario usuario;
    private Paciente paciente;
    private UUID pacienteId;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Maria Silva");
        usuario.setEmail("maria@email.com");
        usuario.setRole(Role.PACIENTE);

        paciente = new Paciente();
        paciente.setId(pacienteId);
        paciente.setCpf("123.456.789-00");
        paciente.setDataNascimento(LocalDate.of(1995, 3, 20));
        paciente.setUsuario(usuario);
    }

    @Test
    @DisplayName("Deve criar paciente com sucesso quando usuário logado cria seu próprio registro")
    void create_DeveCriarPacienteComSucesso() {
        PacienteDTO dto = new PacienteDTO();
        dto.setCpf("123.456.789-00");
        dto.setDataNascimento(LocalDate.of(1995, 3, 20));
        dto.setUsuarioId(usuarioId);

        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(pacienteRepository.findByCpf("123.456.789-00")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        PacienteDTO resultado = pacienteService.create(dto, "maria@email.com", false);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCpf()).isEqualTo("123.456.789-00");
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando CPF já estiver cadastrado")
    void create_DeveLancarExcecao_QuandoCpfDuplicado() {
        PacienteDTO dto = new PacienteDTO();
        dto.setCpf("123.456.789-00");

        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(usuario));
        when(pacienteRepository.findByCpf("123.456.789-00")).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> pacienteService.create(dto, "maria@email.com", false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um paciente cadastrado com este CPF");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar dados de resumo do paciente logado com sucesso")
    void findMe_DeveRetornarResumoPaciente() {
        when(pacienteRepository.findByUsuarioEmail("maria@email.com")).thenReturn(Optional.of(paciente));

        PacienteResumoDTO resumo = pacienteService.findMe("maria@email.com");

        assertThat(resumo).isNotNull();
        assertThat(resumo.getNome()).isEqualTo("Maria Silva");
        assertThat(resumo.getCpf()).isEqualTo("123.456.789-00");
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException quando paciente logado não possuir cadastro")
    void findMe_DeveLancarExcecao_QuandoNaoEncontrado() {
        when(pacienteRepository.findByUsuarioEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.findMe("inexistente@email.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve listar todos os pacientes cadastrados")
    void findAll_DeveRetornarListaDePacientes() {
        when(pacienteRepository.findAll()).thenReturn(List.of(paciente));

        List<PacienteDTO> lista = pacienteService.findAll();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getCpf()).isEqualTo("123.456.789-00");
    }

    @Test
    @DisplayName("Deve prescrever receita médica com sucesso")
    void prescreverReceita_DeveSalvarComSucesso() {
        ReceitaDTO dto = new ReceitaDTO();
        dto.setMedicamento("Amoxicilina 500mg");
        dto.setDosagem("1 comprimido de 8 em 8 horas");
        dto.setValidadeData(LocalDate.now().plusDays(30));

        Medico medico = new Medico();
        medico.setId(UUID.randomUUID());
        medico.setCrm("123456-SP");

        Receita receitaSalva = new Receita();
        receitaSalva.setId(UUID.randomUUID());
        receitaSalva.setPaciente(paciente);
        receitaSalva.setMedico(medico);
        receitaSalva.setMedicamento(dto.getMedicamento());
        receitaSalva.setDosagem(dto.getDosagem());
        receitaSalva.setDataEmissao(java.time.LocalDateTime.now());

        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findByUsuarioEmail("medico@medflow.com")).thenReturn(Optional.of(medico));
        when(receitaRepository.save(any(Receita.class))).thenReturn(receitaSalva);

        ReceitaDTO resultado = pacienteService.prescreverReceita(pacienteId, dto, "medico@medflow.com");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getMedicamento()).isEqualTo("Amoxicilina 500mg");
        verify(receitaRepository).save(any(Receita.class));
    }

    @Test
    @DisplayName("Deve emitir atestado médico com sucesso")
    void emitirAtestado_DeveSalvarComSucesso() {
        AtestadoDTO dto = new AtestadoDTO();
        dto.setDiasAfastamento(3);
        dto.setCidOpcional("J00");

        Medico medico = new Medico();
        medico.setId(UUID.randomUUID());

        Atestado atestadoSalvo = new Atestado();
        atestadoSalvo.setId(UUID.randomUUID());
        atestadoSalvo.setPaciente(paciente);
        atestadoSalvo.setMedico(medico);
        atestadoSalvo.setDiasAfastamento(3);
        atestadoSalvo.setCidOpcional("J00");
        atestadoSalvo.setDataEmissao(java.time.LocalDateTime.now());

        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findByUsuarioEmail("medico@medflow.com")).thenReturn(Optional.of(medico));
        when(atestadoRepository.save(any(Atestado.class))).thenReturn(atestadoSalvo);

        AtestadoDTO resultado = pacienteService.emitirAtestado(pacienteId, dto, "medico@medflow.com");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getDiasAfastamento()).isEqualTo(3);
        verify(atestadoRepository).save(any(Atestado.class));
    }

    @Test
    @DisplayName("Deve deletar paciente quando ID existir")
    void delete_DeveDeletarComSucesso() {
        when(pacienteRepository.existsById(pacienteId)).thenReturn(true);

        pacienteService.delete(pacienteId);

        verify(pacienteRepository).deleteById(pacienteId);
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException ao deletar paciente inexistente")
    void delete_DeveLancarExcecao_QuandoNaoExiste() {
        when(pacienteRepository.existsById(pacienteId)).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.delete(pacienteId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
