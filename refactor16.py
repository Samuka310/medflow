import os
import re

filepath = 'backend/src/main/java/com/medflow/controller/PacienteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. create
content = content.replace('public ResponseEntity<PacienteDTO> create(@Valid @RequestBody PacienteDTO dto) {', 'public ResponseEntity<PacienteDTO> create(@Valid @RequestBody PacienteDTO dto, org.springframework.security.core.Authentication auth) {')
content = content.replace('return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto));', 'boolean isRecepcionista = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RECEPCIONISTA") || a.getAuthority().equals("ROLE_ADMIN"));\n        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto, auth.getName(), isRecepcionista));')

# 2. getProntuarios
# Wait, for getProntuarios I removed the security check from service. I need to add it to AcessoSecurity.
# AcessoSecurity needs temConsulta check.
# And PacienteController already has @PreAuthorize for getResumo and others.

# 3. emitirReceita
content = content.replace('public ResponseEntity<ReceitaDTO> emitirReceita(@PathVariable UUID id, @Valid @RequestBody ReceitaDTO dto) {', 'public ResponseEntity<ReceitaDTO> emitirReceita(@PathVariable UUID id, @Valid @RequestBody ReceitaDTO dto, java.security.Principal principal) {')
content = content.replace('return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.emitirReceita(id, dto));', 'return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.emitirReceita(id, dto, principal.getName()));')

# 4. emitirAtestado
content = content.replace('public ResponseEntity<AtestadoDTO> emitirAtestado(@PathVariable UUID id, @Valid @RequestBody AtestadoDTO dto) {', 'public ResponseEntity<AtestadoDTO> emitirAtestado(@PathVariable UUID id, @Valid @RequestBody AtestadoDTO dto, java.security.Principal principal) {')
content = content.replace('return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.emitirAtestado(id, dto));', 'return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.emitirAtestado(id, dto, principal.getName()));')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("PacienteController fixed!")
