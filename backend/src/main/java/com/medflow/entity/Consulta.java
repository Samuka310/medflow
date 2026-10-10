package com.medflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import com.medflow.entity.enums.ConsultaStatus;

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
    
    @Enumerated(EnumType.STRING)
    private ConsultaStatus status;

    public void confirmar() {
        transitarPara(ConsultaStatus.CONFIRMADA);
    }

    public void aguardarTriagem() {
        transitarPara(ConsultaStatus.AGUARDANDO_TRIAGEM);
    }

    public void realizar() {
        transitarPara(ConsultaStatus.REALIZADA);
    }

    public void cancelar() {
        transitarPara(ConsultaStatus.CANCELADA);
    }

    private void transitarPara(ConsultaStatus novoStatus) {
        if (this.status != null && !this.status.podeTransitarPara(novoStatus)) {
            throw new IllegalStateException("Não é possível transitar a consulta de " + this.status + " para " + novoStatus);
        }
        this.status = novoStatus;
    }
}
