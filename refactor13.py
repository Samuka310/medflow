import os
import re

filepath = 'backend/src/main/java/com/medflow/service/ConsultaService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the agendar extra brace
content = re.sub(r'\.orElseThrow\(\(\) -> new RuntimeException\("Paciente n[^"]*o encontrado\."\)\);\s*\}\s*LocalDateTime inicio = dto\.getDataHora\(\)', 
r'.orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Paciente não encontrado."));\n\n        LocalDateTime inicio = dto.getDataHora()', content)

# Fix Medico não encontrado to RecursoNaoEncontradoException
content = re.sub(r'\.orElseThrow\(\(\) -> new RuntimeException\("M[^"]*dico n[^"]*o encontrado\."\)\);',
r'.orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Médico não encontrado."));', content)


with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaService: corrigido a chave no agendar.")
