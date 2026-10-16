package com.medflow.repository;

import com.medflow.entity.Pagamento;
import com.medflow.entity.enums.PagamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    Optional<Pagamento> findByConsultaId(UUID consultaId);
    List<Pagamento> findByConsultaIdIn(Collection<UUID> consultaIds);

    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p
        JOIN p.consulta c
        WHERE p.status = com.medflow.entity.enums.PagamentoStatus.PAGO
          AND c.dataHora >= :inicio AND c.dataHora < :fim
    """)
    BigDecimal somarTotalPagoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    long countByStatus(PagamentoStatus status);

    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p
        WHERE p.status = com.medflow.entity.enums.PagamentoStatus.PENDENTE
    """)
    BigDecimal somarTotalPendente();
}
