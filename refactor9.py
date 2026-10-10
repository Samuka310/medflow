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

# ConsultaController
replace_in_file('backend/src/main/java/com/medflow/controller/ConsultaController.java', [], [
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PostMapping', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDoPaciente(#dto.pacienteId, authentication)")\n    @PostMapping'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PutMapping\("/\{id\}/remarcar"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDaConsulta(#id, authentication)")\n    @PutMapping("/{id}/remarcar")'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PutMapping\("/\{id\}/confirmar"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDaConsulta(#id, authentication)")\n    @PutMapping("/{id}/confirmar")'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PutMapping\("/\{id\}/cancelar"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDaConsulta(#id, authentication)")\n    @PutMapping("/{id}/cancelar")'),
    (r'public ResponseEntity<List<ConsultaDTO>> agendaHoje\(\) \{\s*return ResponseEntity\.ok\(consultaService\.agendaHoje\(\)\);', 'public ResponseEntity<List<ConsultaDTO>> agendaHoje(java.security.Principal principal) {\n        return ResponseEntity.ok(consultaService.agendaHoje(principal.getName()));')
])

# PacienteController
replace_in_file('backend/src/main/java/com/medflow/controller/PacienteController.java', [], [
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@GetMapping\("/\{id\}"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDoPaciente(#id, authentication)")\n    @GetMapping("/{id}")'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PutMapping\("/\{id\}"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDoPaciente(#id, authentication)")\n    @PutMapping("/{id}")'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'ADMIN\'\)"\)\s*@DeleteMapping\("/\{id\}"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasRole(\'ADMIN\') or @acessoSecurity.donoDoPaciente(#id, authentication)")\n    @DeleteMapping("/{id}")')
])

# PagamentoController
replace_in_file('backend/src/main/java/com/medflow/controller/PagamentoController.java', [], [
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@GetMapping\("/\{id\}"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDoPagamento(#id, authentication)")\n    @GetMapping("/{id}")'),
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'RECEPCIONISTA\',\'ADMIN\'\)"\)\s*@PutMapping\("/\{id\}/pagar"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'RECEPCIONISTA\',\'ADMIN\') or @acessoSecurity.donoDoPagamento(#id, authentication)")\n    @PutMapping("/{id}/pagar")')
])

# ProntuarioController
replace_in_file('backend/src/main/java/com/medflow/controller/ProntuarioController.java', [], [
    (r'@org\.springframework\.security\.access\.prepost\.PreAuthorize\("hasAnyRole\(\'PACIENTE\',\'MEDICO\',\'ADMIN\'\)"\)\s*@GetMapping\("/\{id\}"\)', '@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole(\'MEDICO\',\'ADMIN\') or @acessoSecurity.donoDoProntuario(#id, authentication)")\n    @GetMapping("/{id}")')
])
