import os
import re

filepath = 'backend/src/main/java/com/medflow/service/PagamentoService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

auth_pagamento = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && !pagamento\.getConsulta\(\)\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*'
content = re.sub(auth_pagamento, '\n        ', content)
content = content.replace('import org.springframework.security.core.context.SecurityContextHolder;', '')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("PagamentoService fixed!")
