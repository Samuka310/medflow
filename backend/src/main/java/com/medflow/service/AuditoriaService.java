package com.medflow.service;

import com.medflow.dto.AuditoriaDTO;
import com.medflow.entity.Auditoria;
import com.medflow.entity.Usuario;
import com.medflow.event.AuditoriaFalhaEvent;
import com.medflow.exception.FalhaAuditoriaException;
import com.medflow.metrics.MedflowMetricsService;
import com.medflow.repository.AuditoriaRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedflowMetricsService medflowMetricsService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void registrarAcao(String acao, String entidade, UUID entidadeId, String detalhes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return; // Requisições anônimas ou jobs sem contexto de segurança não possuem usuário para vínculo
        }
        registrarAcao(auth.getName(), acao, entidade, entidadeId, detalhes);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcao(String email, String acao, String entidade, UUID entidadeId, String detalhes) {
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
            if (usuario == null) {
                log.warn("[AUDITORIA_AVISO] Usuário autenticado '{}' não encontrado no banco de dados para registrar ação '{}' na entidade '{}'",
                        email, acao, entidade);
                return;
            }

            Auditoria aud = new Auditoria();
            aud.setUsuario(usuario);
            aud.setUsuarioEmail(usuario.getEmail());
            aud.setAcao(acao);
            aud.setEntidade(entidade);
            aud.setEntidadeId(entidadeId);
            aud.setDetalhes(detalhes);

            auditoriaRepository.save(aud);

            medflowMetricsService.registrarAuditoriaGravada(acao);
            log.info("[AUDITORIA] Ação='{}' registrada com sucesso. Entidade='{}', EntidadeId='{}', Usuário='{}'",
                    acao, entidade, entidadeId, email);

        } catch (Exception e) {
            MDC.put("audit.acao", acao);
            MDC.put("audit.entidade", entidade);
            MDC.put("audit.entidadeId", entidadeId != null ? entidadeId.toString() : "null");
            MDC.put("audit.usuario", email);

            try {
                log.error("[AUDITORIA_FALHA] Falha crítica de conformidade ao persistir registro de auditoria para ação='{}' na entidade='{}' (id={}) pelo usuário='{}': {}",
                        acao, entidade, entidadeId, email, e.getMessage(), e);

                medflowMetricsService.registrarFalhaAuditoria(acao);
                applicationEventPublisher.publishEvent(new AuditoriaFalhaEvent(acao, entidade, entidadeId, email, e.getMessage()));
            } finally {
                MDC.remove("audit.acao");
                MDC.remove("audit.entidade");
                MDC.remove("audit.entidadeId");
                MDC.remove("audit.usuario");
            }

            throw new FalhaAuditoriaException("Falha ao persistir auditoria de segurança para a ação: " + acao, e);
        }
    }

    public List<AuditoriaDTO> listarUltimos(int limite) {
        Page<Auditoria> page = auditoriaRepository.findAll(PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "dataHora")));
        return page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private AuditoriaDTO mapToDTO(Auditoria aud) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.setId(aud.getId());
        dto.setUsuarioId(aud.getUsuario().getId());
        dto.setUsuarioEmail(aud.getUsuarioEmail());
        dto.setAcao(aud.getAcao());
        dto.setEntidade(aud.getEntidade());
        dto.setEntidadeId(aud.getEntidadeId());
        dto.setDetalhes(aud.getDetalhes());
        dto.setDataHora(aud.getDataHora());
        return dto;
    }
}
