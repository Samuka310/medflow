package com.medflow.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PacienteClinicoDTO extends PacienteResumoDTO {
    
    // Este DTO herda os dados cadastrais e adiciona os dados clínicos.
    // Futuros relacionamentos (historico, alergias, prontuarios, condicoes) serao adicionados aqui.
    private String historicoClinicoResumo = "Sem histórico clínico registrado ainda.";
    
}
