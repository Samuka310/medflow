import os
import re

# 1. ProntuarioController
filepath = 'backend/src/main/java/com/medflow/controller/ProntuarioController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<List<ProntuarioDTO>> findMyHistorico() {', 'public ResponseEntity<List<ProntuarioDTO>> findMyHistorico(java.security.Principal principal) {')
content = content.replace('public ResponseEntity<ProntuarioDTO> findById(@PathVariable UUID id) {', 'public ResponseEntity<ProntuarioDTO> findById(@PathVariable UUID id, java.security.Principal principal) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. PacienteController
filepath = 'backend/src/main/java/com/medflow/controller/PacienteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto) {', 'public ResponseEntity<PacienteDTO> update(@PathVariable UUID id, @Valid @RequestBody PacienteDTO dto, java.security.Principal principal) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. PacienteService
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix create: remove oolean isRecepcionista = ... completely because it's a parameter.
content = re.sub(r'\s*boolean isRecepcionista = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_RECEPCIONISTA"\) \|\| a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\);\s*', '\n        ', content)

# Replace all emailLogado with what they should be based on method signatures.
# Actually I'll replace emailLogado with principal parameter where necessary.
# Let's fix update:
content = re.sub(r'public PacienteDTO update\(UUID id, PacienteDTO dto\) \{', 'public PacienteDTO update(UUID id, PacienteDTO dto, String emailLogado) {', content)
content = re.sub(r'public ReceitaDTO emitirReceita\(UUID pacienteId, ReceitaDTO dto\) \{', 'public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto, String emailLogado) {', content)
content = re.sub(r'public AtestadoDTO emitirAtestado\(UUID pacienteId, AtestadoDTO dto\) \{', 'public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto, String emailLogado) {', content)

# There might be some duplicate emailLogado replacements, I'll just change the parameters to String emailLogado so that String email = emailLogado; works!
# Wait, for emitirReceita, the parameter might already be String medicoEmail.
# Let's see if there is String medicoEmail
content = content.replace('public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto, String medicoEmail) {', 'public ReceitaDTO emitirReceita(UUID pacienteId, ReceitaDTO dto, String emailLogado) {')
content = content.replace('public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto, String medicoEmail) {', 'public AtestadoDTO emitirAtestado(UUID pacienteId, AtestadoDTO dto, String emailLogado) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed!")
