import os
import re

filepath = 'backend/src/test/java/com/medflow/service/ConsultaServiceTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace RabbitTemplate with ApplicationEventPublisher
content = content.replace('import org.springframework.amqp.rabbit.core.RabbitTemplate;', 'import org.springframework.context.ApplicationEventPublisher;\nimport com.medflow.event.ConsultaEvento;')
content = content.replace('@Mock\n    private RabbitTemplate rabbitTemplate;', '@Mock\n    private ApplicationEventPublisher applicationEventPublisher;')
content = content.replace('@Mock\n    private AuditoriaService auditoriaService;', '')

# We also need to remove auditoriaService.registrarAcao verification and change rabbitTemplate verification
content = re.sub(r'verify\(rabbitTemplate\)\.convertAndSend\(anyString\(\),\s*anyString\(\)\);', 'verify(applicationEventPublisher).publishEvent(any(ConsultaEvento.class));', content)
content = re.sub(r'verify\(auditoriaService\)\.registrarAcao\(anyString\(\),\s*anyString\(\),\s*any\(\),\s*anyString\(\)\);', '', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaServiceTest fixed!")
