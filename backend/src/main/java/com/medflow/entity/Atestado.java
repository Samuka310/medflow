package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Atestado {
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
    
    @Column(name = "dias_afastamento", nullable = false)
    private Integer diasAfastamento;
    
    @Column(name = "cid_opcional")
    private String cidOpcional;
    
    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();
}
