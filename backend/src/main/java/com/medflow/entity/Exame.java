package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Exame {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "prontuario_id")
    private Prontuario prontuario;
    
    private String nomeArquivo;
    private String urlArquivo;
}
