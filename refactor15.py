import os
import re

filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. create(PacienteDTO dto) -> create(PacienteDTO dto, String emailLogado, boolean isRecepcionista)
content = content.replace('public PacienteDTO create(PacienteDTO dto) {', 'public PacienteDTO create(PacienteDTO dto, String emailLogado, boolean isRecepcionista) {')
auth_create = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth == null\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\("Autentica[^"]*o obrigat[^"]*ria\."\);\s*\}\s*boolean isRecepcionista = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_RECEPCIONISTA"\) \|\| a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\);\s*'
content = re.sub(auth_create, '\n        ', content)
content = content.replace('String email = auth.getName();', 'String email = emailLogado;')

# 2. getResumo(UUID id)
auth_resumo = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = auth\.getName\(\);\s*if \(isPaciente && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\}\s*'
content = re.sub(auth_resumo, '\n        ', content)

# 3. getProntuarios(UUID id)
auth_prontuarios = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{\s*boolean isMedico = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_MEDICO"\)\);\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = auth\.getName\(\);\s*if \(isMedico\) \{\s*boolean temConsulta = consultaRepository\.existsByPacienteIdAndMedicoUsuarioEmail\(id, email\);\s*if \(!temConsulta\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\} else if \(isPaciente && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\}\s*'
content = re.sub(auth_prontuarios, '\n        ', content)

# 4. emitirReceita
content = content.replace('public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto) {', 'public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto, String medicoEmail) {')
auth_receita = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*String email = auth\.getName\(\);\s*'
content = re.sub(auth_receita, '\n        String email = medicoEmail;\n        ', content)

# 5. emitirAtestado
content = content.replace('public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto) {', 'public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto, String medicoEmail) {')
# auth_receita works here too since it matches exactly
content = re.sub(auth_receita, '\n        String email = medicoEmail;\n        ', content)

# Ensure no orphan imports
content = content.replace('import org.springframework.security.core.context.SecurityContextHolder;', '')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("PacienteService fixed!")
