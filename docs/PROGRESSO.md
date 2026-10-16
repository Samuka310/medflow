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
| **Épico 0** | **0.5** N+1 na ordenação de agenda médica | Não iniciado | - | - |

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
