package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "triagem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Triagem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    @ManyToOne
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private Funcionario realizadoPor;

    @Column(name = "pressao_arterial", length = 20)
    private String pressaoArterial;

    private BigDecimal temperatura;

    private BigDecimal peso;

    private BigDecimal altura;

    @Column(name = "queixa_principal", columnDefinition = "TEXT")
    private String queixaPrincipal;

    @Column(nullable = false, length = 20)
    private String prioridade;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
