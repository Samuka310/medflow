import os
import re

def replace_in_file(filepath, replacements, regex_replacements=None):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    if regex_replacements:
        for pattern, new in regex_replacements:
            content = re.sub(pattern, new, content, flags=re.DOTALL)
            
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# ConsultaService
replace_in_file('backend/src/main/java/com/medflow/service/ConsultaService.java', [], [
    (r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{\s*boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && ![^}]*\}', ''),
    (r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{\s*boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && !consulta\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\([^}]*\);\s*\}\s*\}', ''),
    (r'public List<ConsultaDTO> agendaHoje\(\) \{.*String email = auth\.getName\(\);', 'public List<ConsultaDTO> agendaHoje(String email) {')
])

# Remove leftover auth blocks if any
replace_in_file('backend/src/main/java/com/medflow/service/ConsultaService.java', [], [
    (r'org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth != null\) \{[^}]*if \(isPacienteLogado[^}]*\}[^}]*\}', '')
])

# PacienteService
replace_in_file('backend/src/main/java/com/medflow/service/PacienteService.java', [], [
    (r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth == null[^}]*\}\s*boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && !paciente\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\("[^"]+"\);\s*\}', ''),
    (r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth == null \|\| \(!auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\) && !auth\.getName\(\)\.equals\(paciente\.getUsuario\(\)\.getEmail\(\)\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}', ''),
])

# PagamentoService
replace_in_file('backend/src/main/java/com/medflow/service/PagamentoService.java', [], [
    (r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPacienteLogado && !pagamento\.getConsulta\(\)\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(auth\.getName\(\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}', '')
])

# ProntuarioService
replace_in_file('backend/src/main/java/com/medflow/service/ProntuarioService.java', [
    ('import org.springframework.security.core.context.SecurityContextHolder;', '')
], [
    (r'\s*String email = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getName\(\);\s*if \(email != null && !prontuario\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*boolean isPaciente = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPaciente\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*\}', '')
])

print("Remoção de SecurityContextHolder dos Services executada.")
