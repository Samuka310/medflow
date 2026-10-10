import os
import re

# PacienteService
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# create: remove if (auth == null) block
content = re.sub(r'\s*if \(auth == null\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*', '\n        ', content)

# getResumo: remove if (auth != null) block completely
content = re.sub(r'\s*if \(auth != null\) \{\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = emailLogado;\s*if \(isPaciente && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\(".*?"\);\s*\}\s*\}\s*', '\n        ', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed!")
