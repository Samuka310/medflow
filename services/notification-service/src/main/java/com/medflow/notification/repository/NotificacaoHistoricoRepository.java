package com.medflow.notification.repository;

import com.medflow.notification.entity.NotificacaoHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacaoHistoricoRepository extends JpaRepository<NotificacaoHistorico, UUID> {
    Page<NotificacaoHistorico> findByConsultaId(UUID consultaId, Pageable pageable);
    boolean existsByEventoId(UUID eventoId);
}
