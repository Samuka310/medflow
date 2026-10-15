# Changelog

### Fase 12 - Deploy em Produção, Orquestração Docker Compose, Kubernetes & Finalização do Roadmap
- **Orquestração de Produção com Docker Compose**:
  - Enriquecimento do `docker-compose.yml` com *healthchecks* robustos utilizando probes HTTP do Spring Boot Actuator (`/actuator/health`) para `backend`, `notification-service` e `api-gateway`.
  - Encadeamento determinístico de inicialização (`condition: service_healthy`) garantindo que o gateway só inicie após os microsserviços estarem prontos, e o frontend só após o gateway estar operacional.
  - Provisionamento automatizado do Grafana sem necessidade de login ou configuração manual de datasource/dashboards.
- **Infraestrutura Completa e Declarativa para Kubernetes (`k8s/`)**:
  - **`k8s/secret.yaml`**: Segredos desacoplados para credenciais do PostgreSQL, RabbitMQ e chave criptográfica JWT HMAC-SHA256.
  - **`k8s/configmap.yaml`**: ConfigMap central com URLs de descoberta e perfis de produção.
  - **`k8s/postgres.yaml`**: `StatefulSet` com armazenamento persistente via `PersistentVolumeClaim` (5Gi), probes e ClusterIP.
  - **`k8s/rabbitmq.yaml`**: Broker AMQP com probes TCP socket e interface web de gestão.
  - **`k8s/backend.yaml`**: `Deployment` de alta disponibilidade (2 réplicas), cotas de recursos (requests: 512Mi/300m, limits: 1536Mi/1500m), probes nativas do Kubernetes (`livenessProbe: /actuator/health/liveness`, `readinessProbe: /actuator/health/readiness`).
  - **`k8s/notification-service.yaml`**: `Deployment` com cotas de recursos e probes de saúde do Actuator.
  - **`k8s/api-gateway.yaml`**: Borda reativa com Rate Limiting e probes de saúde.
  - **`k8s/frontend.yaml`**: Servidor Web Nginx com 2 réplicas, probes HTTP e cotas de recursos.
  - **`k8s/ingress.yaml`**: Ingress NGINX para roteamento unificado de tráfego externo (`/api/**` para o Gateway e `/` para a SPA).
  - **`k8s/hpa.yaml`**: `HorizontalPodAutoscaler` elástico para auto-scaling de 2 a 10 pods sob 70% de utilização de CPU.
  - **`k8s/kustomization.yaml`**: Deploy declarativo com 1 comando via Kustomize (`kubectl apply -k k8s/`).
- **Automação de CI/CD & Cobertura JaCoCo**:
  - Vinculação do relatório JaCoCo diretamente à fase `test` do Maven em `backend/pom.xml`.
  - Atualização do workflow `.github/workflows/ci.yml` para compilação unificada do reactor multi-módulo com `./mvnw clean test`.
  - Atualização do `README.md` com guia completo de deploy em Kubernetes e tabela de portas.
- **Segurança de Borda & Rate Limiting no Spring Cloud Gateway (`RateLimiterGlobalFilter.java`)**:
  - Implementação de filtro reativo global de borda (`GlobalFilter`, `Ordered.HIGHEST_PRECEDENCE + 1`), executando logo após a injeção do Correlation ID e antes de qualquer downstream/auth.
  - Proteção por IP em janela deslizante de 60 segundos com limites granulares:
    - **15 req/min** para endpoints `/auth/**` (proteção rigorosa contra força bruta e credential stuffing).
    - **120 req/min** para rotas gerais `/api/**` (proteção contra negação de serviço e sobrecarga de CPU/banco).
  - Respostas no padrão RFC 7807 (`application/problem+json`) com status HTTP 429 Too Many Requests e cabeçalhos `Retry-After: 60`, `X-RateLimit-Limit` e `X-RateLimit-Remaining`.
  - Suíte de testes unitários reativos `RateLimiterGlobalFilterTest.java` (3 testes cobrindo rotas permitidas, bypass de preflight/actuator e bloqueio com 429).
- **Métricas Customizadas de Negócio com Micrometer (`MedflowMetricsService.java`)**:
  - Criação de serviço central de telemetria integrado ao `MeterRegistry` do Micrometer e exposto via `/actuator/prometheus`:
    - `medflow.consultas.agendadas`: Total de agendamentos realizados com sucesso.
    - `medflow.consultas.confirmadas`: Consultas confirmadas por paciente ou recepção.
    - `medflow.consultas.canceladas`: Cancelamentos com tag dimensional por motivo (`motivo`).
    - `medflow.consultas.realizadas`: Atendimentos médicos concluídos.
    - `medflow.triagens.realizadas`: Triagens com tag por prioridade Manchester (`prioridade`).
    - `medflow.pagamentos.processados`: Transações com tags por método (`forma`) e status (`status`).
    - `medflow.consultas.agendamento.duracao`: Timer de latência com cálculo de percentis p50, p95 e p99.
  - Instrumentação direta nos métodos de negócio de `ConsultaService.java`.
  - Suíte de testes unitários com `SimpleMeterRegistry` em `MedflowMetricsServiceTest.java` (4 testes).
