package com.medflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PacienteDTO {
    private UUID id;

    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;

    @NotNull(message = "A data de nascimento é obrigatória")
    private LocalDate dataNascimento;

    private UUID usuarioId;

    // Campos opcionais para quando a recepção cadastra um paciente do zero
    private String nome;
    private String email;
    private String senha;
}
