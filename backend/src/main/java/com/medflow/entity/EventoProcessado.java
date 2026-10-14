package com.medflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_processado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcessado {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;
}
