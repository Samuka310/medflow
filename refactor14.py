import os
import re

# 1. ConsultaService
filepath = 'backend/src/main/java/com/medflow/service/ConsultaService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('private final ApplicationEventPublisher eventPublisher;', 'private final ApplicationEventPublisher applicationEventPublisher;')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 2. PacienteService
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
# Completely remove auth logic in PacienteService
content = re.sub(r'if \(auth == null[^}]*\}\s*boolean isPacienteLogado[^}]*\}[^}]*\}', '', content, flags=re.DOTALL)
content = re.sub(r'if \(auth != null[^}]*\}\s*boolean isPacienteLogado[^}]*\}[^}]*\}', '', content, flags=re.DOTALL)
# also for other occurrences of auth in PacienteService
content = re.sub(r'if \(auth == null \|\| \(!auth\.getAuthorities\(\)[^}]*\}[^}]*\}', '', content, flags=re.DOTALL)
content = re.sub(r'boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'if \(\!auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\) && \!auth\.getName\(\)\.equals\(paciente\.getUsuario\(\)\.getEmail\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}', '', content, flags=re.DOTALL)
# just remove all lines with uth.
content = '\n'.join([line for line in content.split('\n') if 'auth.' not in line])
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 3. PagamentoService
filepath = 'backend/src/main/java/com/medflow/service/PagamentoService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = '\n'.join([line for line in content.split('\n') if 'auth.' not in line])
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# 4. ProntuarioService
filepath = 'backend/src/main/java/com/medflow/service/ProntuarioService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = '\n'.join([line for line in content.split('\n') if 'email' not in line])
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Corrigidos!")
