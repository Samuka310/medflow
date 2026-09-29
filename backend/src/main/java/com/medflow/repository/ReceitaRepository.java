package com.medflow.repository;

import com.medflow.entity.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReceitaRepository extends JpaRepository<Receita, UUID> {
    List<Receita> findByPacienteId(UUID pacienteId);
}
