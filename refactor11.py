import os
import re

filepath = 'backend/src/main/java/com/medflow/service/ConsultaService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace imports
content = content.replace('import org.springframework.amqp.rabbit.core.RabbitTemplate;', 'import org.springframework.context.ApplicationEventPublisher;\nimport com.medflow.event.ConsultaEvento;\nimport java.time.Instant;')
content = content.replace('import com.medflow.config.RabbitMQConfig;', '')
# Replace fields
content = content.replace('private final RabbitTemplate rabbitTemplate;', 'private final ApplicationEventPublisher applicationEventPublisher;')
content = content.replace('private final AuditoriaService auditoriaService;', '')

pattern = r'String mensagem = String\.format\("[^"]+",[^)]+\)[^;]*;\s*rabbitTemplate\.convertAndSend\([^,]+,\s*mensagem\);\s*auditoriaService\.registrarAcao\([^;]+\);'
content = re.sub(pattern, 'applicationEventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Operacao de consulta: " + consulta.getStatus()));', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaService refatorado para usar eventos, regex corrigida.")
