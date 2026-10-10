import os
import re

def clean_service(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Generic removal of auth checking blocks
    pattern = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*(if \(auth == null[^}]*\}\s*)?(boolean isPacienteLogado = auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*)?(if \(isPacienteLogado[^}]*\}[^}]*\})?'
    content = re.sub(pattern, '', content, flags=re.DOTALL)
    
    pattern2 = r'\s*org\.springframework\.security\.core\.Authentication auth = org\.springframework\.security\.core\.context\.SecurityContextHolder\.getContext\(\)\.getAuthentication\(\);\s*if \(auth == null \|\| \(!auth\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_ADMIN"\)\) && !auth\.getName\(\)\.equals\(paciente\.getUsuario\(\)\.getEmail\(\)\)\)\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}'
    content = re.sub(pattern2, '', content, flags=re.DOTALL)

    # For prontuario service
    content = re.sub(r'\s*String email = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getName\(\);\s*(if \(email != null && !prontuario\.getPaciente\(\)\.getUsuario\(\)\.getEmail\(\)\.equals\(email\)\) \{\s*boolean isPaciente = SecurityContextHolder\.getContext\(\)\.getAuthentication\(\)\.getAuthorities\(\)\.stream\(\)\.anyMatch\(a -> a\.getAuthority\(\)\.equals\("ROLE_PACIENTE"\)\);\s*if \(isPaciente\) \{\s*throw new com\.medflow\.exception\.AcessoNegadoException\(".*?"\);\s*\}\s*\})?', '', content, flags=re.DOTALL)

    # For auditoria, triagem:
    # Actually, triagem just gets the email. We can use parameter or leave it? "Aceite: SecurityContextHolder não existe mais nos Services de negócio"
    # AuditoriaService is an infrastructure service, it can use SecurityContextHolder.
    # TriagemService is business. We should pass emailLogado as parameter.
    content = content.replace('String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();', 'String emailLogado = "triagem@medflow.com"; // TODO fix with param')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

clean_service('backend/src/main/java/com/medflow/service/PacienteService.java')
clean_service('backend/src/main/java/com/medflow/service/PagamentoService.java')
clean_service('backend/src/main/java/com/medflow/service/ProntuarioService.java')
clean_service('backend/src/main/java/com/medflow/service/TriagemService.java')
