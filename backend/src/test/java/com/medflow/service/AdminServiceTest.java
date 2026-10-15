package com.medflow.service;

import com.medflow.dto.FuncionarioRequest;
import com.medflow.entity.Funcionario;
import com.medflow.entity.Usuario;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.FuncionarioRepository;
import com.medflow.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("Deve cadastrar colaborador com sucesso quando cargo válido")
    void criarFuncionario_DeveCriarComSucesso_QuandoCargoValido() {
        FuncionarioRequest request = new FuncionarioRequest();
        request.setNome("Ana Recepcionista");
        request.setEmail("ana@medflow.com");
        request.setSenha("senha123");
        request.setCargo("RECEPCIONISTA");
        request.setDataAdmissao(LocalDate.now());

        Usuario userSalvo = new Usuario();
        userSalvo.setId(UUID.randomUUID());
        userSalvo.setNome("Ana Recepcionista");
        userSalvo.setEmail("ana@medflow.com");

        Funcionario funcSalvo = new Funcionario();
        funcSalvo.setId(UUID.randomUUID());
        funcSalvo.setCargo("RECEPCIONISTA");
        funcSalvo.setUsuario(userSalvo);

        when(usuarioRepository.findByEmail("ana@medflow.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hash123");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(userSalvo);
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcSalvo);

        Funcionario resultado = adminService.criarFuncionario(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCargo()).isEqualTo("RECEPCIONISTA");
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando e-mail do colaborador já estiver cadastrado")
    void criarFuncionario_DeveLancarExcecao_QuandoEmailJaCadastrado() {
        FuncionarioRequest request = new FuncionarioRequest();
        request.setEmail("existente@medflow.com");

        when(usuarioRepository.findByEmail("existente@medflow.com")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> adminService.criarFuncionario(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("E-mail já está em uso");

        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando cargo for inválido")
    void criarFuncionario_DeveLancarExcecao_QuandoCargoInvalido() {
        FuncionarioRequest request = new FuncionarioRequest();
        request.setNome("Teste Invalido");
        request.setEmail("invalido@medflow.com");
        request.setSenha("123");
        request.setCargo("CARGO_DESCONHECIDO");

        when(usuarioRepository.findByEmail("invalido@medflow.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123")).thenReturn("hash");

        assertThatThrownBy(() -> adminService.criarFuncionario(request))
                .isInstanceOf(Exception.class);
    }
}
