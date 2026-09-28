package com.medflow.repository;

import com.medflow.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
}