- **Health Checks Avançados & Probes de Liveness/Readiness**:
  - Criação de `MedflowSystemHealthIndicator.java` validando integridade do PostgreSQL e medições de memória heap da JVM.
  - Testes unitários em `MedflowSystemHealthIndicatorTest.java` (2 testes cobrindo UP e DOWN).
  - Habilitação de probes nativas do Kubernetes (`/actuator/health/liveness`, `/actuator/health/readiness`) no `backend`, `api-gateway` e `notification-service`.
- **Provisionamento e Dashboard Automatizado no Grafana**:
  - Configuração de provisionamento do datasource Prometheus (`grafana/provisioning/datasources/prometheus.yml`).
  - Provider de dashboards declarativos (`grafana/provisioning/dashboards/dashboards.yml`).
  - Dashboard completo (`grafana/dashboards/medflow-overview.json`) com painéis para KPIs clínicos de negócio, gráfico em rosca de triagem Manchester, tempos médios de resposta, throughput por status HTTP, disparos de Rate Limiter (429) e gráficos de consumo de CPU/Heap.
  - Atualização do `docker-compose.yml` para montagem automática dos volumes de provisionamento no container do Grafana.
- **Métricas de Qualidade**:
  - Suíte expandida para **112 testes automatizados** (107 no backend + 5 no gateway), com 100% de aprovação.
  - **71.27% de cobertura de código por instruções no JaCoCo** (4.018 instruções cobertas).

### Fase 10 - Testes Automatizados Avançados, Cobertura JaCoCo & CI/CD
- **Cobertura de Código com JaCoCo (`jacoco-maven-plugin:0.8.12`)**:
  - Configuração do plugin JaCoCo no `backend/pom.xml` para medição de cobertura de instruções e branches durante as fases de teste e verificação.
  - Exclusão de classes não testáveis por unidade (DTOs, Entidades JPA puras, Configurações de infraestrutura e classe principal do Spring Boot).
  - Alcance de **70.26% de cobertura de código por instruções** (3.813 instruções cobertas).
- **Expansão Massiva da Suíte de Testes Automatizados (de 32 para 101 testes)**:
  - Criação de suítes de testes unitários com Mockito para serviços críticos:
    - `PacienteServiceTest` (9 testes): criação, regras de negócio de CPF duplicado, consulta `findMe`, prescrição de receitas e emissão de atestados médicos.
    - `MedicoServiceTest` (8 testes): cadastro, validação de CRM duplicado e especialidades inexistentes, listagem e filtro por especialidade.
    - `TriagemServiceTest` (4 testes): registro pelo Protocolo de Manchester, verificação de vínculo com paciente e idempotência.
    - `PagamentoServiceTest` (5 testes): liquidação de pagamentos pendentes, estornos, auditoria e bloqueio de transições inválidas.
    - `AdminServiceTest` (3 testes): criação de colaboradores por perfil (RECEPCIONISTA, TRIAGEM, FATURAMENTO) e validação de email.
    - `ProntuarioServiceTest` (3 testes): salvamento clínico, auditoria e bloqueio de acesso a dados de outros pacientes (`AcessoNegadoException`).
    - `EspecialidadeServiceTest` (6 testes): CRUD completo e validações de unicidade por nome.
    - `ExameServiceTest` (2 testes): upload e listagem vinculada a prontuário.
    - `FaturamentoServiceTest` (1 teste): consolidação de métricas e KPIs financeiros.
    - `AuditoriaServiceTest` (3 testes): auditoria automática com e sem contexto de autenticação e paginação.
    - `AcessoSecurityTest` (4 testes): validação de regras de segurança fina em nível de método (`@acessoSecurity`).
    - `JwtServiceTest` (3 testes): geração de tokens com claims, extração de username e validação de expiração.
    - Expansão de `ConsultaServiceTest` (10 testes): agendamento com validação de antecedência mínima, check-in, confirmação, cancelamento via GoF Strategy, realização condicionada a pagamento e ordenação da fila médica pela gravidade de Manchester.
  - Criação de testes de endpoint com MockMvc e `@WebMvcTest` standalone:
    - `ConsultaControllerTest` (4 testes): rotas POST, GET, PUT confirmar e cancelar.
    - `FaturamentoControllerTest` (1 teste): consolidação financeira e relatório.
    - `TriagemControllerTest` (2 testes): registro de triagem e consulta por ID.
    - `PacienteControllerTest` (3 testes): listagem, resumo cadastral e consulta de perfil próprio (`/me`).
