package com.medflow.repository;

import com.medflow.entity.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TriagemRepository extends JpaRepository<Triagem, UUID> {
    Optional<Triagem> findByConsultaId(UUID consultaId);
}
