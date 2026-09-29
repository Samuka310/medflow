package com.medflow.service;

import com.medflow.dto.PagamentoDTO;
import com.medflow.entity.Pagamento;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoDTO pagar(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado."));

        if (!"PENDENTE".equals(pagamento.getStatus())) {
            throw new RuntimeException("Apenas pagamentos pendentes podem ser pagos.");
        }

        pagamento.setStatus("PAGO");
        pagamento = pagamentoRepository.save(pagamento);
        return mapToDTO(pagamento);
    }

    public PagamentoDTO estornar(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado."));

        if (!"PAGO".equals(pagamento.getStatus())) {
            throw new RuntimeException("Apenas pagamentos pagos podem ser estornados.");
        }

        pagamento.setStatus("ESTORNADO");
        pagamento = pagamentoRepository.save(pagamento);
        return mapToDTO(pagamento);
    }

    public PagamentoDTO findById(UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado."));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isPaciente = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
            if (isPaciente && !pagamento.getConsulta().getPaciente().getUsuario().getEmail().equals(auth.getName())) {
                throw new org.springframework.security.access.AccessDeniedException("Você só pode ver seus próprios pagamentos.");
            }
        }
        
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
