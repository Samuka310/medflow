import os
import re

filepath = 'backend/src/main/java/com/medflow/service/ConsultaService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import org.springframework.amqp.rabbit.core.RabbitTemplate;', 'import org.springframework.context.ApplicationEventPublisher;\nimport com.medflow.event.ConsultaEvento;\nimport java.time.Instant;')

content = content.replace('private final RabbitTemplate rabbitTemplate;', 'private final ApplicationEventPublisher eventPublisher;')

# Find all blocks of rabbit + auditoria and replace with event publish
# e.g.:
# String msg = "Consulta agendada: " + consulta.getId();
# rabbitTemplate.convertAndSend(com.medflow.config.RabbitMQConfig.QUEUE_NOTIFICACOES, msg);
# auditoriaService.registrarAcao("EVENTO_CONSULTA", "Consulta", consulta.getId(), "Consulta agendada para " + consulta.getDataHora());

pattern_agendar = r'String msg = "Consulta agendada: " \+ consulta\.getId\(\);\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Consulta agendada para " \+ consulta\.getDataHora\(\)\);'
content = re.sub(pattern_agendar, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Consulta agendada para " + consulta.getDataHora()));', content)

pattern_remarcar = r'String msg = "Consulta remarcada: " \+ consulta\.getId\(\) \+ " para " \+ novaDataHora;\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Consulta remarcada para " \+ novaDataHora\);'
content = re.sub(pattern_remarcar, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Consulta remarcada para " + novaDataHora));', content)

pattern_realizar = r'String msg = "Consulta realizada: " \+ consulta\.getId\(\);\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Consulta realizada com sucesso"\);'
content = re.sub(pattern_realizar, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Consulta realizada com sucesso"));', content)

pattern_confirmar = r'String msg = "Consulta confirmada: " \+ consulta\.getId\(\);\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Consulta confirmada"\);'
content = re.sub(pattern_confirmar, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Consulta confirmada"));', content)

pattern_cancelar = r'String msg = "Consulta cancelada: " \+ consulta\.getId\(\);\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Consulta cancelada pelo paciente/recep\xe7\xe3o"\);'
content = re.sub(pattern_cancelar, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Consulta cancelada"));', content)

pattern_checkin = r'String msg = "Check-in realizado para consulta: " \+ consulta\.getId\(\);\s*rabbitTemplate\.convertAndSend\(com\.medflow\.config\.RabbitMQConfig\.QUEUE_NOTIFICACOES, msg\);\s*auditoriaService\.registrarAcao\("EVENTO_CONSULTA", "Consulta", consulta\.getId\(\), "Check-in realizado"\);'
content = re.sub(pattern_checkin, 'eventPublisher.publishEvent(new ConsultaEvento(consulta.getId(), "EVENTO_CONSULTA", Instant.now(), "Check-in realizado"));', content)


with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("ConsultaService refatorado para usar eventos.")
