package com.medflow.builder;

import com.medflow.entity.enums.ConsultaStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class FiltroConsulta {

    private final UUID medicoId;
    private final UUID pacienteId;
    private final ConsultaStatus status;
    private final LocalDateTime dataInicio;
    private final LocalDateTime dataFim;

    private FiltroConsulta(Builder builder) {
        this.medicoId = builder.medicoId;
        this.pacienteId = builder.pacienteId;
        this.status = builder.status;
        this.dataInicio = builder.dataInicio;
        this.dataFim = builder.dataFim;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID medicoId;
        private UUID pacienteId;
        private ConsultaStatus status;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;

        public Builder comMedico(UUID medicoId) {
            this.medicoId = medicoId;
            return this;
        }

        public Builder comPaciente(UUID pacienteId) {
            this.pacienteId = pacienteId;
            return this;
        }

        public Builder comStatus(ConsultaStatus status) {
            this.status = status;
            return this;
        }

        public Builder noPeriodo(LocalDateTime inicio, LocalDateTime fim) {
            if (inicio != null && fim != null && inicio.isAfter(fim)) {
                throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
            }
            this.dataInicio = inicio;
            this.dataFim = fim;
            return this;
        }

        public FiltroConsulta build() {
            return new FiltroConsulta(this);
        }
    }
}
