package com.medflow.service;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    private final PagamentoRepository pagamentoRepository;

    @Transactional(readOnly = true)
    public FaturamentoRelatorioDTO gerarRelatorio() {
        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.now();

        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.plusDays(1).atStartOfDay();

        LocalDateTime inicioMes = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mesAtual.plusMonths(1).atDay(1).atStartOfDay();

        long pendentes = pagamentoRepository.countByStatus(PagamentoStatus.PENDENTE);
        long recebidos = pagamentoRepository.countByStatus(PagamentoStatus.PAGO);
        BigDecimal totalDia = pagamentoRepository.somarTotalPagoPeriodo(inicioDia, fimDia);
        BigDecimal totalMes = pagamentoRepository.somarTotalPagoPeriodo(inicioMes, fimMes);
        BigDecimal totalPendente = pagamentoRepository.somarTotalPendente();

        FaturamentoRelatorioDTO relatorio = new FaturamentoRelatorioDTO();
        relatorio.setPagamentosPendentesCount(pendentes);
        relatorio.setPagamentosRecebidosCount(recebidos);
        relatorio.setTotalFaturadoDia(totalDia);
        relatorio.setTotalFaturadoMes(totalMes);
        relatorio.setTotalPendente(totalPendente);

        return relatorio;
    }
}
