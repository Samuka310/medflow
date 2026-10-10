import os
import re

# PacienteController
filepath = 'backend/src/main/java/com/medflow/controller/PacienteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<com.medflow.dto.ReceitaDTO> prescreverReceita(@PathVariable UUID id, @Valid @RequestBody com.medflow.dto.ReceitaDTO dto) {', 'public ResponseEntity<com.medflow.dto.ReceitaDTO> prescreverReceita(@PathVariable UUID id, @Valid @RequestBody com.medflow.dto.ReceitaDTO dto, java.security.Principal principal) {')
content = content.replace('return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.prescreverReceita(id, dto));', 'return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.prescreverReceita(id, dto, principal.getName()));')

content = content.replace('public ResponseEntity<com.medflow.dto.AtestadoDTO> emitirAtestado(@PathVariable UUID id, @Valid @RequestBody com.medflow.dto.AtestadoDTO dto) {', 'public ResponseEntity<com.medflow.dto.AtestadoDTO> emitirAtestado(@PathVariable UUID id, @Valid @RequestBody com.medflow.dto.AtestadoDTO dto, java.security.Principal principal) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# PacienteService
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public com.medflow.dto.ReceitaDTO prescreverReceita(UUID pacienteId, com.medflow.dto.ReceitaDTO dto) {', 'public com.medflow.dto.ReceitaDTO prescreverReceita(UUID pacienteId, com.medflow.dto.ReceitaDTO dto, String emailLogado) {')
content = content.replace('public com.medflow.dto.AtestadoDTO emitirAtestado(UUID pacienteId, com.medflow.dto.AtestadoDTO dto) {', 'public com.medflow.dto.AtestadoDTO emitirAtestado(UUID pacienteId, com.medflow.dto.AtestadoDTO dto, String emailLogado) {')

# The errors in PacienteService are:
# isRecepcionista is already defined
# mailLogado not found
content = re.sub(r'\s*boolean isRecepcionista = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_RECEPCIONISTA"\) \|\| a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\);\s*', '\n        ', content)

# Remove all leftover auth stuff in PacienteService
content = re.sub(r'org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);', '', content)
content = content.replace('String email = auth.getName();', 'String email = emailLogado;')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed!")
