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
| **Épico 0** | **0.1** Segredos fora do Git | Não iniciado | - | - |
| **Épico 0** | **0.2** JWT RS256/EdDSA com rotação | Não iniciado | - | - |
| **Épico 0** | **0.3** Correção vazamento prontuários | Não iniciado | - | - |
| **Épico 0** | **0.4** Varredura em memória de pacientes | Não iniciado | - | - |
| **Épico 0** | **0.5** N+1 na ordenação de agenda médica | Não iniciado | - | - |
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
