package com.medflow.repository;

import com.medflow.entity.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {
    Optional<Prontuario> findByConsultaId(UUID consultaId);
    List<Prontuario> findByConsultaPacienteId(UUID pacienteId);
}
