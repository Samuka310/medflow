package com.medflow.repository;

import com.medflow.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    Optional<Paciente> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    Optional<Paciente> findByUsuarioEmail(String email);
    Optional<Paciente> findByUsuarioId(UUID usuarioId);
    boolean existsByUsuarioId(UUID usuarioId);
}
