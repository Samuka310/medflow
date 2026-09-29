package com.medflow.repository;

import com.medflow.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {
    boolean existsByMedicoIdAndDataHoraBetween(UUID medicoId, LocalDateTime start, LocalDateTime end);
    boolean existsByPacienteIdAndDataHoraBetween(UUID pacienteId, LocalDateTime start, LocalDateTime end);
    boolean existsByPacienteIdAndMedicoUsuarioEmail(UUID pacienteId, String email);
}
