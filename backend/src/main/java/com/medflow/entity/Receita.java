package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Receita {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;
    
    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;
    
    @ManyToOne
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;
    
    @Column(nullable = false)
    private String medicamento;
    
    @Column(nullable = false)
    private String dosagem;
    
    @Column(name = "validade_data", nullable = false)
    private LocalDate validadeData;
    
    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();
}
