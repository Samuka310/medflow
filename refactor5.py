import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

filepath = 'backend/src/test/java/com/medflow/service/ConsultaServiceTest.java'
replace_in_file(filepath, [
    ('savedConsulta.setStatus("AGENDADA");', 'savedConsulta.setStatus(com.medflow.entity.enums.ConsultaStatus.AGENDADA);'),
    ('assertEquals("AGENDADA", result.getStatus());', 'assertEquals(com.medflow.entity.enums.ConsultaStatus.AGENDADA, result.getStatus());')
])

print("Substituições 5 realizadas.")
