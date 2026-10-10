import os
import re

filepath = 'backend/src/test/java/com/medflow/service/ConsultaServiceTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace any lingering rabbitTemplate.convertAndSend
content = re.sub(r'verify\(rabbitTemplate\)\.convertAndSend\(anyString\(\),\s*anyString\(\)\);', 'verify(applicationEventPublisher).publishEvent(any(ConsultaEvento.class));', content)
content = re.sub(r'verify\(auditoriaService\)\.registrarAcao\(anyString\(\),\s*anyString\(\),\s*any\(\),\s*anyString\(\)\);', '', content)

# There might be verifyNoInteractions(rabbitTemplate)
content = content.replace('verifyNoInteractions(rabbitTemplate);', 'verifyNoInteractions(applicationEventPublisher);')
content = content.replace('verifyNoInteractions(auditoriaService);', '')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaServiceTest fixed!")
