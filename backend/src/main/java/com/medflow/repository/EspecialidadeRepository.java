package com.medflow.repository;

import com.medflow.entity.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, UUID> {
    Optional<Especialidade> findByNomeIgnoreCase(String nome);
}
