package com.medflow.security;

import com.medflow.repository.AtestadoRepository;
import com.medflow.repository.ConsultaRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.ProntuarioRepository;
import com.medflow.repository.PagamentoRepository;
import com.medflow.repository.ReceitaRepository;
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
    private final ReceitaRepository receitaRepository;
    private final AtestadoRepository atestadoRepository;

    private boolean isPaciente(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));
    }

    private boolean isMedico(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean temVinculoAssistencialComPaciente(UUID pacienteId, Authentication auth) {
        if (isAdmin(auth)) return true;
        if (isPaciente(auth)) {
            return pacienteRepository.findById(pacienteId)
                    .filter(p -> p.getUsuario() != null)
                    .map(p -> p.getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        if (isMedico(auth)) {
            return consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(pacienteId, auth.getName());
        }
        return false;
    }

    public boolean donoDaConsulta(UUID consultaId, Authentication auth) {
        if (isAdmin(auth)) return true;
        if (isPaciente(auth)) {
            return consultaRepository.findById(consultaId)
                    .filter(c -> c.getPaciente() != null && c.getPaciente().getUsuario() != null)
                    .map(c -> c.getPaciente().getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        if (isMedico(auth)) {
            return consultaRepository.findById(consultaId)
                    .filter(c -> c.getMedico() != null && c.getMedico().getUsuario() != null)
                    .map(c -> c.getMedico().getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        return false;
    }

    public boolean donoDoPaciente(UUID pacienteId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return pacienteRepository.findById(pacienteId)
                .filter(p -> p.getUsuario() != null)
                .map(p -> p.getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }

    public boolean donoDoProntuario(UUID prontuarioId, Authentication auth) {
        if (isAdmin(auth)) return true;
        if (isPaciente(auth)) {
            return prontuarioRepository.findById(prontuarioId)
                    .filter(p -> p.getConsulta() != null && p.getConsulta().getPaciente() != null && p.getConsulta().getPaciente().getUsuario() != null)
                    .map(p -> p.getConsulta().getPaciente().getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        if (isMedico(auth)) {
            return prontuarioRepository.findById(prontuarioId)
                    .filter(p -> p.getConsulta() != null && p.getConsulta().getPaciente() != null)
                    .map(p -> consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(
                            p.getConsulta().getPaciente().getId(), auth.getName()))
                    .orElse(false);
        }
        return false;
    }

    public boolean donoDoPagamento(UUID pagamentoId, Authentication auth) {
        if (!isPaciente(auth)) return true;
        return pagamentoRepository.findById(pagamentoId)
                .filter(p -> p.getConsulta() != null && p.getConsulta().getPaciente() != null && p.getConsulta().getPaciente().getUsuario() != null)
                .map(p -> p.getConsulta().getPaciente().getUsuario().getEmail().equals(auth.getName()))
                .orElse(false);
    }

    public boolean donoDaReceita(UUID receitaId, Authentication auth) {
        if (isAdmin(auth)) return true;
        if (isPaciente(auth)) {
            return receitaRepository.findById(receitaId)
                    .filter(r -> r.getPaciente() != null && r.getPaciente().getUsuario() != null)
                    .map(r -> r.getPaciente().getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        if (isMedico(auth)) {
            return receitaRepository.findById(receitaId)
                    .map(r -> (r.getMedico() != null && r.getMedico().getUsuario() != null && r.getMedico().getUsuario().getEmail().equals(auth.getName())) ||
                            (r.getPaciente() != null && consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(r.getPaciente().getId(), auth.getName())))
                    .orElse(false);
        }
        return false;
    }

    public boolean donoDoAtestado(UUID atestadoId, Authentication auth) {
        if (isAdmin(auth)) return true;
        if (isPaciente(auth)) {
            return atestadoRepository.findById(atestadoId)
                    .filter(a -> a.getPaciente() != null && a.getPaciente().getUsuario() != null)
                    .map(a -> a.getPaciente().getUsuario().getEmail().equals(auth.getName()))
                    .orElse(false);
        }
        if (isMedico(auth)) {
            return atestadoRepository.findById(atestadoId)
                    .map(a -> (a.getMedico() != null && a.getMedico().getUsuario() != null && a.getMedico().getUsuario().getEmail().equals(auth.getName())) ||
                            (a.getPaciente() != null && consultaRepository.existsByPacienteIdAndMedicoUsuarioEmail(a.getPaciente().getId(), auth.getName())))
                    .orElse(false);
        }
        return false;
    }
}
