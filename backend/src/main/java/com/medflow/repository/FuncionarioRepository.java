package com.medflow.repository;

import com.medflow.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    Optional<Funcionario> findByUsuarioEmail(String email);
}
