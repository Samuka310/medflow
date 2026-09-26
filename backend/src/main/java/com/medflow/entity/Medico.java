package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Medico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String crm;
    
    @ManyToOne
    @JoinColumn(name = "especialidade_id")
    private Especialidade especialidade;
    
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
