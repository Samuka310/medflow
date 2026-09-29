package com.medflow.service;

import com.medflow.dto.FaturamentoRelatorioDTO;
import com.medflow.entity.Pagamento;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

    private final PagamentoRepository pagamentoRepository;

    public FaturamentoRelatorioDTO gerarRelatorio() {
        List<Pagamento> todosPagamentos = pagamentoRepository.findAll();
        
        FaturamentoRelatorioDTO relatorio = new FaturamentoRelatorioDTO();
        
        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.now();

        long pendentes = 0;
        long recebidos = 0;
        BigDecimal totalDia = BigDecimal.ZERO;
        BigDecimal totalMes = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;

        for (Pagamento p : todosPagamentos) {
            LocalDateTime dataConsulta = p.getConsulta().getDataHora();
            
            if ("PAGO".equals(p.getStatus())) {
                recebidos++;
                
                if (dataConsulta != null) {
                    if (dataConsulta.toLocalDate().equals(hoje)) {
                        totalDia = totalDia.add(p.getValor());
                    }
                    if (YearMonth.from(dataConsulta).equals(mesAtual)) {
                        totalMes = totalMes.add(p.getValor());
                    }
                }
            } else if ("PENDENTE".equals(p.getStatus())) {
                pendentes++;
                totalPendente = totalPendente.add(p.getValor());
            }
        }

        relatorio.setPagamentosPendentesCount(pendentes);
        relatorio.setPagamentosRecebidosCount(recebidos);
        relatorio.setTotalFaturadoDia(totalDia);
        relatorio.setTotalFaturadoMes(totalMes);
        relatorio.setTotalPendente(totalPendente);

        return relatorio;
    }
}
