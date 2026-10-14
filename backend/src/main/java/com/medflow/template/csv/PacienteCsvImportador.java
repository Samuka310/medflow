package com.medflow.template.csv;

import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.Role;
import com.medflow.exception.RegraDeNegocioException;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class PacienteCsvImportador extends AbstractCsvImportador<Paciente> {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SENHA_PADRAO = "MedFlow@2026";

    @Override
    protected boolean isCabecalho(String linha) {
        String lower = linha.toLowerCase();
        return lower.contains("cpf") || lower.contains("nome");
    }

    @Override
    protected int getQuantidadeMinimaColunas() {
        return 4;
    }

    @Override
    protected void validarCamposObrigatorios(String[] colunas) {
        String nome = colunas[0].trim();
        String email = colunas[1].trim();
        String cpf = colunas[2].trim().replaceAll("[^0-9]", "");
        String dataNascStr = colunas[3].trim();

        if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty() || dataNascStr.isEmpty()) {
            throw new RegraDeNegocioException("Campos obrigatórios vazios.");
        }
        if (cpf.length() != 11) {
            throw new RegraDeNegocioException("CPF com formato inválido (deve conter 11 dígitos): " + cpf);
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new RegraDeNegocioException("Email inválido: " + email);
        }
        if (pacienteRepository.findByCpf(cpf).isPresent()) {
            throw new RegraDeNegocioException("CPF já cadastrado no sistema: " + cpf);
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RegraDeNegocioException("Email já cadastrado no sistema: " + email);
        }
    }

    @Override
    protected void processarRegistro(int numeroLinha, String[] colunas) {
        String nome = colunas[0].trim();
        String email = colunas[1].trim();
        String cpf = colunas[2].trim().replaceAll("[^0-9]", "");
        String dataNascStr = colunas[3].trim();

        LocalDate dataNascimento;
        try {
            dataNascimento = parseData(dataNascStr);
        } catch (DateTimeParseException ex) {
            throw new RegraDeNegocioException("Formato de data inválido ('" + dataNascStr + "'). Esperado: YYYY-MM-DD ou DD/MM/YYYY.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO));
        usuario.setRole(Role.PACIENTE);
        usuario = usuarioRepository.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setCpf(cpf);
        paciente.setDataNascimento(dataNascimento);
        paciente.setUsuario(usuario);
        pacienteRepository.save(paciente);
    }

    private LocalDate parseData(String str) {
        if (str.contains("/")) {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
