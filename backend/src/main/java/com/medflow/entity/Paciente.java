package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String cpf;
    private LocalDate dataNascimento;
    
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
