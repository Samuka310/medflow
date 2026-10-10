import os
import re

filepath = 'backend/src/main/java/com/medflow/service/ProntuarioService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. findById(UUID id)
auth_pront_find = r'\s*String email = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getName\(\);\s*if \(email != null && !prontuario\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*boolean isPaciente = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPaciente\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*\}\s*'
content = re.sub(auth_pront_find, '\n        ', content)

# 2. adicionarEvolucao
content = content.replace('public ProntuarioDTO adicionarEvolucao(UUID id, EvolucaoDTO dto) {', 'public ProntuarioDTO adicionarEvolucao(UUID id, EvolucaoDTO dto, String medicoEmail) {')
auth_pront_evol = r'\s*String email = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getName\(\);\s*Medico medico = medicoRepository\.findByUsuarioEmail\(email\)\s*\.orElseThrow\(\(\) -> new com\.medflow\.exception\.RecursoNaoEncontradoException\("M.*dico n.*o encontrado"\)\);\s*'
content = re.sub(auth_pront_evol, '\n        Medico medico = medicoRepository.findByUsuarioEmail(medicoEmail).orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Médico não encontrado."));\n        ', content)

content = content.replace('import org.springframework.security.core.context.SecurityContextHolder;', '')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ProntuarioService fixed!")
