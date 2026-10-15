package com.medflow.service;

import com.medflow.dto.AuthResponse;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.RefreshTokenRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.RefreshToken;
import com.medflow.entity.Usuario;
import com.medflow.repository.UsuarioRepository;
import com.medflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.medflow.exception.RegraDeNegocioException("E-mail já está em uso.");
        }

        Usuario user = new Usuario();
        user.setNome(request.getNome());
        user.setEmail(request.getEmail());
        user.setSenha(passwordEncoder.encode(request.getSenha()));
        user.setRole(com.medflow.entity.enums.Role.PACIENTE); // Segurança: registro público só cria pacientes

        usuarioRepository.save(user);

        return gerarRespostaAutenticacao(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getSenha()
                )
        );

        Usuario user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Usuário não encontrado"));

        return gerarRespostaAutenticacao(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken novoRefreshToken = refreshTokenService.rotacionarRefreshToken(request.getRefreshToken());
        Usuario usuario = novoRefreshToken.getUsuario();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", usuario.getRole());
        String novoAccessToken = jwtService.generateToken(extraClaims, usuario);

        return AuthResponse.builder()
                .token(novoAccessToken)
                .accessToken(novoAccessToken)
                .refreshToken(novoRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMillis() / 1000)
                .build();
    }

    @Transactional
    public void revogarToken(RefreshTokenRequest request) {
        refreshTokenService.revogarToken(request.getRefreshToken());
    }

    private AuthResponse gerarRespostaAutenticacao(Usuario user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        String jwtToken = jwtService.generateToken(extraClaims, user);

        RefreshToken refreshToken = refreshTokenService.criarRefreshToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMillis() / 1000)
                .build();
    }
}
