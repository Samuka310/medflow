package com.medflow.service;

import com.medflow.dto.AuditoriaDTO;
import com.medflow.entity.Auditoria;
import com.medflow.entity.Usuario;
import com.medflow.repository.AuditoriaRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public void registrarAcao(String acao, String entidade, UUID entidadeId, String detalhes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return; // Se não tem usuário logado (ex: rotas públicas ou jobs), não audita com usuário
            }
            
            String email = auth.getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
            
            if (usuario != null) {
                Auditoria aud = new Auditoria();
                aud.setUsuario(usuario);
                aud.setUsuarioEmail(usuario.getEmail());
                aud.setAcao(acao);
                aud.setEntidade(entidade);
                aud.setEntidadeId(entidadeId);
                aud.setDetalhes(detalhes);
                auditoriaRepository.save(aud);
            }
        } catch (Exception e) {
            // Logar silenciosamente, não queremos travar o fluxo principal se a auditoria falhar
            System.err.println("Erro ao registrar auditoria: " + e.getMessage());
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
