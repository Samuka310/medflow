package com.medflow.controller;

import com.medflow.dto.AuthResponse;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.RefreshTokenRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.security.RsaKeyProvider;
import com.medflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos de autenticação, registro, renovação de token e chaves públicas")
public class AuthController {

    private final AuthService authService;
    private final RsaKeyProvider rsaKeyProvider;

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Cria um novo usuário com perfil de PACIENTE e devolve os tokens de acesso inicial")
    @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou campos obrigatórios ausentes",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou nome de usuário já cadastrado no sistema",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticação de usuário (Login)", description = "Valida credenciais e retorna Access Token (RS256) e Refresh Token com expiração e kid")
    @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Formato de requisição inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais incorretas ou usuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovação de Access Token (Refresh)", description = "Emite novo par de tokens a partir de um Refresh Token válido com rotação imediata")
    @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "403", description = "Refresh token expirado, revogado ou inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/revoke")
    @Operation(summary = "Revogar Refresh Token (Logout)", description = "Invalida o refresh token informado encerrando a sessão do dispositivo")
    @ApiResponse(responseCode = "204", description = "Token revogado com sucesso")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RefreshTokenRequest request) {
        authService.revogarToken(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/public-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chave pública RSA (JWK/PEM)", description = "Retorna a chave pública RSA ativa e respectivo kid para validação assimétrica de tokens na borda")
    @ApiResponse(responseCode = "200", description = "Chave pública retornada com sucesso")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "kid", rsaKeyProvider.getKeyId(),
                "algorithm", "RS256",
                "publicKey", rsaKeyProvider.getPublicKeyPem()
        ));
    }
}
