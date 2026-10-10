import os
import re

# PacienteService clean auth in getResumo and findClinicoById
filepath = 'backend/src/main/java/com/medflow/service/PacienteService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace auth in findClinicoById
content = re.sub(r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{\s*boolean isMedico = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_MEDICO"\)\);\s*boolean isPaciente = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*String email = auth\.getName\(\);\s*if \(isMedico\) \{\s*boolean temConsulta = consultaRepository\.existsByPacienteIdAndMedicoUsuarioEmail\(id, email\);\s*if \(!temConsulta\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\} else if \(isPaciente\) \{\s*if \(!paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*throw new org\.springframework\.security\.access\.AccessDeniedException\("[^"]+"\);\s*\}\s*\}\s*\}', '\n        ', content, flags=re.DOTALL)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed again!")
