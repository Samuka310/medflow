package com.medflow.repository;

import com.medflow.entity.Atestado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AtestadoRepository extends JpaRepository<Atestado, UUID> {
    List<Atestado> findByPacienteId(UUID pacienteId);
}
