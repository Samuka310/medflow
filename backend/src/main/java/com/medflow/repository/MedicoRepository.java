package com.medflow.repository;

import com.medflow.entity.Medico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {
    Optional<Medico> findByCrm(String crm);
    java.util.List<Medico> findByEspecialidadesId(UUID especialidadeId);
    Optional<Medico> findByUsuarioEmail(String email);
    Optional<Medico> findByUsuarioId(UUID usuarioId);
    boolean existsByUsuarioId(UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medico m WHERE m.id = :id")
    Optional<Medico> findByIdWithLock(@Param("id") UUID id);
}
