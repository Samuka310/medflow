package com.medflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Builder.Default
    @Column(nullable = false)
    private boolean revogado = false;

    @Builder.Default
    @Column(name = "data_criacao", nullable = false)
    private Instant dataCriacao = Instant.now();

    public boolean isExpirado() {
        return Instant.now().isAfter(this.dataExpiracao);
    }

    public boolean isAtivo() {
        return !revogado && !isExpirado();
    }
}
