package com.medflow.service;

import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.dto.LinhaErroDTO;
import com.medflow.entity.Especialidade;
import com.medflow.entity.Medico;
import com.medflow.entity.Paciente;
import com.medflow.entity.Usuario;
import com.medflow.entity.enums.Role;
import com.medflow.repository.EspecialidadeRepository;
import com.medflow.repository.MedicoRepository;
import com.medflow.repository.PacienteRepository;
import com.medflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacaoCsvService {

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String SENHA_PADRAO_IMPORTACAO = "MedFlow@2026";

    @Transactional
    public ImportacaoResultadoDTO importarMedicosCsv(InputStream inputStream) {
        List<LinhaErroDTO> erros = new ArrayList<>();
        int totalLinhas = 0;
        int sucessos = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            boolean primeiraLinha = true;

            while ((linha = reader.readLine()) != null) {
                totalLinhas++;
                String linhaTrim = linha.trim();
                if (linhaTrim.isEmpty()) {
                    continue;
                }

                // Pula cabeçalho se contiver "nome" ou "crm"
                if (primeiraLinha) {
                    primeiraLinha = false;
                    if (linhaTrim.toLowerCase().contains("crm") || linhaTrim.toLowerCase().contains("nome")) {
                        continue;
                    }
                }

                String[] colunas = separarColunas(linhaTrim);
                if (colunas.length < 3) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Quantidade insuficiente de colunas (mínimo 3: nome,email,crm).", linhaTrim));
                    continue;
                }

                String nome = colunas[0].trim();
                String email = colunas[1].trim();
                String crm = colunas[2].trim();
                String especialidadeNome = colunas.length > 3 ? colunas[3].trim() : "Clínica Geral";

                if (nome.isEmpty() || email.isEmpty() || crm.isEmpty()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Campos obrigatórios vazios (nome, email ou crm).", linhaTrim));
                    continue;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Email inválido: " + email, linhaTrim));
                    continue;
                }

                if (medicoRepository.findByCrm(crm).isPresent()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "CRM já cadastrado no sistema: " + crm, linhaTrim));
                    continue;
                }

                if (usuarioRepository.findByEmail(email).isPresent()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Email já cadastrado no sistema: " + email, linhaTrim));
                    continue;
                }

                try {
                    Usuario usuario = new Usuario();
                    usuario.setNome(nome);
                    usuario.setEmail(email);
                    usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO_IMPORTACAO));
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

                    sucessos++;
                } catch (Exception ex) {
                    log.error("Erro ao persistir médico na linha {}", totalLinhas, ex);
                    erros.add(new LinhaErroDTO(totalLinhas, "Erro ao persistir médico: " + ex.getMessage(), linhaTrim));
                }
            }
        } catch (IOException e) {
            log.error("Falha de I/O ao ler arquivo CSV de médicos", e);
            throw new RuntimeException("Erro ao processar arquivo CSV: " + e.getMessage(), e);
        }

        return ImportacaoResultadoDTO.criar(totalLinhas, sucessos, erros);
    }

    @Transactional
    public ImportacaoResultadoDTO importarPacientesCsv(InputStream inputStream) {
        List<LinhaErroDTO> erros = new ArrayList<>();
        int totalLinhas = 0;
        int sucessos = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            boolean primeiraLinha = true;

            while ((linha = reader.readLine()) != null) {
                totalLinhas++;
                String linhaTrim = linha.trim();
                if (linhaTrim.isEmpty()) {
                    continue;
                }

                if (primeiraLinha) {
                    primeiraLinha = false;
                    if (linhaTrim.toLowerCase().contains("cpf") || linhaTrim.toLowerCase().contains("nome")) {
                        continue;
                    }
                }

                String[] colunas = separarColunas(linhaTrim);
                if (colunas.length < 4) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Quantidade insuficiente de colunas (mínimo 4: nome,email,cpf,dataNascimento).", linhaTrim));
                    continue;
                }

                String nome = colunas[0].trim();
                String email = colunas[1].trim();
                String cpf = colunas[2].trim().replaceAll("[^0-9]", "");
                String dataNascStr = colunas[3].trim();

                if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty() || dataNascStr.isEmpty()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Campos obrigatórios vazios.", linhaTrim));
                    continue;
                }

                if (cpf.length() != 11) {
                    erros.add(new LinhaErroDTO(totalLinhas, "CPF com formato inválido (deve conter 11 dígitos): " + cpf, linhaTrim));
                    continue;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Email inválido: " + email, linhaTrim));
                    continue;
                }

                if (pacienteRepository.findByCpf(cpf).isPresent()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "CPF já cadastrado no sistema: " + cpf, linhaTrim));
                    continue;
                }

                if (usuarioRepository.findByEmail(email).isPresent()) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Email já cadastrado no sistema: " + email, linhaTrim));
                    continue;
                }

                LocalDate dataNascimento;
                try {
                    dataNascimento = parseData(dataNascStr);
                } catch (DateTimeParseException ex) {
                    erros.add(new LinhaErroDTO(totalLinhas, "Formato de data inválido ('" + dataNascStr + "'). Esperado: YYYY-MM-DD ou DD/MM/YYYY.", linhaTrim));
                    continue;
                }

                try {
                    Usuario usuario = new Usuario();
                    usuario.setNome(nome);
                    usuario.setEmail(email);
                    usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO_IMPORTACAO));
                    usuario.setRole(Role.PACIENTE);
                    usuario = usuarioRepository.save(usuario);

                    Paciente paciente = new Paciente();
                    paciente.setCpf(cpf);
                    paciente.setDataNascimento(dataNascimento);
                    paciente.setUsuario(usuario);
                    pacienteRepository.save(paciente);

                    sucessos++;
                } catch (Exception ex) {
                    log.error("Erro ao persistir paciente na linha {}", totalLinhas, ex);
                    erros.add(new LinhaErroDTO(totalLinhas, "Erro ao persistir paciente: " + ex.getMessage(), linhaTrim));
                }
            }
        } catch (IOException e) {
            log.error("Falha de I/O ao ler arquivo CSV de pacientes", e);
            throw new RuntimeException("Erro ao processar arquivo CSV: " + e.getMessage(), e);
        }

        return ImportacaoResultadoDTO.criar(totalLinhas, sucessos, erros);
    }

    private String[] separarColunas(String linha) {
        if (linha.contains(";")) {
            return linha.split(";");
        }
        return linha.split(",");
    }

    private LocalDate parseData(String str) {
        if (str.contains("/")) {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
