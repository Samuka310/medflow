package com.medflow.service;

import com.medflow.dto.AuthResponse;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.Usuario;
import com.medflow.repository.UsuarioRepository;
import com.medflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("E-mail já está em uso.");
        }

        Usuario user = new Usuario();
        user.setNome(request.getNome());
        user.setEmail(request.getEmail());
        user.setSenha(passwordEncoder.encode(request.getSenha()));
        user.setRole(request.getRole());
        
        usuarioRepository.save(user);

        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", user.getRole());
        String jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthResponse.builder().token(jwtToken).build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getSenha()
                )
        );

        Usuario user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
        extraClaims.put("role", user.getRole());
        String jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthResponse.builder().token(jwtToken).build();
    }
}
