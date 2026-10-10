import os
import re

# 1. PacienteService
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I will just remove the whole auth blocks that remained.
# Lines with org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
content = re.sub(r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*', '\n        ', content)
content = re.sub(r'if \(auth != null\) \{\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = emailLogado;\s*if \(isPaciente && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\}\s*', '', content)
content = re.sub(r'if \(auth != null\) \{\s*boolean isMedico = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_MEDICO"\)\);\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = emailLogado;\s*if \(isMedico\) \{\s*boolean temConsulta = consultaRepository\.existsByPacienteIdAndMedicoUsuarioEmail\(id, email\);\s*if \(!temConsulta\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\} else if \(isPaciente && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\}\s*', '', content)

content = content.replace('String email = emailLogado;\n        com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(email)', 'com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(medicoEmail)')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. ProntuarioService
filepath = 'backend/src/main/java/com/medflow/service/ProntuarioService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'\s*String email = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getName\(\);\s*if \(email != null && !prontuario\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*boolean isPaciente = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPaciente\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*\}\s*', '\n        ', content)

# 3. PacienteController
filepath = 'backend/src/main/java/com/medflow/controller/PacienteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto) {', 'public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto, java.security.Principal principal) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed!")
