package com.medflow.config;

import com.medflow.entity.*;
import com.medflow.entity.enums.Role;
import com.medflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoRepository medicoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando dados padrão de demonstração do MedFlow...");

        // 1. Especialidades
        Especialidade cardio = getOrCreateEspecialidade("Cardiologia", "Cardiologia Geral e Diagnóstico Cardiovascular", new BigDecimal("250.00"));
        getOrCreateEspecialidade("Ortopedia", "Traumatologia e Ortopedia Geral", new BigDecimal("200.00"));
        getOrCreateEspecialidade("Dermatologia", "Dermatologia Clínica e Estética", new BigDecimal("180.00"));
        getOrCreateEspecialidade("Clínica Geral", "Atendimento Clínico Primário", new BigDecimal("150.00"));
        getOrCreateEspecialidade("Pediatria", "Saúde Infantil e Neonatologia", new BigDecimal("190.00"));

        // 2. Admin
        if (usuarioRepository.findByEmail("admin@medflow.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador Geral");
            admin.setEmail("admin@medflow.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            usuarioRepository.save(admin);
            log.info("Usuário ADMIN inicial criado: admin@medflow.com");
        }

        // 3. Médico
        if (usuarioRepository.findByEmail("medico@medflow.com").isEmpty()) {
            Usuario userMedico = new Usuario();
            userMedico.setNome("Dr. Carlos Eduardo");
            userMedico.setEmail("medico@medflow.com");
            userMedico.setSenha(passwordEncoder.encode("medico123"));
            userMedico.setRole(Role.MEDICO);
            usuarioRepository.save(userMedico);

            Medico medico = new Medico();
            medico.setCrm("123456-SP");
            medico.setUsuario(userMedico);
            medico.setEspecialidades(List.of(cardio));
            medicoRepository.save(medico);
            log.info("Usuário MÉDICO inicial criado: medico@medflow.com (CRM 123456-SP)");
        }

        // 4. Recepcionista
        if (usuarioRepository.findByEmail("recepcao@medflow.com").isEmpty()) {
            Usuario userRecepcao = new Usuario();
            userRecepcao.setNome("Mariana Souza");
            userRecepcao.setEmail("recepcao@medflow.com");
            userRecepcao.setSenha(passwordEncoder.encode("recepcao123"));
            userRecepcao.setRole(Role.RECEPCIONISTA);
            usuarioRepository.save(userRecepcao);

            Funcionario func = new Funcionario();
            func.setCargo("RECEPCIONISTA");
            func.setDataAdmissao(LocalDate.now());
            func.setUsuario(userRecepcao);
            funcionarioRepository.save(func);
            log.info("Usuário RECEPCIONISTA inicial criado: recepcao@medflow.com");
        }

        // 5. Triagem
        if (usuarioRepository.findByEmail("triagem@medflow.com").isEmpty()) {
            Usuario userTriagem = new Usuario();
            userTriagem.setNome("Enf. Roberto Lima");
            userTriagem.setEmail("triagem@medflow.com");
            userTriagem.setSenha(passwordEncoder.encode("triagem123"));
            userTriagem.setRole(Role.TRIAGEM);
            usuarioRepository.save(userTriagem);

            Funcionario func = new Funcionario();
            func.setCargo("TRIAGEM");
            func.setDataAdmissao(LocalDate.now());
            func.setUsuario(userTriagem);
            funcionarioRepository.save(func);
            log.info("Usuário TRIAGEM inicial criado: triagem@medflow.com");
        }

        // 6. Faturamento
        if (usuarioRepository.findByEmail("faturamento@medflow.com").isEmpty()) {
            Usuario userFat = new Usuario();
            userFat.setNome("Fernanda Castro");
            userFat.setEmail("faturamento@medflow.com");
            userFat.setSenha(passwordEncoder.encode("faturamento123"));
            userFat.setRole(Role.FATURAMENTO);
            usuarioRepository.save(userFat);

            Funcionario func = new Funcionario();
            func.setCargo("FATURAMENTO");
            func.setDataAdmissao(LocalDate.now());
            func.setUsuario(userFat);
            funcionarioRepository.save(func);
            log.info("Usuário FATURAMENTO inicial criado: faturamento@medflow.com");
        }

        // 7. Paciente
        if (usuarioRepository.findByEmail("paciente@medflow.com").isEmpty()) {
            Usuario userPac = new Usuario();
            userPac.setNome("João da Silva");
            userPac.setEmail("paciente@medflow.com");
            userPac.setSenha(passwordEncoder.encode("paciente123"));
            userPac.setRole(Role.PACIENTE);
            usuarioRepository.save(userPac);

            Paciente pac = new Paciente();
            pac.setCpf("123.456.789-00");
            pac.setDataNascimento(LocalDate.of(1990, 5, 15));
            pac.setUsuario(userPac);
            pacienteRepository.save(pac);
            log.info("Usuário PACIENTE inicial criado: paciente@medflow.com");
        }
    }

    private Especialidade getOrCreateEspecialidade(String nome, String descricao, BigDecimal valor) {
        return especialidadeRepository.findByNomeIgnoreCase(nome).orElseGet(() -> {
            Especialidade esp = new Especialidade();
            esp.setNome(nome);
            esp.setDescricao(descricao);
            esp.setValorConsulta(valor);
            return especialidadeRepository.save(esp);
        });
    }
}
