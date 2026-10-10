import os
import re

filepath = 'backend/src/main/java/com/medflow/service/ConsultaService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix confirmar
content = re.sub(r'public void confirmar\(UUID id\) \{\s*Consulta consulta = consultaRepository\.findById\(id\)\s*\.orElseThrow\(\(\) -> new RuntimeException\("Consulta n[^"]*o encontrada\."\)\);\s*\}\s*if \(consulta\.getStatus\(\) != com\.medflow\.entity\.enums\.ConsultaStatus\.AGENDADA\) \{', 'public void confirmar(UUID id) {\n        Consulta consulta = consultaRepository.findById(id)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Consulta não encontrada."));\n\n        if (consulta.getStatus() != com.medflow.entity.enums.ConsultaStatus.AGENDADA) {', content)

# Fix cancelar
content = re.sub(r'public void cancelar\(UUID id\) \{\s*Consulta consulta = consultaRepository\.findById\(id\)\s*\.orElseThrow\(\(\) -> new RuntimeException\("Consulta n[^"]*o encontrada\."\)\);\s*\}\s*if \(consulta\.getDataHora\(\)\.isBefore', 'public void cancelar(UUID id) {\n        Consulta consulta = consultaRepository.findById(id)\n                .orElseThrow(() -> new com.medflow.exception.RecursoNaoEncontradoException("Consulta não encontrada."));\n\n        if (consulta.getDataHora().isBefore', content)

# I notice there was another one around agendar ?
# Let's check if (isPacienteLogado again to see if any } were left behind
content = re.sub(r'\}\s*\}\s*LocalDateTime inicio = novaDataHora\.minusMinutes\(59\);', r'}\n\n        LocalDateTime inicio = novaDataHora.minusMinutes(59);', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaService chaves extra corrigidas.")
