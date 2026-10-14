package com.medflow.repository;

import com.medflow.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {
    List<Consulta> findByMedicoIdAndDataHoraBetween(UUID medicoId, LocalDateTime start, LocalDateTime end);
    boolean existsByMedicoIdAndDataHoraBetween(UUID medicoId, LocalDateTime start, LocalDateTime end);
    List<Consulta> findByDataHoraBetween(LocalDateTime start, LocalDateTime end);
    boolean existsByPacienteIdAndDataHoraBetween(UUID pacienteId, LocalDateTime start, LocalDateTime end);
    boolean existsByPacienteIdAndMedicoUsuarioEmail(UUID pacienteId, String email);

    @Query("""
        SELECT COUNT(c) > 0 FROM Consulta c
        WHERE c.medico.id = :medicoId
          AND c.status <> com.medflow.entity.enums.ConsultaStatus.CANCELADA
          AND c.dataHora > :inicio AND c.dataHora < :fim
          AND (:consultaId IS NULL OR c.id <> :consultaId)
    """)
    boolean existsConflitoMedico(
        @Param("medicoId") UUID medicoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        @Param("consultaId") UUID consultaId
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Consulta c
        WHERE c.paciente.id = :pacienteId
          AND c.status <> com.medflow.entity.enums.ConsultaStatus.CANCELADA
          AND c.dataHora > :inicio AND c.dataHora < :fim
          AND (:consultaId IS NULL OR c.id <> :consultaId)
    """)
    boolean existsConflitoPaciente(
        @Param("pacienteId") UUID pacienteId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        @Param("consultaId") UUID consultaId
    );
}
