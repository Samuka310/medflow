package com.medflow.service;

import com.medflow.dto.FuncionarioRequest;
import com.medflow.entity.Funcionario;
import com.medflow.entity.Usuario;
import com.medflow.repository.FuncionarioRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Funcionario criarFuncionario(FuncionarioRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.medflow.exception.RegraDeNegocioException("E-mail já está em uso.");
        }

        Usuario user = new Usuario();
        user.setNome(request.getNome());
        user.setEmail(request.getEmail());
        user.setSenha(passwordEncoder.encode(request.getSenha()));
        
        // Valida e define a role baseada no cargo
        String role = request.getCargo().toUpperCase();
        if (com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.RECEPCIONISTA && com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.TRIAGEM && com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.FATURAMENTO) {
            throw new com.medflow.exception.RegraDeNegocioException("Cargo inválido. Use RECEPCIONISTA, TRIAGEM ou FATURAMENTO.");
        }
        user.setRole(com.medflow.entity.enums.Role.valueOf(role));
        
        user = usuarioRepository.save(user);

        Funcionario funcionario = new Funcionario();
        funcionario.setCargo(role);
        funcionario.setDataAdmissao(request.getDataAdmissao());
        funcionario.setUsuario(user);

        return funcionarioRepository.save(funcionario);
    }
}
