import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'backend/src/main/java/com/medflow/service/'

# ConsultaService
replace_in_file(base_dir + 'ConsultaService.java', [
    ('!"AGENDADA".equals(consulta.getStatus())', 'consulta.getStatus() != com.medflow.entity.enums.ConsultaStatus.AGENDADA'),
    ('"CANCELADA".equals(consulta.getStatus()) || "REALIZADA".equals(consulta.getStatus())', 'consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.CANCELADA || consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.REALIZADA'),
    ('"CANCELADA".equals(consulta.getStatus())', 'consulta.getStatus() == com.medflow.entity.enums.ConsultaStatus.CANCELADA'),
    ('!"PAGO".equals(pagamento.getStatus())', 'pagamento.getStatus() != com.medflow.entity.enums.PagamentoStatus.PAGO'),
    ('consulta.setStatus("CONFIRMADA");', 'consulta.confirmar();'),
    ('consulta.setStatus("AGUARDANDO_TRIAGEM");', 'consulta.aguardarTriagem();'),
    ('consulta.setStatus("REALIZADA");', 'consulta.realizar();'),
    ('consulta.setStatus("CANCELADA");', 'consulta.cancelar();'),
    ('consulta.setStatus("AGENDADA");', 'consulta.setStatus(com.medflow.entity.enums.ConsultaStatus.AGENDADA);'),
    ('pagamento.setStatus("PENDENTE");', 'pagamento.setStatus(com.medflow.entity.enums.PagamentoStatus.PENDENTE);')
])

# FaturamentoService
replace_in_file(base_dir + 'FaturamentoService.java', [
    ('"PAGO".equals(p.getStatus())', 'p.getStatus() == com.medflow.entity.enums.PagamentoStatus.PAGO'),
    ('"PENDENTE".equals(p.getStatus())', 'p.getStatus() == com.medflow.entity.enums.PagamentoStatus.PENDENTE')
])

# PagamentoService
replace_in_file(base_dir + 'PagamentoService.java', [
    ('!"PENDENTE".equals(pagamento.getStatus())', 'pagamento.getStatus() != com.medflow.entity.enums.PagamentoStatus.PENDENTE'),
    ('!"PAGO".equals(pagamento.getStatus())', 'pagamento.getStatus() != com.medflow.entity.enums.PagamentoStatus.PAGO'),
    ('pagamento.setStatus("PAGO");', 'pagamento.setStatus(com.medflow.entity.enums.PagamentoStatus.PAGO);')
])

# MedicoService, PacienteService, ProntuarioService (Roles)
roles_replacements = [
    ('!"MEDICO".equalsIgnoreCase(usuario.getRole())', 'usuario.getRole() != com.medflow.entity.enums.Role.MEDICO'),
    ('!"PACIENTE".equalsIgnoreCase(usuario.getRole())', 'usuario.getRole() != com.medflow.entity.enums.Role.PACIENTE'),
    ('"PACIENTE".equalsIgnoreCase(usuario.getRole())', 'usuario.getRole() == com.medflow.entity.enums.Role.PACIENTE'),
    ('"MEDICO".equalsIgnoreCase(usuario.getRole())', 'usuario.getRole() == com.medflow.entity.enums.Role.MEDICO'),
    ('usuario.setRole("PACIENTE");', 'usuario.setRole(com.medflow.entity.enums.Role.PACIENTE);'),
    ('usuario.setRole("MEDICO");', 'usuario.setRole(com.medflow.entity.enums.Role.MEDICO);'),
    ('usuario.setRole("RECEPCIONISTA");', 'usuario.setRole(com.medflow.entity.enums.Role.RECEPCIONISTA);'),
    ('usuario.setRole("TRIAGEM");', 'usuario.setRole(com.medflow.entity.enums.Role.TRIAGEM);'),
    ('usuario.setRole("FATURAMENTO");', 'usuario.setRole(com.medflow.entity.enums.Role.FATURAMENTO);')
]
replace_in_file(base_dir + 'MedicoService.java', roles_replacements)
replace_in_file(base_dir + 'PacienteService.java', roles_replacements)
replace_in_file(base_dir + 'ProntuarioService.java', roles_replacements)
replace_in_file(base_dir + 'AuthService.java', roles_replacements)

# AdminService
replace_in_file(base_dir + 'AdminService.java', [
    ('!role.equals("RECEPCIONISTA") && !role.equals("TRIAGEM") && !role.equals("FATURAMENTO")', 'com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.RECEPCIONISTA && com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.TRIAGEM && com.medflow.entity.enums.Role.valueOf(role) != com.medflow.entity.enums.Role.FATURAMENTO'),
    ('usuario.setRole(role);', 'usuario.setRole(com.medflow.entity.enums.Role.valueOf(role));')
])

print("Substituições realizadas com sucesso.")
