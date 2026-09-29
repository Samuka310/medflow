package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Prontuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;
    
    @Column(name = "diagnostico_cid")
    private String diagnosticoCid;
    
    @Column(name = "evolucao_clinica")
    private String evolucaoClinica;
    
    @Column(name = "conduta_medica")
    private String condutaMedica;
    
    private String observacoes;
}
