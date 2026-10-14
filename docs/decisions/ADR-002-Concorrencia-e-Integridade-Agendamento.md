# ADR 002: Controle de Concorrência e Integridade no Agendamento de Consultas

## Contexto
O agendamento de consultas médicas em sistemas de saúde é uma operação crítica sujeita a *race conditions* (duas ou mais requisições tentando agendar o mesmo médico ou o mesmo paciente em horários sobrepostos ou idênticos). Anteriormente, o banco continha apenas uma constraint única para `(medico_id, data_hora)` exato, o que não cobria a regra de negócio de duração da consulta (ex.: 60 minutos de intervalo) e impedia o reaproveitamento de horários cancelados. Além disso, sem mecanismos de bloqueio transacional, requisições simultâneas podiam passar pela checagem em memória e colidir.

## Decisão
Adotamos uma estratégia em três camadas defensivas para garantir 100% de integridade e serialização segura:

1. **Bloqueio Pessimista no Médico (`Pessimistic Write Lock`)**:
   - Ao iniciar a transação de agendamento ou remarcação, a entidade `Medico` é carregada com `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`).
   - Isso serializa transações concorrentes para o mesmo médico, eliminando leituras fantasmas durante a validação do intervalo de horário.
   - É uma solução portável entre qualquer banco de dados relacional e não depende de extensões específicas de engine.

2. **Janela Temporal Dinâmica e Configurável**:
   - Criamos `ConsultaProperties` (`medflow.consulta.duracao-minutos`), permitindo configurar o tempo de consulta (padrão 60 minutos).
   - O intervalo é validado via queries otimizadas (`existsConflitoMedico` e `existsConflitoPaciente`), desconsiderando consultas canceladas e o próprio ID na remarcação.

3. **Restrição de Integridade no Banco (Flyway V9)**:
   - Substituição da constraint estrita por um índice único parcial:
     `CREATE UNIQUE INDEX uk_medico_data_hora_ativa ON consulta (medico_id, data_hora) WHERE (status != 'CANCELADA');`
   - O `GlobalExceptionHandler` captura `DataIntegrityViolationException` e a traduz em `409 Conflict` (RFC 7807 `ProblemDetail`), garantindo contrato consistente caso ocorra colisão na borda do banco.

4. **Idempotência no Consumo Assíncrono (RabbitMQ)**:
   - O contrato `ConsultaEvento` passou a carregar um `UUID eventoId` único.
   - O listener de notificações implementa deduplicação em dois níveis (Cache L1 em memória + Tabela persistente `evento_processado` em L2), prevenindo envio duplicado de emails/mensagens em cenários de reentrega.

## Consequências
- **Vantagens**:
  - Eliminação completa de agendamentos duplicados ou conflitantes.
  - Liberação automática de horários após cancelamento.
  - Resiliência contra retentativas de mensageria (idempotência).
  - Teste de concorrência automatizado com múltiplas threads (`ConsultaConcurrencyTest`).
- **Desvantagens**:
  - Pequeno overhead de lock por linha na tabela `medico` durante a transação de agendamento (desprezível para a volumetria de uma clínica e isolado por médico).
