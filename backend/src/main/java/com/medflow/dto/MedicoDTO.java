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

    @jakarta.validation.constraints.NotEmpty(message = "Pelo menos uma especialidade é obrigatória")
    private java.util.List<UUID> especialidadesIds;

    @NotNull(message = "O usuário é obrigatório")
    private UUID usuarioId;

    // Campos de resposta para o frontend
    private java.util.List<EspecialidadeDTO> especialidades;
    private UsuarioResumoDTO usuario;

    @Data
    public static class UsuarioResumoDTO {
        private UUID id;
        private String username;
        private String nome;
    }
}
