package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "usuario_email", nullable = false)
    private String usuarioEmail;
    
    @Column(nullable = false)
    private String acao;
    
    @Column(nullable = false)
    private String entidade;
    
    @Column(name = "entidade_id")
    private UUID entidadeId;
    
    private String detalhes;
    
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();
}
