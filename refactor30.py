import os
import re

filepath = 'backend/src/test/java/com/medflow/service/ConsultaServiceTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());', 'verify(applicationEventPublisher, times(1)).publishEvent(any(ConsultaEvento.class));')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaServiceTest fixed again!")
