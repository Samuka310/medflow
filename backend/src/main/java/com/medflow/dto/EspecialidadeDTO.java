package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class EspecialidadeDTO {
    private UUID id;

    @NotBlank(message = "O nome da especialidade é obrigatório")
    private String nome;

    private String descricao;
}
