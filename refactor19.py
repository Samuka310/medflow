import os
import re

filepath = 'backend/src/main/java/com/medflow/controller/ProntuarioController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<ProntuarioDTO> adicionarEvolucao(@PathVariable UUID id, @Valid @RequestBody EvolucaoDTO dto) {', 'public ResponseEntity<ProntuarioDTO> adicionarEvolucao(@PathVariable UUID id, @Valid @RequestBody EvolucaoDTO dto, java.security.Principal principal) {')
content = content.replace('return ResponseEntity.ok(prontuarioService.adicionarEvolucao(id, dto));', 'return ResponseEntity.ok(prontuarioService.adicionarEvolucao(id, dto, principal.getName()));')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ProntuarioController fixed!")
