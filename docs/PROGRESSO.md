# Progresso da evolução do MedFlow

## Comandos verificados
- **Build & Testes Multi-Módulo (Backend, Gateway, Notificações, Eventos):**
  ```bash
  ./mvnw clean test
  ```
- **Build do Frontend (TypeScript + Vite):**
  ```bash
  cd frontend && npm run build
  ```
- **Lint do Frontend:**
  ```bash
  cd frontend && npm run lint
  ```
- **Subir ambiente completo (Docker Compose):**
  ```bash
  docker compose up -d --build
  ```
- **Verificação de Saúde (Health Check):**
  * Gateway: `http://localhost:8080/actuator/health`
  * Core Backend: `http://localhost:8081/actuator/health`
  * Notification Service: `http://localhost:8082/actuator/health`
  * Prometheus: `http://localhost:9090/-/healthy`
- **Smoke test manual (Fluxo Principal E2E):**
  1. Login como `paciente@medflow.com` / `paciente123` e agendar consulta com médico.
  2. Login como `recepcao@medflow.com` / `recepcao123` e realizar check-in presencial da consulta.
  3. Login como `triagem@medflow.com` / `triagem123` e registrar sinais vitais com classificação de risco Manchester.
  4. Login como `faturamento@medflow.com` / `faturamento123` e quitar fatura para status `PAGO`.
  5. Login como `medico@medflow.com` / `medico123`, visualizar fila ordenada por prioridade Manchester, preencher evolução clínica e clicar em Realizar Consulta.
  6. Download dos PDFs de receitas e atestados médicos emitidos.

---

## Linha de base (Baseline)
- **Data de Execução:** 15/10/2026 17:08
- **Commit Base:** `6679f22` (feat(deploy): production orchestration with Docker Compose, Kubernetes manifests and Kustomize)
- **Tag Git:** `baseline-antes-evolucao`
- **Resultado dos Testes Backend:**
  * Total de testes executados: **112 testes automatizados** (107 backend + 5 gateway)
  * Falhas: 0 | Erros: 0 | Ignorados/Skipped: 2 (Testcontainers de integração com banco externo)
  * Tempo de execução: 36.880 segundos
  * Status: **VERDE (BUILD SUCCESS)**
  * Cobertura JaCoCo gerada em: `backend/target/site/jacoco/index.html` (71.27% instruções cobertas)
- **Resultado do Frontend:**
  * TypeScript + Vite (`npm run build`): **VERDE (Sucesso em 811ms)**
  * Linter (`oxlint`): 0 erros, 4 avisos não impeditivos.
- **Diagnóstico da Linha de Base:** **100% Estável e Aprovada para Evolução.**

---

## Estado dos épicos

| Épico | Item | Status | Commit/PR | Evidência |
| :---: | :---: | :---: | :---: | :---: |
| **Épico 0** | **0.1** Segredos fora do Git | Concluído e verificado | Commit feat(security): remove hardcoded secrets, add env injection and gitleaks | 112 testes OK, frontend build OK, .env.example, .gitleaks.toml, k8s/external-secret.yaml |
| **Épico 0** | **0.2** JWT RS256/EdDSA com rotação | Concluído e verificado | Commit feat(security): migrate JWT to RS256 with kid, rotation and refresh token | 120 testes OK (5 novos), Flyway V10, RS256 assimétrico, replay detection |
| **Épico 0** | **0.3** Correção vazamento prontuários | Concluído e verificado | Commit feat(security): enforce care relationship and eliminate IDOR in medical records | 124 testes OK (+4 novos), ABAC de vínculo assistencial, bloqueio de IDOR em prontuários e PDFs |
| **Épico 0** | **0.4** Varredura em memória de pacientes | Concluído e verificado | Commit feat(perf): replace in-memory entity scans with indexed queries (item 0.4) | 129 testes OK (+5 novos), Flyway V11, elimina scans em ProntuarioService e FaturamentoService |
| **Épico 0** | **0.5** N+1 na ordenação de agenda médica | Concluído e verificado | Commit feat(perf): eliminate N+1 queries in medical schedule sorting (item 0.5) | 130 testes OK (+2 novos), eliminação de N+1 no sort TimSort e no mapeamento DTO via carregamento em lote |
| **Épico 0** | **0.6** Auditoria estruturada e confiável | Concluído e verificado | Commit feat(audit): structured transactional audit and clinical read tracking (item 0.6) | 134 testes OK (+4 novos), eliminação de System.err, REQUIRES_NEW, métricas Micrometer, eventos de alerta, Flyway V12 |
| **Épico 0** | **0.7** Rate limit distribuído via Redis | Concluído e verificado | Commit feat(security): distributed rate limiting via redis with dual-dimension ip and user throttling (item 0.7) | 10 testes Gateway OK (8 rate limiter), 134 testes Backend OK, Redis no Docker Compose e K8s, RFC 7807 429 |
| **Épico 0** | **0.8** PostgreSQL HA e Pool de Conexões | Concluído e verificado | Commit feat(db): postgresql ha cluster with cloudnativepg, pgbouncer, wal archiving and pitr runbook (item 0.8) | CloudNativePG 3 réplicas, PgBouncer pooler, ScheduledBackup PITR, docs/database/DISASTER_RECOVERY.md, scripts/test-db-restore.sh/.ps1, HikariCP calibrado, 134 testes OK |
| **Épico 0** | **0.9** Logs Centralizados e Mascaramento de PII | Planejado | Branch feat/epico-0-correcoes-imediatas | Aguardando autorização |

