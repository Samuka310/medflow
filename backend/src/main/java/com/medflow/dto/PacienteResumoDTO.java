package com.medflow.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PacienteResumoDTO {
    private UUID id;
    private String nome;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;
    
    // Futuros campos mencionados no roadmap (telefone, endereco, convenio) podem ser adicionados aqui
}
