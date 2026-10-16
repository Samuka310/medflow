# Status de Análise de Gaps — MedFlow

Este documento mapeia o status evolutivo de todos os itens do backlog (`MedFlow_Backlog_Completo_Hospital_Clinica.md`) de acordo com o protocolo de execução (`MedFlow_Protocolo_Execucao.md`).

---

## Tabela Geral de Itens e Status

| Item | Descrição Resumida | Prioridade | Status | Arquivos / Commits Principais | Testes Associados | Riscos & Observações |
| :--- | :--- | :---: | :---: | :--- | :--- | :--- |
| **0.1** | Segredos fora do Git | P0 | **Feito** | `.env.example`, `.gitleaks.toml`, `k8s/external-secret.yaml` (Commit `4e1778b`) | Validação de inicialização sem secrets expostos | Sem segredos no git; dependência de injeção em runtime |
| **0.2** | JWT RS256 com kid, rotação e Refresh Token | P0 | **Feito** | `RsaKeyProvider.java`, `RefreshTokenService.java`, `V10__criar_tabela_refresh_token.sql` (Commit `32abdaf`) | `RefreshTokenServiceTest` (5 testes), `AuthServiceTest` | Chaves expiram; rotação com cache em memória |
| **0.3** | Vazamento de prontuários & IDOR | P0 | **Feito** | `AcessoSecurity.java`, `ProntuarioService.java`, `DocumentoPdfService.java` (Commit `9285ec0`) | `ProntuarioServiceTest`, `DocumentoPdfServiceTest` (6 testes) | ABAC exige vínculo assistencial para médicos |
| **0.4** | Varredura em memória de pacientes | P0 | **Feito** | `V11__indices_otimizacao_buscas.sql`, `PacienteRepository.java`, `FaturamentoService.java` (Commit `0ac3c5a`) | `FaturamentoServiceTest`, `PacienteServiceTest` (14 testes) | Índices cobrem agregações de faturamento e busca |
| **0.5** | N+1 na ordenação de agenda médica | P0 | **Feito** | `ConsultaService.java`, `TriagemRepository.java`, `PagamentoRepository.java` (Commit `0413df9`) | `ConsultaServiceTest` (11 testes) | Carregamento em lote $O(1)$ queries |
| **0.6** | Auditoria confiável e leituras clínicas | P0 | **Feito** | `AuditoriaService.java`, `V12__indices_auditoria.sql`, `MedflowMetricsService.java` (Commit `5250eab`) | `AuditoriaServiceTest` (5 testes) | `REQUIRES_NEW` isola logs transacionais |
| **0.7** | Rate limit distribuído via Redis | P0 | **Feito** | `RateLimiterGlobalFilter.java`, `docker-compose.yml`, `k8s/redis.yaml` (Commit `1699672`) | `RateLimiterGlobalFilterTest` (8 testes) | Fallback gracioso em memória caso Redis caia |
| **0.8** | PostgreSQL HA e Backup PITR | P0 | **Feito** | `k8s/postgres-ha-cluster.yaml`, `k8s/postgres-pgbouncer.yaml`, `scripts/test-db-restore.sh` (Commit `e13be45`) | Script de validação de backup e restore | PgBouncer em modo transaction requer `prepareThreshold=0` |
| **0.9** | Logs centralizados e Mascaramento PII | P0 | **Feito** | `logback-spring.xml`, `PiiMasker.java`, `docker-compose.yml`, `k8s/loki-promtail.yaml` (Commit `ab57a25`) | `PiiMaskerTest`, `CorrelationIdServletFilterTest` (12 testes) | Regex de alta performance para sanitização |
| **0.10** | Hardening base e Segurança de Borda | P0 | **Feito** | `SecurityConfig.java`, `SecurityHeadersFilter.java`, `Dockerfile`, `k8s/network-policies.yaml` (Commit `3762452`) | `SecurityHeadersFilterTest`, `SecurityConfigTest`, `CorsConfigTest` (5 testes) | Zero-trust no cluster e headers OWASP |
| **1.1** | Modelo de Tenant e Unidades | P1 | *Planejado* | Branch `feat/epico-1-multi-tenancy` (Flyway `V13`, `Tenant`, `Unidade`, `tenant_id`) | Testes unitários de isolamento e contexto | Migração de dados de tenant piloto |
| **1.2** | Row-Level Security no Postgres | P1 | *Pendente* | RLS policies PostgreSQL + Session tenant variable | Testes de isolamento a nível de conexão SQL | Bypass com usuário superuser evitado |
| **1.3** | Testes de vazamento entre tenants | P1 | *Pendente* | Matriz de testes cross-tenant em todos os endpoints | Testes automatizados de invasão cross-tenant | Nenhum dado de tenant A acessível por B |
| **1.4** | Console de administração de tenants | P1 | *Pendente* | Endpoints e telas de gestão SaaS | Testes de onboarding de novas clínicas | Gestão de limites de uso e plano |
| **1.5** | Configuração por tenant | P1 | *Pendente* | Tabela de preços, feriados, convênios por clínica | Testes de resolução dinâmica de regras | Customização sem alterar código |
| **1.6** | Onboarding e migração de dados | P1 | *Pendente* | Importador de legado com validação e staging | Testes com CSV/JSON com erros sintáticos | Validação de duplicidade e higienização |
| **1.7** | Backup e encerramento de contrato | P1 | *Pendente* | Exportação isolada por tenant e expurgo com laudo | Testes de expurgo auditado | Conformidade com retenção legal CFM/LGPD |
| **2.1 - 2.21** | Segurança de dados e LGPD avançada | P0/P1 | *Pendente* | MFA, Criptografia de campo (KMS), Trilha WORM, RoPA/RIPD | Testes de segurança ASVS e criptografia | Chaves de criptografia e conformidade ANPD |
| **3.1 - 3.7** | Arquitetura, Outbox e Confiabilidade | P1/P2 | *Pendente* | Outbox Pattern, DLQ RabbitMQ, Cache Redis L2 | Testes de concorrência e idempotência de mensageria | Garantia de entrega at-least-once |
| **4.1 - 4.10** | Gestão de Pacientes e Recepção | P1 | *Pendente* | Biometria/foto, elegibilidade de convênio, fila painel | Testes de agendamento e elegibilidade | Atendimento ambulatorial ponta a ponta |
| **5.1 - 5.12** | PEP Clínico e Prescrição Digital | P1 | *Pendente* | Assinatura ICP-Brasil, CID-10/11, Interações medicamentosas | Testes de validação de assinatura e posologia | Conformidade CFM 1.821 e SBIS |
| **6.1 - 6.6** | SADT e Exames Complementares | P1 | *Pendente* | Fluxo de pedidos, coleta, laudos com modelo | Testes de entrega segura e controle de status | Rastreabilidade de resultados críticos |
| **7.1 - 7.9** | Faturamento, Convênios e TISS | P1 | *Pendente* | XML TISS ANS, tabelas CBHPM/AMB/TUSS, glosas | Testes de validação XSD TISS e conciliação | Rejeição por operadoras de saúde |
| **8.1 - 8.6** | Financeiro e Estoque Clínico | P2 | *Pendente* | Contas a pagar/receber, DRE, rastreio de lote/validade | Testes contábeis e de baixa por atendimento | Desabastecimento e perdas de validade |
| **9.1 - 9.5** | Telemedicina e Portal do Paciente | P2 | *Pendente* | WebRTC seguro, termo de consentimento, agendamento online | Testes de sigilo e links temporários | Resolução CFM 2.314/2022 |
| **10.1 - 10.7** | Pronto Atendimento e Urgência | P2 | *Pendente* | Manchester informatizado, tempo porta-médico, fast track | Testes de reclassificação e painéis Kanban | Sobrecarga de leitos de observação |
| **11.1 - 11.8** | Internação e Gestão de Leitos | P3 | *Pendente* | Censo hospitalar, higienização, transferência, kanban | Testes de ocupação e alta hospitalar | Bloqueio indevido de leitos |
| **12.1 - 12.8** | Centro Cirúrgico e CME | P3 | *Pendente* | Mapa cirúrgico, checklist OMS, rastreabilidade OPME | Testes de conflito de sala/equipe e kits cirúrgicos | Cirurgia segura e rastreio de materiais |
| **13.1 - 13.8** | Farmácia Hospitalar e Beira-leito | P3 | *Pendente* | Dose unitária, checagem beira-leito (9 certos), SNGPC | Testes de dispensação e leitura de código de barras | Erros de administração medicamentosa |
| **14.1 - 14.4** | LIS (Laboratório) e RIS/PACS (Imagem) | P3 | *Pendente* | Interfaces HL7/ASTM, worklist DICOM, visualizador web | Testes de comunicação biomédica e laudos | Integridade de imagens e valores de pânico |
| **15.1 - 15.5** | Faturamento Hospitalar e SUS | P3 | *Pendente* | Conta hospitalar, AIH, BPA, SIGTAP, SIH/SUS | Testes de consistência de faturamento e glosas | Conformidade com regras de repasse do SUS |
| **16.1 - 16.7** | Certificação SBIS/CFM e Preparo Comercial | P1/P2 | *Pendente* | Requisitos NGS2, modo downtime, manuais por perfil | Testes de usabilidade e plano de contingência | Acreditação hospitalar ONA/JCI e SBIS |