### Plano Técnico — Item 0.9: Logs Centralizados (Loki/Promtail) e Mascaramento de PII (LGPD)
- **O que existe hoje:**
  - O backend e os microserviços utilizam logs em texto simples no console sem formatação JSON estruturada para ingestão em stacks centralizadas.
  - Não há stack de agregação centralizada de logs (Grafana Loki + Promtail/Fluent Bit) provisionada no `docker-compose.yml` ou `k8s/`.
  - Dados sensíveis e identificadores pessoais (PII) segundo a LGPD (Art. 5º II e Art. 11), como CPF (`\d{3}\.\d{3}\.\d{3}-\d{2}`), e-mail e termos de anotações clínicas, correm o risco de vazamento em logs de erro ou payloads de requisição.
  - Os logs não injetam formalmente campos estruturados de rastreamento (`traceId`, `spanId`, `correlationId`) no formato JSON do Loki.
- **O que será alterado:**
  - Configurar Logback estruturado em JSON com `logstash-logback-encoder` (`logback-spring.xml`) no backend e api-gateway.
  - Implementar mascarador de logs customizado (`PiiMaskingConverter` / `PiiJsonMasker`) para sanitizar automaticamente:
    * CPF: `***.***.999-**`
    * E-mail: `u***@dominio.com`
    * Dados clínicos protegidos e tokens de autenticação.
  - Provisionar serviço Grafana Loki e coletor Promtail no `docker-compose.yml` e manifestos no Kubernetes (`k8s/loki-promtail.yaml`) com datasource integrado no Grafana.
  - Criar testes unitários para validar que logs gerados com PII são rigorosamente mascarados antes da saída.
- **Riscos & Mitigações:**
  - *Risco:* Degradação de performance devido a expressões regulares em grande volume de logs.
  - *Mitigação:* Compilar patterns de Regex estáticos, limitar tamanho de buffer inspecionado e utilizar appender assíncrono (`AsyncAppender`).
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.8: Alta Disponibilidade PostgreSQL (CloudNativePG/Patroni), PgBouncer e PITR
- **O que existe hoje:**
  - O PostgreSQL no Kubernetes é executado em `k8s/postgres.yaml` como um Deployment de réplica única (Single Point of Failure - SPOF) sem alta disponibilidade, sem failover automatizado e sem réplicas de leitura.
  - Não há camada de connection pooling dedicado (PgBouncer) para absorver picos de conexões concorrentes gerados por múltiplos pods do backend escalados pelo HPA.
  - Não há configuração de arquivamento de WAL (Write-Ahead Logging) contínuo nem política de Point-in-Time Recovery (PITR) para recuperação contra corrupção acidental ou desastre.
