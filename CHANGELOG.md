# Changelog

Todas as alterações notáveis neste projeto serão documentadas neste arquivo.
O formato é baseado no [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

---

## [epico-0-concluido] - 2026-10-16

### Épico 0 — Correções Imediatas Apontadas na Auditoria (P0)

#### Adicionado
- **Segurança de Segredos (Item 0.1):** Externalização de todos os segredos via variáveis de ambiente, criação de `.env.example`, política de varredura `.gitleaks.toml` e manifesto `k8s/external-secret.yaml`.
- **JWT RS256 e Refresh Token (Item 0.2):** Provedor de chaves assimétricas `RsaKeyProvider`, rotação de chaves com parâmetro `kid`, tokens JWT de curta duração (15 min), entidade `RefreshToken` persistida com detecção de replay e revogação em cascata (migração Flyway `V10__criar_tabela_refresh_token.sql`).
- **Controle de Acesso Baseado em Atributos (ABAC) (Item 0.3):** Vínculo assistencial estrito em `AcessoSecurity`, bloqueio de acesso a prontuários e downloads de PDFs clínicos sem vínculo prévio, e mitigação completa de vulnerabilidades IDOR.
- **Otimização de Banco de Dados (Item 0.4):** Substituição de varreduras em memória por consultas indexadas (`findByUsuarioId`) e agregações SQL diretas (`somarTotalPagoPeriodo`), com índices B-Tree criados pela migração Flyway `V11__indices_otimizacao_buscas.sql`.
- **Eliminação de N+1 na Agenda Médica (Item 0.5):** Carregamento em lote (`findByConsultaIdIn`) de triagens e pagamentos para a ordenação por prioridade de Manchester em `ConsultaService.agendaHoje`, garantindo complexidade de queries $O(1)$.
- **Auditoria Transacional e Rastreamento de Leituras Clínicas (Item 0.6):** Propagação transacional independente (`REQUIRES_NEW`), contexto estruturado MDC, métricas Micrometer, publicação de alertas `AuditoriaFalhaEvent` e rastreamento de leituras clínicas em prontuários, PDFs, triagens e exames (migração Flyway `V12__indices_auditoria.sql`).
- **Rate Limit Distribuído via Redis (Item 0.7):** Implementação reativa no API Gateway (`RateLimiterGlobalFilter`) com janela deslizante, proteção em dupla dimensão (IP e Usuário), limites estritos em endpoints de autenticação e fallback gracioso em memória.
- **Cluster PostgreSQL de Alta Disponibilidade (Item 0.8):** Orquestração Kubernetes com CloudNativePG (3 nós em quórum, anti-afinidade, failover automático), PgBouncer em modo transação, backups contínuos com arquivamento WAL, runbook de Disaster Recovery (`docs/database/DISASTER_RECOVERY.md`) e scripts de teste de restauração PITR.
- **Observabilidade Centralizada e Mascaramento de PII (Item 0.9):** Ingestão estruturada de logs JSON com `logstash-logback-encoder`, mascaradores automáticos de dados sensíveis LGPD (`PiiMaskingConverter`), correlação distribuída de traces e integração com Grafana Loki e Promtail.
- **Hardening de Borda e Segurança em Profundidade (Item 0.10):** Cabeçalhos HTTP de segurança recomendados pela OWASP (HSTS, CSP, X-Frame-Options: DENY, X-Content-Type-Options: nosniff, Referrer-Policy, Permissions-Policy), restrição do Actuator na borda, sanitização de métodos CORS, desativação de Swagger em produção, execução com containers não-root (UID 10001 `medflow`) e NetworkPolicies zero-trust com Default-Deny no Kubernetes.

#### Modificado
- Suíte de testes automatizados expandida de 112 para **161 testes verdes** (148 no backend + 13 no gateway).
- Documentação centralizada de progresso (`docs/PROGRESSO.md`) atualizada com evidências de auditoria e conformidade técnica.

---

## [baseline-antes-evolucao] - 2026-10-15

### Linha de Base (Baseline)
- Tag inicial com o repositório estável (112 testes passando, build de frontend Vite e imagens base).
