import os
import re

# ProntuarioService fix email
filepath = 'backend/src/main/java/com/medflow/service/ProntuarioService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public List<ProntuarioDTO> findMyHistorico() {', 'public List<ProntuarioDTO> findMyHistorico(String email) {')
content = content.replace('public ProntuarioDTO findById(UUID id) {', 'public ProntuarioDTO findById(UUID id, String email) {')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# ProntuarioController fix email
filepath = 'backend/src/main/java/com/medflow/controller/ProntuarioController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('public ResponseEntity<List<ProntuarioDTO>> getMeusProntuarios() {', 'public ResponseEntity<List<ProntuarioDTO>> getMeusProntuarios(java.security.Principal principal) {')
content = content.replace('return ResponseEntity.ok(prontuarioService.findMyHistorico());', 'return ResponseEntity.ok(prontuarioService.findMyHistorico(principal.getName()));')

content = content.replace('public ResponseEntity<ProntuarioDTO> getProntuarioById(@PathVariable UUID id) {', 'public ResponseEntity<ProntuarioDTO> getProntuarioById(@PathVariable UUID id, java.security.Principal principal) {')
content = content.replace('return ResponseEntity.ok(prontuarioService.findById(id));', 'return ResponseEntity.ok(prontuarioService.findById(id, principal.getName()));')


with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed")
