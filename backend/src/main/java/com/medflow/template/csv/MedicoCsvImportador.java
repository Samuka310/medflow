package com.medflow.template.csv;

import com.medflow.entity.Especialidade;
import com.medflow.entity.Medico;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.Role;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.EspecialidadeRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicoCsvImportador extends AbstractCsvImportador<Medico> {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SENHA_PADRAO = "MedFlow@2026";

    @Override
    protected boolean isCabecalho(String linha) {
        String lower = linha.toLowerCase();
        return lower.contains("crm") || lower.contains("nome");
    }

    @Override
    protected int getQuantidadeMinimaColunas() {
        return 3;
    }

    @Override
    protected void validarCamposObrigatorios(String[] colunas) {
        String nome = colunas[0].trim();
        String email = colunas[1].trim();
        String crm = colunas[2].trim();

        if (nome.isEmpty() || email.isEmpty() || crm.isEmpty()) {
            throw new RegraDeNegocioException("Campos obrigatórios vazios (nome, email ou crm).");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new RegraDeNegocioException("Email inválido: " + email);
        }
        if (medicoRepository.findByCrm(crm).isPresent()) {
            throw new RegraDeNegocioException("CRM já cadastrado no sistema: " + crm);
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RegraDeNegocioException("Email já cadastrado no sistema: " + email);
        }
    }

    @Override
    protected void processarRegistro(int numeroLinha, String[] colunas) {
        String nome = colunas[0].trim();
        String email = colunas[1].trim();
        String crm = colunas[2].trim();
        String especialidadeNome = colunas.length > 3 ? colunas[3].trim() : "Clínica Geral";

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO));
        usuario.setRole(Role.MEDICO);
        usuario = usuarioRepository.save(usuario);

        Especialidade especialidade = especialidadeRepository.findByNomeIgnoreCase(especialidadeNome)
                .orElseGet(() -> {
                    Especialidade esp = new Especialidade();
                    esp.setNome(especialidadeNome);
                    esp.setDescricao("Especialidade " + especialidadeNome);
                    return especialidadeRepository.save(esp);
                });

        Medico medico = new Medico();
        medico.setCrm(crm);
        medico.setUsuario(usuario);
        medico.setEspecialidades(List.of(especialidade));
        medicoRepository.save(medico);
    }
}
