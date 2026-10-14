# Changelog

### Fase 7 - Microsserviços Reais com Spring Cloud
- **Estrutura Multi-Module Maven**:
  - `medflow-aggregator` (POM raiz) orquestrando `libs/medflow-events`, `services/notification-service`, `services/api-gateway` e `backend` (`medflow-backend`).
  - Biblioteca comum compartilhada `libs/medflow-events` contendo o contrato imutável `ConsultaEvento`.
- **Spring Cloud API Gateway (`services/api-gateway`)**:
  - Gateway de borda na porta 8080 roteando dinamicamente `/api/notificacoes/**` para `notification-service:8082` e `/api/**` para `backend:8081`.
  - `CorrelationIdFilter`: injeção e propagação de cabeçalho `X-Request-Id` para rastreabilidade distribuída.
  - `JwtAuthGlobalFilter`: validação JWT edge com tratamento de tokens ausentes/inválidos e resposta HTTP 401 padronizada.
  - Centralização de políticas de CORS para o frontend (portas 5173/3000 e domínio Vercel).
- **Microsserviço de Notificações Autônomo (`services/notification-service`)**:
  - Aplicação independente consumindo assincronamente a fila `fila.notificacoes` do RabbitMQ.
  - Persistência e auditoria de disparos de notificação na entidade `NotificacaoHistorico`.
  - Endpoint REST paginado `GET /api/notificacoes` para consultas por `consultaId` ou paciente.
- **Comunicação Inter-serviços Resiliente (OpenFeign + Resilience4j)**:
  - Integração síncrona declarativa no `backend` com `NotificationFeignClient`.
  - Fallback defensivo com `NotificationClientFallback` sob indisponibilidade ou timeout do serviço de notificações.
  - Endpoint `GET /api/consultas/{id}/notificacoes` no `ConsultaController`.
- **Orquestração e Observabilidade**:
  - `docker-compose.yml` e manifestos `k8s/` (`api-gateway.yaml`, `notification-service.yaml`, `backend.yaml`) padronizados com `validate` no DDL do Hibernate.
  - Prometheus configurado com jobs para Gateway, Core e Notification Service.
  - Documentação em `docs/ARCHITECTURE.md` e decisão arquitetural em `docs/decisions/ADR-005-Spring-Cloud-Microsservicos.md`.

### Fase 6 - Padrões de Projeto Aplicados e Documentados (OOP)
- **Strategy Pattern**:
  - Implementação da interface `PoliticaCancelamentoStrategy` e das estratégias concretas `ReembolsoIntegralStrategy` (> 24h: 100%), `ReembolsoParcialTaxaStrategy` (2h a 24h: 80%) e `SemReembolsoBloqueadoStrategy` (< 2h: bloqueado).
  - Contexto `PoliticaCancelamentoContext` integrado ao `ConsultaService.cancelar(id)`.
- **Factory Method Pattern**:
  - Implementação de `NotificadorFactory` e dos canais `EmailNotificador`, `SmsNotificador` e `WhatsAppNotificador` via interface `NotificadorCanal`.
  - Desacoplamento do `NotificacaoListener`, permitindo múltiplos canais assíncronos.
- **Template Method Pattern**:
  - Criação da classe abstrata `AbstractCsvImportador<T>` com método template final para esqueleto de parsing em streaming via I/O e ganchos protegidos para validação e persistência.
  - Implementação de `MedicoCsvImportador` e `PacienteCsvImportador`.
- **Builder Pattern**:
  - Criação de `FiltroConsulta.builder()` com encadeamento fluente de métodos e validação de invariantes de período.
- **Documentação e Testes**:
  - Criação do documento completo `docs/DESIGN_PATTERNS.md` cobrindo padrões GoF, trechos de código, trade-offs e correlação com cada princípio SOLID.
  - Registro de decisão em `docs/decisions/ADR-004-Padroes-de-Projeto-OOP.md`.
  - Testes unitários para todas as estratégias, fábricas, importadores e builders (`PoliticaCancelamentoStrategyTest`, `NotificadorFactoryTest`, `FiltroConsultaTest`, `ImportacaoCsvServiceTest`). Total de 31 testes unitários/integrados passando no backend.

### Fase 5 - I/O e Integrações de Arquivo (Java I/O)
- Importação em lote via CSV com streaming (`BufferedReader` / `StandardCharsets.UTF_8`) e relatório de erros por linha.
- Exportação de relatórios em streaming CSV com `StreamingResponseBody` e BOM UTF-8 para Excel.
- Geração de PDFs de Receitas e Atestados médicos com OpenPDF 2.0.3.
- Amostras em `docs/samples/`, `docs/JAVA_CORE.md` e `ADR-003`.

### Fase 4 - Concorrência e Integridade do Agendamento
- Alinhamento da regra de negócio com o banco de dados (`ConsultaProperties` e índice único parcial `uk_medico_data_hora_ativa`).
- Bloqueio pessimista (`Pessimistic Write Lock`) no médico contra *race conditions*.
- Tratamento de `DataIntegrityViolationException` no `GlobalExceptionHandler` convertendo em `409 Conflict`.
- Idempotência em 2 níveis no consumo de mensagens assíncronas no `NotificacaoListener`.
- Teste multithread com `ExecutorService` (`ConsultaConcurrencyTest`) e registro do `ADR-002`.

### Fase 3 - Camada de Serviço Limpa (Streams, Lambdas, Optional, Collections)
- Extração de autorizações para `AcessoSecurity` e anotações `@PreAuthorize`.
- Desacoplamento da mensageria via Eventos de Domínio (`ConsultaEvento`) e `@TransactionalEventListener(phase = AFTER_COMMIT)`.

### Fase 2 - Exceções, Validação e Contrato de Erro (RFC 7807)
- Hierarquia de exceções de domínio selada (`MedflowException` -> 404, 409, 422, 403).
- Respostas de erro no formato RFC 7807 `ProblemDetail`.

### Fase 1 - Modelo de Domínio Forte (OOP + Generics + Enums)
- Remoção de Magic Strings e criação de enums com máquina de estados.
- Value objects e tipos genéricos.

### Fase 0 - Higiene do Repositório
- Limpeza da raiz, configuração de `.gitignore`, criação de `docs/` e `LICENSE`.