- **O que será alterado:**
  - Criar manifesto Kubernetes de alta disponibilidade corporativa com operador CloudNativePG (`k8s/postgres-ha-cluster.yaml`) contendo:
    * Cluster com 3 instâncias (1 primário de escrita + 2 réplicas com streaming replication e failover automático).
    * `PodAntiAffinity` para garantir dispersão entre diferentes nós/zonas do cluster.
    * Pooler integrado PgBouncer gerenciado em modo `transaction` (`k8s/postgres-pgbouncer.yaml`).
    * Configuração de backup automatizado (`ScheduledBackup`) com destino para S3/MinIO e retenção de 30 dias com WAL archiving.
  - Criar documentação operacional de Disaster Recovery e restauração (`docs/database/DISASTER_RECOVERY.md`) com procedimentos passo a passo de failover e restore de PITR.
  - Criar script de validação de backup e integridade de restauração (`scripts/test-db-restore.sh`).
  - Otimizar `application-prod.yml` do Spring Boot com dimensionamento fino do HikariCP integrado ao PgBouncer (`leak-detection-threshold`, `connection-timeout`, `keepalive-time`).
- **Riscos & Mitigações:**
  - *Risco:* Incompatibilidade de prepared statements entre Spring Data JPA / Hibernate e PgBouncer em modo `transaction`.
  - *Mitigação:* Configurar `prepareThreshold=0` na string de conexão JDBC de produção ou utilizar pool em modo transaction com parâmetros recomendados pelo PostgreSQL Global Development Group.
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.7: Rate Limit Distribuído via Redis e Dupla Dimensão (IP e Usuário)
- **O que existe hoje:**
  - `RateLimiterGlobalFilter` utiliza um `ConcurrentHashMap` em memória local na JVM do API Gateway.
  - Ao escalar horizontalmente para múltiplas réplicas (`k8s/hpa.yaml`), os limites são multiplicados pelo número de pods, fragilizando a defesa contra DoS e ataques de força bruta.
  - O controle de taxa é aplicado unicamente por IP, sem limites diferenciados por usuário autenticado.
  - Não há serviço de Redis provisionado no `docker-compose.yml` nem manifestos no Kubernetes (`k8s/redis.yaml`).
- **O que será alterado:**
  - Adicionar dependência `spring-boot-starter-data-redis-reactive` no `services/api-gateway/pom.xml`.
  - Configurar conexão do Redis no `services/api-gateway/src/main/resources/application.yml` com variáveis de ambiente (`SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`).
  - Atualizar `RateLimiterGlobalFilter` para operar de forma distribuída via `ReactiveStringRedisTemplate` com chave de janela deslizante e expiração automática (`EXPIRE 60s`).
  - Implementar rate limiting em duas dimensões independentes:
    1. **Por IP:** Proteção contra DoS e scraping (120 req/min geral; 15 req/min em `/api/auth/` e recuperação de senha).
    2. **Por Usuário Autenticado:** Proteção contra abuso de contas comprometidas (180 req/min geral; 10 req/min em operações sensíveis).
  - Implementar fallback resiliente para memória local caso o Redis esteja indisponível ou não configurado no ambiente local de testes unitários.
  - Adicionar container `redis:7-alpine` no `docker-compose.yml` com healthcheck.
  - Criar manifesto `k8s/redis.yaml` (Deployment + Service) e registrar no `k8s/kustomization.yaml`.
  - Atualizar a suíte de testes unitários em `RateLimiterGlobalFilterTest` para validar limites por IP, limites por usuário e endpoints de autenticação.
- **Riscos & Mitigações:**
  - *Risco:* Indisponibilidade temporária do Redis bloquear todas as requisições legítimas do hospital.
  - *Mitigação:* Mecanismo de degradação graciosa com fallback em memória e log de advertência estruturado, garantindo disponibilidade contínua.
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.6: Auditoria Confiável, Transacional e Rastreamento de Leituras Clínicas
- **O que existe hoje:**
  - `AuditoriaService.registrarAcao` engole exceções em um bloco genérico `catch (Exception e)` imprimindo em `System.err.println`, mascarando falhas críticas de auditoria e conformidade LGPD/CFM.
  - Gravação de auditoria não possui demarcação transacional explícita (`REQUIRES_NEW`), podendo ser afetada pelo rollback de transações externas ou falhar silenciosamente no listener pós-commit.
  - Não há métricas Micrometer no Prometheus nem eventos/alertas para falhas de auditoria.
  - Leituras de prontuários (`findMyHistorico`), dados clínicos de pacientes (`findClinicoById`), geração de PDFs (`gerarPdfReceita`, `gerarPdfAtestado`), triagens (`findByConsultaId`) e exames (`findByProntuario`) não registram trilha de auditoria sobre quem consultou os dados clínicos.
