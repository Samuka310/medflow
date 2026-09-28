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
            throw new RuntimeException("E-mail já está em uso.");
        }

        Usuario user = new Usuario();
        user.setNome(request.getNome());
        user.setEmail(request.getEmail());
        user.setSenha(passwordEncoder.encode(request.getSenha()));
        
        // Valida e define a role baseada no cargo
        String role = request.getCargo().toUpperCase();
        if (!role.equals("RECEPCIONISTA") && !role.equals("TRIAGEM") && !role.equals("FATURAMENTO")) {
            throw new RuntimeException("Cargo inválido. Use RECEPCIONISTA, TRIAGEM ou FATURAMENTO.");
        }
        user.setRole(role);
        
        user = usuarioRepository.save(user);

        Funcionario funcionario = new Funcionario();
        funcionario.setCargo(role);
        funcionario.setDataAdmissao(request.getDataAdmissao());
        funcionario.setUsuario(user);

        return funcionarioRepository.save(funcionario);
    }
}
