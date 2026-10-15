package com.medflow.service;

import com.medflow.entity.RefreshToken;
import com.medflow.entity.Usuario;
import com.medflow.exception.AcessoNegadoException;
import com.medflow.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration-days:7}")
    private int refreshExpirationDays;

    @Transactional
    public RefreshToken criarRefreshToken(Usuario usuario) {
        RefreshToken rt = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .dataExpiracao(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS))
                .revogado(false)
                .dataCriacao(Instant.now())
                .build();
        return refreshTokenRepository.save(rt);
    }

    @Transactional
    public RefreshToken rotacionarRefreshToken(String tokenStr) {
        RefreshToken tokenAtual = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new AcessoNegadoException("Refresh token inválido ou não encontrado."));

        Usuario usuario = tokenAtual.getUsuario();

        // Detecção de Ataque de Replay: se um token já revogado for reutilizado, invalida toda a família de sessões
        if (tokenAtual.isRevogado()) {
            log.warn("[SEGURANÇA] Tentativa de reuso de Refresh Token já revogado para o usuário {}. Revogando todas as sessões ativas.",
                    usuario.getEmail());
            refreshTokenRepository.revogarTodosDoUsuario(usuario);
            throw new AcessoNegadoException("Tentativa de reúso de sessão detectada. Todas as sessões foram invalidadas por segurança.");
        }

        if (tokenAtual.isExpirado()) {
            tokenAtual.setRevogado(true);
            refreshTokenRepository.save(tokenAtual);
            throw new AcessoNegadoException("Refresh token expirado. Por favor, autentique-se novamente.");
        }

        // Rotação: invalida o token consumido
        tokenAtual.setRevogado(true);
        refreshTokenRepository.save(tokenAtual);

        // Emite novo token
        return criarRefreshToken(usuario);
    }

    @Transactional
    public void revogarToken(String tokenStr) {
        refreshTokenRepository.findByToken(tokenStr).ifPresent(rt -> {
            rt.setRevogado(true);
            refreshTokenRepository.save(rt);
        });
    }
}
