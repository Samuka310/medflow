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
| **Épico 0** | **0.3** Correção vazamento prontuários | Não iniciado | - | - |
| **Épico 0** | **0.4** Varredura em memória de pacientes | Não iniciado | - | - |
| **Épico 0** | **0.5** N+1 na ordenação de agenda médica | Não iniciado | - | - |

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
