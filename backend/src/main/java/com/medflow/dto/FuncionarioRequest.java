package com.medflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FuncionarioRequest {
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;
    
    @NotBlank(message = "O cargo (role) é obrigatório")
    private String cargo; // RECEPCIONISTA, TRIAGEM, FATURAMENTO

    private LocalDate dataAdmissao;
}
