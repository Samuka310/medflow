package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"medico_id", "data_hora"}))
public class Consulta {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "medico_id")
    private Medico medico;
    
    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;
    
    @Column(name = "data_hora")
    private LocalDateTime dataHora;
    
    private String status;
}
