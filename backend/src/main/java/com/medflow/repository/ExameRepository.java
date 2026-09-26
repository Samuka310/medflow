package com.medflow.repository;

import com.medflow.entity.Exame;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExameRepository extends JpaRepository<Exame, UUID> {
    List<Exame> findByProntuarioId(UUID prontuarioId);
}
