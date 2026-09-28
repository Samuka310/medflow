package com.medflow.controller;

import com.medflow.dto.FuncionarioRequest;
import com.medflow.entity.Funcionario;
import com.medflow.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/funcionarios")
    public ResponseEntity<Funcionario> criarFuncionario(@Valid @RequestBody FuncionarioRequest request) {
        return ResponseEntity.ok(adminService.criarFuncionario(request));
    }
}
