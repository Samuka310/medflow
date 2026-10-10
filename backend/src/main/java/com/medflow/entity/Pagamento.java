package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.entity.enums.FormaPagamento;

@Data
@Entity
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;
    
    private BigDecimal valor;
    
    @Enumerated(EnumType.STRING)
    private PagamentoStatus status;
    
    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;
}
