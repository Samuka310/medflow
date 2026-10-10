package com.medflow.security;

import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.ProntuarioRepository;
import com.medflow.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("acessoSecurity")
@RequiredArgsConstructor
public class AcessoSecurity {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final PagamentoRepository pagamentoRepository;

    private boolean isPaciente(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
    }

    public boolean donoDaConsulta(UUID consultaId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return consultaRepository.findById(consultaId)
                .map(c -> c.getPaciente().getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }

    public boolean donoDoPaciente(UUID pacienteId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return pacienteRepository.findById(pacienteId)
                .map(p -> p.getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }

    public boolean donoDoProntuario(UUID prontuarioId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return prontuarioRepository.findById(prontuarioId)
                .map(p -> p.getConsulta().getPaciente().getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }

    public boolean donoDoPagamento(UUID pagamentoId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return pagamentoRepository.findById(pagamentoId)
                .map(p -> p.getConsulta().getPaciente().getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }
}
