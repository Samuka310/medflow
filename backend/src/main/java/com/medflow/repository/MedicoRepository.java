package com.medflow.repository;

import com.medflow.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {
    Optional<Medico> findByCrm(String crm);
    java.util.List<Medico> findByEspecialidadesId(UUID especialidadeId);
    Optional<Medico> findByUsuarioEmail(String email);
}
