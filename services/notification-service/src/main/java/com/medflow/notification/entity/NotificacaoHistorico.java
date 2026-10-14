package com.medflow.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacao_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "consulta_id", nullable = false)
    private UUID consultaId;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(length = 500)
    private String detalhes;

    @Column(nullable = false, length = 50)
    private String canal;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;
}