- **Pipeline de Integração Contínua (CI/CD com GitHub Actions)**:
  - Criação do wrapper Maven raiz (`mvnw` e `mvnw.cmd`) para permitir execução unificada do reactor multi-módulo na raiz do repositório.
  - Atualização de `.github/workflows/ci.yml`:
    - Disparos automáticos em pushes para `main`, `master` e branches de feature (`feat/**`), além de pull requests.
    - Job `backend-test`: compilação Java 21, execução de todos os testes do reactor com `./mvnw clean test jacoco:report`, e upload automático do relatório HTML do JaCoCo como artefato de build.
    - Job `frontend-test`: instalação de dependências Node 20, type-checking com TypeScript (`tsc -b`) e compilação de produção com Vite.
    - Job `docker-build`: verificação de integridade dos builds do Docker Compose.
- **Documentação & Badges**:
  - Atualização do `README.md` com badges de status de CI/CD, JaCoCo Coverage (70.2%), contagem de testes (101 passing) e instruções para geração de relatórios locais.

### Fase 9 - Frontend Completo e Polido (React 19, TypeScript, Tailwind, RBAC)
- **Implementação Integral de Telas por Papel (RBAC)**:
  - `AgendaMedico.tsx` (`/agenda`): Fila de atendimento médico com ordenação por gravidade do Protocolo de Manchester (EMERGÊNCIA, MUITO URGENTE, URGENTE, POUCO URGENTE, NÃO URGENTE), chamada de paciente e ação direta para início de atendimento.
  - `Triagem.tsx` (`/triagem`): Painel de enfermagem com aferição de sinais vitais (PA, FC, Temperatura), descrição de queixas e classificação em 5 níveis com badges e cores padronizadas de Manchester.
  - `Faturamento.tsx` (`/faturamento`): Painel financeiro de gestão de caixa, liquidação de consultas, estornos e download em streaming de arquivo CSV para conciliação contábil.
  - `ProntuarioView.tsx` (`/prontuario/:consultaId`): Atendimento clínico com registro de evolução médica, prescrição de receitas e emissão de atestados médicos com download instantâneo de PDFs gerados pelo backend.
  - `Recepcao.tsx` (`/recepcao`): Painel de recepção para check-in presencial, agendamento imediato, cancelamento e reagendamento de consultas.
  - `Dashboard.tsx` (`/admin`): Painel administrativo com métricas gerenciais, cadastro de colaboradores por cargo, histórico de auditoria de acessos e importador em lote CSV com diagnóstico de erros linha a linha.
- **Navegação, Roteamento e Feedback Visual**:
  - `Layout.tsx`: Barra de navegação adaptativa para todos os 6 perfis (`ADMIN`, `MEDICO`, `RECEPCIONISTA`, `TRIAGEM`, `FATURAMENTO`, `PACIENTE`), menu hambúrguer responsivo mobile, badge colorido por papel e Toaster global do Sonner.
  - `App.tsx`: Centralização de rotas com `ProtectedRoute` e criação do `HomePortal` com cartões de atalho contextuais por papel de usuário.
  - `Login.tsx`: Botões de acesso rápido com 1 clique para demonstração de portfólio de todos os 6 papéis com credenciais pré-carregadas.
  - `MinhasConsultas.tsx` & `Medicos.tsx`: Integração com `GET /api/pacientes/me`, skeletons animados de carregamento, estados vazios ilustrados e links diretos para visualização de documentos.
- **Backend & Portfólio**:
  - Adição de `DataInitializer` populando especialidades, médicos, funcionários, pacientes e usuário administrador para testes imediatos sem dependência externa.
  - Criação do documento executivo de portfólio `docs/CURRICULO_PROJECT_DESCRIPTION.md` com bullet points para currículo/LinkedIn e guia de perguntas para entrevistas técnicas.

### Fase 8 - Documentação Técnica da API (OpenAPI, Swagger, Postman e docs/API.md)
- **Padronização OpenAPI / Swagger**:
  - Enriquecimento de `OpenApiConfig` com múltiplos servidores (Gateway 8080 e Core 8081), esquema de segurança `bearerAuth` detalhado e licença MIT.
  - Documentação OpenAPI exaustiva em todos os 12 controllers do sistema (`@Tag`, `@Operation`, `@ApiResponse` com schemas tipados para 200, 201, 204, 400, 401, 403, 404, 409 e 422 com `ProblemDetail`).
- **Postman Collection & Environment (`docs/postman/`)**:
  - Criação de `MedFlow.postman_collection.json` com 10 pastas temáticas cobrindo todos os fluxos e endpoints do sistema.
  - Criação de `MedFlow.postman_environment.json` com variáveis de ambiente (`baseUrl`, `coreUrl`, `token`, IDs) e scripts de teste para captura automática do token JWT no login.
- **Manual Completo da API (`docs/API.md`)**:
  - Matriz de todos os endpoints agrupados por módulo, com método, path, descrição e roles necessárias.
  - Exemplos completos de requisições e respostas de sucesso e formato de erro RFC 7807 (`ProblemDetail`).
  - Diagrama Mermaid de sequência ilustrando o ciclo de vida clínico ponta a ponta (Login → Médico → Paciente → Consulta → Pagamento → Check-in → Triagem → Prontuário → Realização → PDF).
  - Guia passo a passo de autenticação e utilização no Swagger UI e Postman.

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