- **O que será alterado:**
  - `AuditoriaService`: anotar `registrarAcao` com `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
  - Remover `System.err` e adotar logging estruturado com SLF4J e MDC contextual (`audit.acao`, `audit.entidade`, `audit.entidadeId`, `audit.usuario`).
  - Adicionar métricas no `MedflowMetricsService`: `medflow.auditoria.gravadas` e `medflow.auditoria.falhas` (com tags de ação).
  - Publicar evento de alerta `AuditoriaFalhaEvent` e lançar `FalhaAuditoriaException` para tornar a falha visível e auditável caso o repositório falhe.
  - Implementar auditoria explícita de leituras clínicas em `ProntuarioService`, `PacienteService`, `DocumentoPdfService`, `TriagemService` e `ExameService`.
  - Criar migração Flyway `V12__indices_auditoria.sql` para indexar `auditoria(data_hora DESC)`, `auditoria(entidade, entidade_id)` e `auditoria(usuario_id)`.
  - Atualizar e expandir a suíte de testes unitários para cobrir caminhos de sucesso, falha estruturada, métricas, alertas e auditoria de leituras clínicas.
- **Riscos & Mitigações:**
  - *Risco:* Sobrecarga de transações com `REQUIRES_NEW` sob alta concorrência.
  - *Mitigação:* Queries de auditoria são inserts simples otimizados com índices; índices criados via Flyway; conexões liberadas imediatamente após cada insert.
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.5: Eliminação de N+1 na Ordenação da Fila Médica
- **O que existe hoje:**
  - Em `ConsultaService.agendaHoje`, a ordenação de prioridade Manchester executava `triagemRepository.findByConsultaId(id)` dentro do comparator (`consultas.sort(...)`), gerando $O(N \log N)$ queries concorrentes ao banco para cada comparação de pares.
  - No mapeamento final para DTO, `pagamentoRepository.findByConsultaId(consulta.getId())` era chamado para cada item da lista (mais $N$ queries).
- **O que será alterado:**
  - Adicionar `findByConsultaIdIn(Collection<UUID> consultaIds)` em `TriagemRepository` e `PagamentoRepository`.
  - Atualizar `ConsultaService.agendaHoje` para carregar as triagens e pagamentos de todas as consultas do dia em lote (2 queries únicas em vez de centenas).
  - Pré-computar os pesos de prioridade em um `Map<UUID, Integer>` em memória e utilizar esse mapa estático durante a ordenação, com zero queries disparadas durante o sort.
  - Repassar os IDs de pagamento pré-carregados para o mapeamento DTO, eliminando o loop de consultas a pagamentos.
  - Atualizar e expandir a suíte de testes em `ConsultaServiceTest` garantindo que `findByConsultaId` nunca é chamado no fluxo de agenda e que o número de queries é constante.
- **Riscos & Mitigações:**
  - *Risco:* Alteração na ordem dos pacientes na fila da agenda médica.
  - *Mitigação:* Testes unitários com casos de teste verificando que a ordem estrita do Protocolo de Manchester (Emergência > Alta > Média > Baixa > Horário) permanece rigorosamente intacta.
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.4: Varredura em Memória e Substituição por Queries Indexadas
- **O que existe hoje:**
  - `ProntuarioService.findMyHistorico` realizava busca de `Paciente` varrendo todos os registros (`findAll().stream().filter(...)`).
  - `FaturamentoService.gerarRelatorio` carrega todos os pagamentos da história da clínica via `pagamentoRepository.findAll()` e itera em memória sobre cada registro para calcular totais diários e mensais.
  - Ausência de métodos indexados diretos como `findByUsuarioId` e `existsByUsuarioId` em `PacienteRepository`, `MedicoRepository` e `FuncionarioRepository`.
  - Ausência de índices explícitos em colunas de alta frequência de agregação como `consulta.data_hora` e `pagamento.status`.
- **O que será alterado:**
  - Criar migração Flyway `V11__indices_otimizacao_buscas.sql` adicionando índices B-Tree em `consulta(data_hora)`, `pagamento(status)`, `medico(usuario_id)` e `funcionario(usuario_id)`.
  - Adicionar `findByUsuarioId(UUID usuarioId)` e `existsByUsuarioId(UUID usuarioId)` em `PacienteRepository`, `MedicoRepository` e `FuncionarioRepository`.
  - Atualizar `ProntuarioService` para recuperar o paciente usando diretamente `pacienteRepository.findByUsuarioId(usuario.getId())`.
  - Adicionar `findByUsuarioId` e validações de unicidade em `PacienteService` e endpoint protegido em `PacienteController`.
  - Otimizar `FaturamentoService` para usar agregações SQL diretas em `PagamentoRepository` (`somarTotalPagoPeriodo`, `countByStatus`, `somarTotalPendente`), eliminando completamente o carregamento em lote de entidades na JVM.
  - Atualizar e expandir a suíte de testes unitários para cobrir todos os métodos criados e refatorados.
- **Riscos & Mitigações:**
  - *Risco:* Regressão em endpoints existentes de relatório ou paciente.
  - *Mitigação:* Executar a suíte completa de 124 testes unitários e de integração antes e após cada alteração.
- **Como reverter:**
  - `git checkout HEAD~1`.

### Plano Técnico — Item 0.3: Vínculo Assistencial e Proteção IDOR em Prontuários
- **O que existe hoje:**
  - `ProntuarioService.findMyHistorico` executa `prontuarioRepository.findAll()` para médicos, vazando prontuários de toda a instituição.
  - `ProntuarioService.findById` valida posse apenas quando o usuário é `ROLE_PACIENTE`. Médicos podiam acessar prontuário de qualquer paciente sem vínculo assistencial.
  - `findById` registrava auditoria de escrita ("SALVAR_PRONTUARIO") em requisição de leitura.
  - `PacienteController` em `/{id}/clinico` permitia acesso a qualquer paciente autenticado sem validação de vínculo/propriedade.
- **O que será alterado:**
  - Adicionar `findByConsultaMedicoUsuarioEmail(String email)` em `ProntuarioRepository`.
  - Atualizar `ProntuarioService.findMyHistorico` para médicos utilizarem exclusivamente busca com filtro pelo e-mail do médico logado.
  - Atualizar `ProntuarioService.findById` para exigir **vínculo assistencial** (`existsByPacienteIdAndMedicoUsuarioEmail`) para médicos, lançando `403 AcessoNegadoException` se não houver vínculo.
  - Corrigir ação de auditoria de leitura de prontuário para `LEITURA_PRONTUARIO`.
  - Atualizar `AcessoSecurity.temVinculoAssistencialComPaciente` e proteger `PacienteController.findClinicoById` com `@acessoSecurity`.
  - Adicionar testes de permissão negativa comprovando o bloqueio de médicos sem vínculo e pacientes bisbilhoteiros.
- **Riscos & Mitigações:**
  - *Risco:* Médicos legítimos com consultas marcadas terem acesso bloqueado.
  - *Mitigação:* A verificação valida consultas passadas e futuras entre o médico e o paciente no banco de dados.
- **Como reverter:**
  - Reverter via `git checkout HEAD~1`.

### Plano Técnico — Item 0.2: Migração JWT para RS256 com kid, rotação e Refresh Token
- **O que existe hoje:**
  - `JwtService` e `JwtAuthGlobalFilter` usam HMAC-SHA256 (chave simétrica compartilhada).
  - O access token possui validade longa de 24 horas (`86400000ms`).
  - Não há entidade nem fluxo de `RefreshToken`.
  - Não há suporte a header `kid` para rotação de chaves.
- **O que será alterado:**
  - Criar `RsaKeyProvider` no backend para geração/carregamento de chaves RSA (2048-bit) com identificador `kid`.
  - Atualizar `JwtService` para assinar tokens com chave privada RSA (algoritmo RS256) e incluir `kid` no cabeçalho JWT. Reduzir validade do access token para 15 minutos (900.000 ms).
  - Manter suporte de validação legada como rede de segurança (`medflow.feature.jwt-legacy-fallback=true`).
  - Atualizar `JwtAuthGlobalFilter` no API Gateway para validar assinaturas com chave pública RSA e suporte a `kid`.
  - Criar migração Flyway `V10__criar_tabela_refresh_token.sql`.
  - Criar entidade `RefreshToken`, repositório `RefreshTokenRepository`, DTOs `RefreshTokenRequest` e `TokenRefreshResponse`.
  - Adicionar endpoints em `AuthController`: `POST /auth/refresh`, `POST /auth/revoke` e `GET /auth/public-key`.
  - Atualizar `AuthService` para emitir pares (Access Token + Refresh Token) com rotação e detecção de reúso.
- **Riscos & Mitigações:**
  - *Risco:* Invalidação de sessões de clientes existentes durante a transição.
  - *Mitigação:* Manter `token` no DTO `AuthResponse` e período de compatibilidade para tokens existentes.
- **Como reverter:**
  - Desligar a flag ou reverter via `git checkout HEAD~1`.

### Plano Técnico — Item 0.1: Segredos fora do Git
- **O que existe hoje:**
  - `backend/src/main/resources/application-dev.yml` possui `jwt.secret` exposto em texto plano.
  - `services/api-gateway/src/main/resources/application.yml` possui `jwt.secret` exposto em texto plano.
  - `services/api-gateway/.../JwtAuthGlobalFilter.java` possui valor default hardcoded de 64 caracteres.
  - `k8s/secret.yaml` possui `jwt-secret` exposto no repositório Git.
  - Não há `.env.example` na raiz do projeto.
  - O pipeline `.github/workflows/ci.yml` não possui etapa de Secret Scanning com Gitleaks.
- **O que será alterado:**
  - Configurar `backend` e `api-gateway` para obter `jwt.secret` estritamente via `${JWT_SECRET}`.
  - Criar `.env.example` com placeholders claros de configuração.
  - Atualizar `docker-compose.yml` para mapear `JWT_SECRET=${JWT_SECRET:-...}`.
  - Atualizar `k8s/secret.yaml` com placeholders e criar manifesto de referência `k8s/external-secret.yaml` para integração com External Secrets Operator (Vault/AWS Secrets Manager).
  - Adicionar job de secret scanning com Gitleaks no `.github/workflows/ci.yml` e arquivo `.gitleaks.toml`.
- **Riscos & Mitigações:**
  - *Risco:* Testes automatizados que sobem o contexto do Spring falharem por ausência de `${JWT_SECRET}`.
  - *Mitigação:* Configurar valor de teste específico no perfil de teste/dev ou via `System.setProperty` nos testes relevantes.
- **Como reverter:**
  - Reverter as alterações via `git checkout HEAD~1` na branch `feat/epico-0-correcoes-imediatas`.
| **Épico 0** | **0.6** Auditoria estruturada e sem silenciamento | Não iniciado | - | - |
| **Épico 0** | **0.7** Rate limit distribuído via Redis | Não iniciado | - | - |
| **Épico 0** | **0.8** PostgreSQL HA com PgBouncer | Não iniciado | - | - |
| **Épico 0** | **0.9** Logs centralizados Loki com mascaramento PII | Não iniciado | - | - |
| **Épico 0** | **0.10** Hardening de borda e segurança | Não iniciado | - | - |
| **Épico 1** | Multi-tenancy e base SaaS | Não iniciado | - | - |
| **Épico 2** | Segurança de dados e LGPD | Não iniciado | - | - |
| **Épico 3** | Arquitetura e confiabilidade | Não iniciado | - | - |
| **Épicos 4-7**| Clínica completa vendável | Não iniciado | - | - |
| **Épicos 8, 9, 16**| Gestão, interoperabilidade e certificação | Não iniciado | - | - |
| **Épicos 10-15**| Módulos hospitalares (Urgência, ADT, Cirurgia, Farmácia) | Não iniciado | - | - |

---

## Decisões (ADRs resumidos)
- **ADR-009:** Adoção estrita do `MedFlow_Protocolo_Execucao.md` como processo de garantia de qualidade para execução de todos os épicos do `MedFlow_Backlog_Completo_Hospital_Clinica.md`.

---

## Descobertas fora de escopo
- Quatro avisos de linter no frontend relacionados ao `AuthContext.tsx` (não impeditivos de build). Registrados para monitoramento.

---

## Armadilhas conhecidas
- `backend/pom.xml` requer que o plugin JaCoCo esteja ligado ao ciclo `test` para que a compilação do reactor agregador raiz consiga gerar o relatório HTML automaticamente.
- RateLimiter atual usa `ConcurrentHashMap` em memória; escalar réplicas do Gateway antes do Item 0.7 multiplica os limites locais.

---

## Próximo passo
- Criar a tag Git `baseline-antes-evolucao`.
- Criar a branch de trabalho para o Épico 0: `feat/epico-0-correcoes-imediatas`.
- Iniciar o **Item 0.1** (Segredos fora do Git).
