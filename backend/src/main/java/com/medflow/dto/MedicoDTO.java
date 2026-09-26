package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class MedicoDTO {
    private UUID id;

    @NotBlank(message = "O CRM é obrigatório")
    private String crm;

    @NotNull(message = "A especialidade é obrigatória")
    private UUID especialidadeId;

    @NotNull(message = "O usuário é obrigatório")
    private UUID usuarioId;
}
