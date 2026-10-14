package com.medflow.controller;

import com.medflow.dto.FuncionarioRequest;
import com.medflow.entity.Funcionario;
import com.medflow.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administração", description = "Operações administrativas do sistema e gestão de colaboradores")
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/funcionarios")
    @Operation(summary = "Cadastrar colaborador/funcionário", description = "Cria um novo funcionário da clínica associado a um perfil de usuário (Acesso exclusivo ADMIN)")
    @ApiResponse(responseCode = "200", description = "Funcionário cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = Funcionario.class)))
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado - Requer perfil ADMIN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de unicidade (ex: e-mail ou CPF já existente)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Funcionario> criarFuncionario(@Valid @RequestBody FuncionarioRequest request) {
        return ResponseEntity.ok(adminService.criarFuncionario(request));
    }
}
