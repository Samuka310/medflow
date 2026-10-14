package com.medflow.service;

import com.medflow.dto.PagamentoDTO;
import com.medflow.entity.Pagamento;
import com.medflow.entity.enums.PagamentoStatus;
import com.medflow.exception.RecursoNaoEncontradoException;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PagamentoDTO pagar(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));

        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new RegraDeNegocioException("Apenas pagamentos pendentes podem ser pagos.");
        }

        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamento = pagamentoRepository.save(pagamento);
        auditoriaService.registrarAcao("EVENTO_PAGAMENTO", "Pagamento", pagamento.getId(), "Operacao: " + pagamento.getStatus());
        return mapToDTO(pagamento);
    }

    @Transactional
    public PagamentoDTO estornar(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));

        if (pagamento.getStatus() != PagamentoStatus.PAGO) {
            throw new RegraDeNegocioException("Apenas pagamentos pagos podem ser estornados.");
        }

        pagamento.setStatus(PagamentoStatus.ESTORNADO);
        pagamento = pagamentoRepository.save(pagamento);
        auditoriaService.registrarAcao("EVENTO_PAGAMENTO", "Pagamento", pagamento.getId(), "Operacao: " + pagamento.getStatus());
        return mapToDTO(pagamento);
    }

    public PagamentoDTO findById(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));

        auditoriaService.registrarAcao("EVENTO_PAGAMENTO", "Pagamento", pagamento.getId(), "Operacao: " + pagamento.getStatus());
        return mapToDTO(pagamento);
    }

    private PagamentoDTO mapToDTO(Pagamento pagamento) {
        PagamentoDTO dto = new PagamentoDTO();
        dto.setId(pagamento.getId());
        dto.setConsultaId(pagamento.getConsulta().getId());
        dto.setValor(pagamento.getValor());
        dto.setStatus(pagamento.getStatus());
        dto.setFormaPagamento(pagamento.getFormaPagamento());
        return dto;
    }
}
