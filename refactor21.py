import os
import re

# PacienteController update method fixing
filepath = 'backend/src/main/java/com/medflow/controller/PacienteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto, java.security.Principal principal) {', 'public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto) {')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# PacienteService clean auth in update, delete
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# I will replace the auth block in update (lines 104-107) manually by removing it
content = re.sub(r'\s*if \(auth \!\= null\) \{\s*boolean isPacienteLogado \= auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a \-\> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado \&\& \!paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(emailLogado\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\("Acesso Negado"\);\s*\}\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'\s*if \(auth \!\= null\) \{\s*boolean isPacienteLogado \= auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a \-\> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado \&\& \!paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*\}', '', content, flags=re.DOTALL)

# For emitirReceita and emitirAtestado, replace medicoEmail with String email = auth.getName() again, NO WAIT I want to remove auth from service!
# Let's fix emitirReceita/emitirAtestado:
content = re.sub(r'public ReceitaDTO emitirReceita\(UUID pacienteId, ReceitaDTO dto, String medicoEmail\) \{\s*Paciente paciente \= pacienteRepository\.findById\(pacienteId\)\s*\.orElseThrow\(\(\) \-\> new RuntimeException\("Paciente não encontrado\."\)\);\s*com\.medflow\.entity\.Medico medico \= medicoRepository\.findByUsuarioEmail\(medicoEmail\)\s*\.orElseThrow\(\(\) \-\> new RuntimeException\("Médico não encontrado\."\)\);', 
r'public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto, String medicoEmail) {\n        Paciente paciente = pacienteRepository.findById(pacienteId)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado."));\n        com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(medicoEmail)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Médico não encontrado."));', content, flags=re.DOTALL)

content = re.sub(r'public AtestadoDTO emitirAtestado\(UUID pacienteId, AtestadoDTO dto, String medicoEmail\) \{\s*Paciente paciente \= pacienteRepository\.findById\(pacienteId\)\s*\.orElseThrow\(\(\) \-\> new RuntimeException\("Paciente não encontrado\."\)\);\s*com\.medflow\.entity\.Medico medico \= medicoRepository\.findByUsuarioEmail\(medicoEmail\)\s*\.orElseThrow\(\(\) \-\> new RuntimeException\("Médico não encontrado\."\)\);', 
r'public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto, String medicoEmail) {\n        Paciente paciente = pacienteRepository.findById(pacienteId)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado."));\n        com.medflow.entity.Medico medico = medicoRepository.findByUsuarioEmail(medicoEmail)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Médico não encontrado."));', content, flags=re.DOTALL)


# Delete ANY remaining uth. in PacienteService
lines = content.split('\n')
new_lines = []
for line in lines:
    if 'org.springframework.security.core.Authentication auth =' in line:
        continue
    if 'if (auth ' in line:
        continue
    if 'boolean isPacienteLogado = auth.' in line:
        continue
    if 'boolean isRecepcionista = auth.' in line:
        continue
    if 'if (isPacienteLogado' in line:
        continue
    if 'throw new com.medflow.exception.AcessoNegadoException(' in line and 'Acesso Negado' in line:
        continue
    if 'auth.getName()' in line:
        continue
    new_lines.append(line)

content = '\n'.join(new_lines)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. ProntuarioService
filepath = 'backend/src/main/java/com/medflow/service/ProntuarioService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Delete ALL SecurityContextHolder references
lines = content.split('\n')
new_lines = []
for line in lines:
    if 'SecurityContextHolder' in line:
        continue
    if 'String email =' in line and 'SecurityContextHolder' in line:
        continue
    if 'if (email != null' in line:
        continue
    if 'boolean isPaciente = ' in line:
        continue
    if 'if (isPaciente)' in line:
        continue
    new_lines.append(line)

content = '\n'.join(new_lines)
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed again!")
