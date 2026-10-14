# Changelog

## [Unreleased]
### Fase 4 - Concorrência e Integridade do Agendamento
- **Alinhamento da regra de negócio com o banco de dados**:
  - Implementação da classe de configuração `ConsultaProperties` (`medflow.consulta.duracao-minutos` e `medflow.consulta.valor-padrao`), substituindo durações e valores fixos no código.
  - Criação da migration Flyway `V9__alinhamento_concorrencia_consulta.sql`: remoção da antiga constraint `uk_medico_data_hora` e criação do índice único parcial `uk_medico_data_hora_ativa` no PostgreSQL com cláusula `WHERE (status != 'CANCELADA')`, permitindo reutilização imediata de horários desmarcados.
- **Bloqueio Pessimista (`Pessimistic Write Lock`)**:
  - Adição de `findByIdWithLock(medicoId)` com `@Lock(LockModeType.PESSIMISTIC_WRITE)` em `MedicoRepository` para serializar agendamentos simultâneos para o mesmo médico.
  - Queries customizadas de checagem de sobreposição de intervalo (`existsConflitoMedico` e `existsConflitoPaciente`) considerando a duração configurada da consulta e ignorando consultas canceladas.
- **Transacionalidade e Contrato de Erro**:
  - Anotação explícita de métodos de escrita com `@Transactional` em `ConsultaService` e `PagamentoService`.
  - Tratamento de `DataIntegrityViolationException` no `GlobalExceptionHandler` convertendo colisões em `409 Conflict` (RFC 7807 `ProblemDetail`).
- **Idempotência no Consumo de Mensagens**:
  - Adição de identificador único `eventoId` no contrato `ConsultaEvento`.
  - Implementação de filtro de idempotência em 2 níveis (Cache L1 em memória + persistência L2 em `evento_processado`) no `NotificacaoListener`.
- **Testes e Documentação**:
  - Teste automatizado multithread `ConsultaConcurrencyTest` (10 threads simultâneas disputando o mesmo horário com 1 sucesso e 9 conflitos 409).
  - Teste de idempotência `NotificacaoListenerTest`.
  - Registro de decisão técnica em `docs/decisions/ADR-002-Concorrencia-e-Integridade-Agendamento.md`.
  - Detalhamento arquitetural e diagrama de sequência em `docs/ARCHITECTURE.md`.

### Fase 3 - Camada de Serviço Limpa (Streams, Lambdas, Optional, Collections)
- Extração da regra de autorização 'paciente só acessa o que é seu' para um componente de segurança (`AcessoSecurity`), removendo o acoplamento do `SecurityContextHolder` e transferindo validações para as anotações `@PreAuthorize`.
- Refatoração profunda do `ConsultaService`, removendo as dependências de envio direto de mensagens via `RabbitTemplate`.
- Implementação de comunicação via Eventos de Domínio (`ConsultaEvento`) e publicação via `ApplicationEventPublisher`.
- Criação do ouvinte (`DomainEventListener`) executado após a confirmação da transação do banco (`@TransactionalEventListener(phase = AFTER_COMMIT)`), garantindo que mensagens só sejam publicadas no RabbitMQ se a transação do banco for bem sucedida.
- Ajuste e limpeza de Serviços secundários (`PacienteService`, `ProntuarioService`, `PagamentoService`) para adequar as assinaturas aos novos padrões.

### Fase 2 - Exceções, Validação e Contrato de Erro (RFC 7807)
- Criação da hierarquia de exceções de domínio selada (`MedflowException` -> 404, 409, 422, 403).
- Resposta de erros padronizada via RFC 7807 `ProblemDetail`.

### Fase 1 - Modelo de Domínio Forte (OOP + Generics + Enums)
- Substituição de strings mágicas por enums ricos com máquina de estados (`ConsultaStatus`, `PagamentoStatus`, etc.).
- Value objects e paginação genérica.

### Fase 0 - Higiene do Repositório
- Limpeza da raiz, configuração de `.gitignore`, criação de `docs/` e `LICENSE`.
