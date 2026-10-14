package com.medflow.repository;

import com.medflow.entity.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, UUID> {
    boolean existsById(UUID id);
}
