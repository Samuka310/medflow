# MedFlow — Backlog completo para virar produto de nível comercial (clínica e hospital)

Cole este arquivo inteiro na outra IA (com o repositório aberto). Ele está organizado como instruções + backlog priorizado.

---

## PAPEL E REGRAS

Você é arquiteto/engenheiro sênior de software de saúde (Java/Spring, React, PostgreSQL, RabbitMQ, Kubernetes) e vai evoluir o **MedFlow**. Base atual (segundo a auditoria interna): Java 21, Spring Boot, API Gateway (Spring Cloud Gateway), notification-service, RabbitMQ, PostgreSQL, React 19, Flyway, RFC 7807, Micrometer/Prometheus/Grafana, K8s com HPA, 6 perfis (Paciente, Recepção, Triagem, Médico, Faturamento, Admin), triagem Manchester, agendamento com lock pessimista, estorno via Strategy, importação CSV, relatórios em streaming, PDFs de receita/atestado (iText), auditoria.

Regras:
1. **Verifique o código antes de implementar.** Se algo já existir (ex.: entidades Exame, Receita, Atestado, Prontuario), estenda; não duplique. Registre o que encontrou.
2. Trabalhe por épico, uma branch por épico, commits pequenos (Conventional Commits). Toda mudança de banco = nova migration Flyway; nunca editar migration aplicada.
3. Cada item entrega: código + testes (unitário, integração com Testcontainers rodando Flyway, e teste de permissão por perfil) + documentação (`docs/`) + atualização do OpenAPI.
4. Nunca declarar item concluído sem teste verde. Não inventar funcionalidade nem métrica.
5. Dados de saúde são dados sensíveis (LGPD art. 5º II e art. 11). Qualquer novo endpoint que devolva dado clínico precisa de regra de acesso explícita, auditoria e teste de negação (usuário sem vínculo recebe 403).
6. Confirme requisitos legais e normativos citados abaixo na versão vigente antes de implementar (podem ter sido atualizados). Marque no documento o que depende de validação jurídica.
7. Ao fim de cada épico, entregue resumo: o que mudou, como testar, riscos, pendências.

Legenda de prioridade: **P0** = corrigir antes de qualquer cliente real; **P1** = necessário para vender a clínicas; **P2** = diferencial/escala; **P3** = módulo hospitalar completo.

---

## ÉPICO 0 — Correções imediatas apontadas na auditoria (P0)

- [ ] **0.1 Segredos fora do Git:** remover `jwt.secret` de `.yml` e `k8s/secret.yaml`; usar variáveis de ambiente/Secrets do K8s, com External Secrets + Vault/AWS Secrets Manager/GCP Secret Manager. **Rotacionar o segredo atual** (considere-o vazado, pois está no histórico do Git; limpar histórico com `git filter-repo` ou assumir a rotação). Adicionar secret scanning (gitleaks) no CI.
- [ ] **0.2 JWT:** migrar de HS256 com segredo compartilhado para **RS256/EdDSA** (gateway valida com chave pública; só o serviço de auth assina), suporte a `kid` e rotação de chaves, access token curto (5–15 min) + refresh token com rotação e revogação.
- [ ] **0.3 Vazamento de prontuários:** `ProntuarioService.findMyHistorico` usa `findAll()` para médico. Restringir ao **vínculo assistencial** (ver 2.3). Revisar **todos** os `findAll()` e consultas sem filtro de dono em todos os serviços, procurando outros casos de IDOR.
- [ ] **0.4 Varredura em memória:** substituir `pacienteRepository.findAll().stream()...` por `findByUsuarioId`. Buscar padrão equivalente no restante do código.
- [ ] **0.5 N+1 na fila médica:** `ConsultaService.agendaHoje` consulta triagem dentro do comparador. Usar `JOIN FETCH`/projeção ou carregar prioridades em `Map` antes de ordenar. Teste com contagem de queries (Hibernate statistics ou `datasource-proxy`) garantindo número constante de queries.
- [ ] **0.6 Auditoria confiável:** `AuditoriaService.registrarAcao` engole exceção e usa `System.err`; corrigir para falha visível (log estruturado + métrica + alerta), gravação transacional/outbox, e corrigir ação errada ("SALVAR_PRONTUARIO" em leitura). Auditoria deve registrar também **leituras** de dado clínico (quem viu o quê e quando).
- [ ] **0.7 Rate limit distribuído:** trocar `ConcurrentHashMap` por Redis (`RequestRateLimiter` do Spring Cloud Gateway ou Bucket4j + Redis). Limites por IP **e** por usuário, mais rígidos em login/recuperação de senha.
- [ ] **0.8 Banco de dados:** não rodar Postgres como StatefulSet de 1 réplica em produção. Usar banco gerenciado (RDS/Cloud SQL) ou operador HA (CloudNativePG/Patroni), com PgBouncer, backups automáticos e PITR; testar restauração.
- [ ] **0.9 Logs centralizados:** Loki (ou ELK) + Promtail/Fluent Bit; logs JSON com `traceId`/`requestId`; **mascarar PII** (CPF, nome, e-mail, dados clínicos) nos logs.
- [ ] **0.10 Hardening base:** cabeçalhos de segurança (HSTS, CSP, X-Content-Type-Options, frame-options), CORS restrito por ambiente, actuator só `health` público, Swagger desabilitado ou protegido em produção, usuários/senhas padrão removidos (guest/admin etc.), imagens Docker não-root e com scan (Trivy), NetworkPolicies no K8s.

## ÉPICO 1 — Multi-tenancy e base de produto SaaS (P1)

Para vender a mais de um cliente é preciso isolar dados.
- [ ] 1.1 Modelo de **tenant** (clínica/hospital) com unidades/filiais. Coluna `tenant_id` em todas as tabelas de negócio.
- [ ] 1.2 Isolamento forte: **Row-Level Security** do PostgreSQL (`SET app.tenant_id` por requisição) + filtro Hibernate; tenant extraído do JWT, nunca do payload.
- [ ] 1.3 Testes automatizados de **vazamento entre tenants** (usuário do tenant A nunca lê/escreve dado do B, em todos os endpoints).
- [ ] 1.4 Console de administração da plataforma: criar tenant, plano, limites, ativar/suspender, métricas de uso.
- [ ] 1.5 Configuração por tenant: logo, horário de funcionamento, feriados, convênios aceitos, regras de cancelamento, tabela de preços.
- [ ] 1.6 Onboarding e migração de dados (importação de pacientes/agenda de sistemas anteriores, com validação e relatório).
- [ ] 1.7 Backup/exportação por tenant e processo de **encerramento de contrato** (devolver dados, eliminar com certificado).

## ÉPICO 2 — Segurança de dados e LGPD de nível comercial (P0/P1)

**Autenticação e sessão**
- [ ] 2.1 **MFA** (TOTP; opcional WebAuthn/passkeys) obrigatório para ADMIN, MEDICO e FATURAMENTO. Política de senha, verificação contra senhas vazadas, bloqueio progressivo, recuperação de senha segura, notificação de login suspeito, encerramento de sessões, expiração por inatividade.
- [ ] 2.2 Avaliar delegar identidade a um IdP (Keycloak/Auth0/Cognito) com OIDC, SSO corporativo (SAML/OIDC) para hospitais.

**Autorização**
- [ ] 2.3 **Vínculo assistencial (care relationship):** médico só acessa prontuário de paciente que atende/atendeu (consulta, equipe, encaminhamento ativo) ou por exceção registrada. Implementar como ABAC no `AcessoSecurity` com política centralizada e testada (matriz perfil × recurso × vínculo).
- [ ] 2.4 **Acesso de emergência ("break-glass"):** acesso excepcional com justificativa obrigatória, alerta ao DPO/gestor e revisão posterior.
- [ ] 2.5 Segregação de dados clínicos sensíveis por categoria (saúde mental, HIV/IST, violência, reprodutivo) com restrição reforçada e visibilidade explícita ao paciente.
- [ ] 2.6 Princípio do menor privilégio: perfis granulares (ex.: enfermagem, técnico, farmacêutico, gestor, DPO, auditor somente leitura), permissões por tenant, revisão periódica de acessos.

**Proteção do dado**
- [ ] 2.7 **Criptografia em trânsito** (TLS 1.2+ em tudo, inclusive entre serviços/mTLS via service mesh ou cert-manager) e **em repouso** (disco/banco criptografado + criptografia em nível de campo para CPF, documentos, anotações clínicas, com chaves em KMS e rotação; hash/blind index para busca por CPF).
- [ ] 2.8 Criptografia e controle dos anexos (laudos, imagens): storage S3/MinIO com SSE-KMS, URLs pré-assinadas curtas, antivírus (ClamAV) no upload, validação de tipo/tamanho.
- [ ] 2.9 **Auditoria imutável:** trilha append-only com encadeamento de hash (hash chain) ou WORM; retenção definida; consulta/relatório para o paciente ("quem acessou meu prontuário").
- [ ] 2.10 **Versionamento imutável do prontuário:** nenhuma edição apaga o registro anterior; correções via adendo/retificação com autor e motivo; trava de edição após assinatura/finalização.

**LGPD operacional**
- [ ] 2.11 **Consentimento:** registro versionado de termos/consentimentos por finalidade (atendimento, telemedicina, compartilhamento, pesquisa), revogação e prova.
- [ ] 2.12 **Direitos do titular:** exportar dados do paciente (formato legível e FHIR), correção, anonimização/eliminação onde permitido, com **regras de retenção** (prontuário tem prazo mínimo legal de guarda, confirmar na legislação vigente, p. ex. Lei 13.787/2018 e normas do CFM) e fluxo de pedido com prazo e rastreio.
- [ ] 2.13 Política de retenção e descarte automatizados por tipo de dado; bloqueio de exclusão indevida.
- [ ] 2.14 Documentação de conformidade em `docs/compliance/`: RoPA (registro de operações), RIPD/DPIA, política de privacidade, política de segurança, plano de resposta a incidentes (com fluxo de comunicação à ANPD e titulares), matriz de operadores/suboperadores, modelo de contrato DPA.
- [ ] 2.15 Minimização: mascarar CPF/telefone em telas e exportações conforme perfil; relatórios CSV com controle de acesso, marca d'água/ID de exportação e auditoria.

**Engenharia de segurança**
- [ ] 2.16 CI com SAST (CodeQL/Semgrep), dependency scanning (OWASP Dependency-Check/Dependabot/Renovate), container scan (Trivy), SBOM (CycloneDX), IaC scan (Checkov), DAST (OWASP ZAP) em ambiente de staging.
- [ ] 2.17 Checklist **OWASP ASVS nível 2** (e API Security Top 10) mapeado em documento com evidência de cada controle.
- [ ] 2.18 Proteções: validação rigorosa de entrada, proteção contra SSRF/XXE/zip-bomb em importações CSV/arquivos, limites de payload, CSRF onde houver cookie, proteção de enumeração de usuários.
- [ ] 2.19 WAF/CDN na borda, DDoS, proteção de bots no login (captcha adaptativo).
- [ ] 2.20 **Backup e DR:** RPO/RTO definidos, backup criptografado em outra região, teste de restauração periódico automatizado, runbook de desastre.
- [ ] 2.21 Preparar para **pentest externo** e política de divulgação responsável (`SECURITY.md`).

## ÉPICO 3 — Arquitetura e confiabilidade (P1/P2)

- [ ] 3.1 **Outbox pattern** + consumidor idempotente persistente (hoje há cache L1 em memória + banco L2; garantir que L1 não seja a única proteção ao escalar réplicas) e **DLQ** com retry exponencial no RabbitMQ; monitorar filas.
- [ ] 3.2 Definir **bounded contexts** e limites de serviço: Identidade, Cadastro/Pacientes, Agenda, Clínico (prontuário, exames, prescrições), Financeiro/Faturamento, Notificações, (futuros) Internação, Farmácia, Centro Cirúrgico. Banco/schema por serviço, sem joins entre serviços, comunicação por eventos/API.
- [ ] 3.3 **Cache** com Redis (cadastros de referência, sessões, rate limit), invalidação por evento.
- [ ] 3.4 Locks otimistas (`@Version`) nas entidades de escrita concorrente; revisar transações e níveis de isolamento.
- [ ] 3.5 Contratos de eventos versionados (schema registry ou JSON Schema) e **contract tests** (Pact/Spring Cloud Contract) entre serviços.
- [ ] 3.6 **Observabilidade completa:** tracing distribuído (OpenTelemetry → Tempo/Jaeger), logs (Loki), métricas, **SLOs** e alertas (Alertmanager) para latência, erro, fila acumulada, falha de auditoria, falha de login em massa.
- [ ] 3.7 **Testes de carga e resiliência:** k6/Gatling com metas (ex.: p95 por endpoint), teste de pico de agendamento concorrente, chaos (derrubar serviço/banco/fila) e documentar resultados.
- [ ] 3.8 K8s produção: `PodDisruptionBudget`, probes corretas, requests/limits, HPA por métrica real, anti-affinity, múltiplas zonas, rollout blue/green ou canary, Helm/Kustomize, **IaC com Terraform**, GitOps (Argo CD/Flux), ambientes dev/staging/prod separados.
- [ ] 3.9 Versionamento de API (`/v1`), política de depreciação, paginação/filtros padronizados em todas as listagens, idempotency-key em operações críticas (pagamento, agendamento).
- [ ] 3.10 Fuso horário e calendário: persistir `Instant`/`timestamptz`, exibir por tenant; internacionalização (pt-BR + en) e acessibilidade (WCAG 2.1 AA).
- [ ] 3.11 Alta disponibilidade e capacidade documentadas (`docs/CAPACITY.md`: números medidos, não estimados).

## ÉPICO 4 — Cadastro e agenda de clínica completa (P1)

- [ ] 4.1 **Paciente completo:** CPF/CNS, documentos, nome social, sexo/gênero, endereço, contatos, responsável legal (menores/incapazes), estado civil, profissão, convênio(s) e carteirinha com validade, foto, anexos, alergias, observações críticas; detecção de **duplicidade** (match por CPF/nome/nascimento) e mesclagem auditada.
- [ ] 4.2 **Profissionais:** CRM/UF (validação), especialidades e RQE, tipos de atendimento, duração por procedimento, agenda semanal com intervalos, férias/bloqueios, feriados, múltiplas unidades.
- [ ] 4.3 **Agenda avançada:** recursos físicos (consultórios, salas, equipamentos), agendamento de procedimento com recurso + profissional, encaixe, **lista de espera** com preenchimento automático ao cancelar, recorrência (retornos, sessões de fisioterapia), retorno com regra de prazo, overbooking controlado, visão semanal/mensal, múltiplos profissionais lado a lado, drag-and-drop.
- [ ] 4.4 **Confirmação e redução de faltas (no-show):** lembretes automáticos (e-mail/SMS/WhatsApp Business) com link de confirmar/cancelar/remarcar, métricas de no-show, regra de bloqueio opcional.
- [ ] 4.5 **Teleconsulta:** sala de vídeo (WebRTC/Jitsi/Twilio/Daily), termo de consentimento, registro da sessão no prontuário, conformidade com normas de telemedicina vigentes.
- [ ] 4.6 **Portal/app do paciente (PWA):** agendar, ver histórico, exames, receitas, pagar, atualizar cadastro, termos/consentimentos, ver quem acessou o prontuário.
- [ ] 4.7 Painel de chamada/TV da recepção e totem de autoatendimento/check-in (QR code).
- [ ] 4.8 Notificações reais: provedores de e-mail/SMS/WhatsApp com templates, preferências do paciente, opt-out e log de entrega no notification-service.

## ÉPICO 5 — Prontuário e documentos clínicos (P1)

- [ ] 5.1 **Prontuário estruturado:** anamnese (queixa, HDA, antecedentes, hábitos, histórico familiar), exame físico, hipótese diagnóstica, conduta, evolução; formato SOAP; modelos/templates por especialidade; **lista de problemas**, **alergias**, **medicações em uso**, vacinas, histórico cirúrgico, escalas e gráficos de sinais vitais ao longo do tempo (peso, PA, glicemia).
- [ ] 5.2 **Diagnóstico:** CID-10 (e preparar CID-11) e CIAP-2 com busca; base oficial importada por migration versionada.
- [ ] 5.3 **Anexos clínicos** (fotos, PDFs, imagens) com controle de acesso, categorias e miniaturas.
- [ ] 5.4 **Prescrição de medicamentos:** base de medicamentos (CMED/ANVISA, princípio ativo, apresentação, posologia), **alerta de alergia, interação medicamentosa e duplicidade terapêutica**, receituário simples, **controlado (B, C1) com numeração e regras de retenção**, antimicrobianos, uso contínuo, favoritos/modelos de prescrição. Avaliar integração com plataforma existente (ex.: Memed) em vez de reconstruir a base.
- [ ] 5.5 **Assinatura digital ICP-Brasil (PAdES) com certificado A1/A3** (ou serviço de assinatura em nuvem tipo BirdID/VIDaaS/etc.) para receitas, atestados, laudos e encaminhamentos; QR code/URL de verificação de autenticidade; carimbo de tempo. Observação: a auditoria cita "Portaria CFM 2.299/21"; o instrumento é a **Resolução CFM nº 2.299/2021** e **confirme as regras vigentes**, inclusive a necessidade de certificação **SBIS/CFM (NGS1/NGS2)** para sistemas de prontuário eletrônico sem papel.
- [ ] 5.6 **Atestados e declarações** com modelos configuráveis, CID opcional com consentimento, comprovante de comparecimento, laudos, relatórios médicos, encaminhamentos e contrarreferência.
- [ ] 5.7 Termos de consentimento informado (TCLE) por procedimento, com assinatura (digital/biométrica/desenho) e arquivo no prontuário.
- [ ] 5.8 **Finalização e trava:** consulta finalizada gera registro assinado e imutável; edição só por adendo.
- [ ] 5.9 Resumo clínico do paciente (visão única: problemas, alergias, medicações, últimos exames) e linha do tempo.

## ÉPICO 6 — Exames e procedimentos ambulatoriais (P1)

Hoje existe a entidade `Exame`; verifique a profundidade e evolua para o ciclo completo.
- [ ] 6.1 **Catálogo de procedimentos e exames** baseado em **TUSS** (e SIGTAP para SUS), com código, nome, preparo, prazo, valor por tabela/convênio, necessidade de autorização, recursos exigidos.
- [ ] 6.2 **Ciclo do exame:** pedido (pelo médico, com indicação clínica) → agendamento → guia/etiqueta → coleta/realização → resultado → laudo → liberação → ciência do médico e do paciente. Status e SLA em cada etapa.
- [ ] 6.3 **Resultados:** laudo estruturado e/ou anexo PDF; valores de referência por sexo/idade, destaque de alterados, **alerta de valor crítico** ao médico, histórico comparativo e gráfico de evolução.
- [ ] 6.4 Integração com laboratórios (HL7 v2/ORU, API de parceiros como Dasa/Fleury/Hermes Pardini quando aplicável) e com imagem (DICOM/PACS; visualizador web como OHIF; ver Épico 9).
- [ ] 6.5 **Procedimentos clínicos/ambulatoriais** (curativo, sutura, inalação, infiltração, pequenas cirurgias, sessões): agenda com recurso, ficha de execução, materiais/medicamentos consumidos (baixa de estoque), profissional executante, termo de consentimento, cobrança.
- [ ] 6.6 **Pacotes e protocolos** (check-up, pré-natal, pré-operatório) que geram vários pedidos de uma vez.
- [ ] 6.7 Fila de retorno de exame pendente e alertas de exames não retirados/não revisados.

## ÉPICO 7 — Financeiro e convênios (P1)

Grande parte das clínicas brasileiras vive de convênio; sem isso o produto vende pouco.
- [ ] 7.1 **Convênios e planos:** cadastro, tabelas de preço por convênio, regras de cobertura, carência, coparticipação, validação de elegibilidade (online quando houver).
- [ ] 7.2 **Faturamento TISS/TUSS (ANS):** guias de consulta, SP/SADT, honorários, internação (GTO/resumo), geração de **lotes XML** na versão vigente do padrão TISS, validação por XSD, protocolo de envio, retorno e **gestão de glosas** (recurso, reapresentação), conciliação de repasse, relatórios por convênio.
- [ ] 7.3 **Autorização prévia** (solicitação, acompanhamento, número de autorização).
- [ ] 7.4 **Caixa:** abertura/fechamento, sangria, formas de pagamento, parcelamento, descontos com alçada, pacotes, adiantamento/crédito do paciente.
- [ ] 7.5 Pagamentos online: Pix (QR dinâmico), cartão via gateway, boleto, conciliação automática por webhook (idempotente).
- [ ] 7.6 Contas a pagar/receber, fluxo de caixa, DRE gerencial, centro de custo.
- [ ] 7.7 **Repasse médico:** regras por profissional/procedimento/convênio, cálculo, fechamento mensal, extrato.
- [ ] 7.8 **Nota fiscal de serviço (NFS-e)** por município/padrão nacional, recibos, comprovante para imposto de renda.
- [ ] 7.9 Tabela de preços particular com histórico de vigência; orçamentos.
- [ ] 7.10 Estornos e cancelamentos com dupla aprovação e trilha de auditoria (já existe Strategy; adicionar alçada).

## ÉPICO 8 — Estoque, equipe e gestão (P2)

- [ ] 8.1 Estoque/almoxarifado: itens, lote, validade, entradas/saídas, inventário, estoque mínimo e alertas, baixa automática por procedimento, fornecedores, pedido de compra.
- [ ] 8.2 Equipe: cadastro de funcionários, escalas e plantões, ponto (integração), controle de CRM/COREN/registros e validade.
- [ ] 8.3 **BI e indicadores:** tempo de espera, tempo de atendimento, taxa de no-show, ocupação da agenda, faturamento por médico/convênio/procedimento, glosa, ticket médio, NPS/pesquisa de satisfação; dashboards filtráveis e exportação.
- [ ] 8.4 Qualidade e segurança do paciente: notificação de eventos adversos, checklists, protocolos clínicos, indicadores assistenciais.
- [ ] 8.5 Gestão documental: contratos, alvarás, licenças, manuais.

## ÉPICO 9 — Interoperabilidade e integrações (P2)

- [ ] 9.1 **HL7 FHIR R4:** API FHIR para Patient, Practitioner, Encounter, Appointment, Observation, Condition, AllergyIntolerance, MedicationRequest, ServiceRequest, DiagnosticReport, Immunization, DocumentReference; perfis e terminologias brasileiras (**BR Core**).
- [ ] 9.2 **RNDS (Ministério da Saúde):** autenticação, envio/consulta de dados (ex.: resultados de exames, sumário de alta, imunização), seguindo a documentação oficial atual.
- [ ] 9.3 HL7 v2 (ADT, ORM, ORU) para equipamentos/laboratórios; middleware de integração (ex.: Mirth/NextGen Connect ou camel).
- [ ] 9.4 **DICOM/PACS** e integração com RIS/modalidades (Orthanc + OHIF) para imagem.
- [ ] 9.5 Cadastros e notificações oficiais: **CNES**, **CNS/CADSUS**, notificação compulsória (SINAN), **SNGPC** (controlados), e-SUS APS/BPA para clínicas que atendem SUS.
- [ ] 9.6 API pública documentada, OAuth2 para parceiros, **webhooks** assinados, SDK/Postman, limites por cliente.
- [ ] 9.7 Integrações de mercado: WhatsApp Business, Memed, gateways de pagamento, assinatura em nuvem, ERPs contábeis.

## ÉPICO 10 — Pronto atendimento / Urgência e Emergência (P3, comece aqui antes de internação)

Fluxo real de um pronto atendimento: **chegada → recepção/ficha → classificação de risco → atendimento médico → exames/medicação → reavaliação → desfecho (alta, observação, internação, transferência, óbito)**.
- [ ] 10.1 **Recepção de urgência:** abertura rápida de ficha (inclusive paciente não identificado/"João Doe", paciente inconsciente, trazido por SAMU/polícia), senha, vínculo posterior com cadastro (merge).
- [ ] 10.2 **Classificação de risco Manchester completa:** fluxogramas por queixa e discriminadores (não só cor e sinais vitais), tempo-alvo por cor (vermelho imediato, laranja 10 min, amarelo 60 min, verde 120 min, azul 240 min; confirmar com o protocolo adotado), **alarme de estouro de tempo**, reclassificação, escalas de apoio (dor, Glasgow, NEWS/MEWS, escalas pediátricas, sepse/AVC/IAM como protocolos gatilho).
- [ ] 10.3 **Painel de fila de urgência** em tempo real (WebSocket/SSE), chamada por voz/TV, mapa de boxes/salas/macas, ocupação em tempo real.
- [ ] 10.4 **Atendimento de urgência:** ficha de atendimento (FAA/BAM), prescrição e checagem de medicação, solicitação rápida de exames/imagem com prioridade, sala de medicação, sala de observação com tempo de permanência.
- [ ] 10.5 **Desfechos:** alta com orientações, observação, internação (gera solicitação de leito), transferência (externa com regulação), evasão/recusa, óbito (declaração de óbito, fluxos legais).
- [ ] 10.6 Protocolos tempo-dependentes: dor torácica, AVC, sepse, trauma, com cronômetros e checklists.
- [ ] 10.7 Integração com SAMU/regulação (CRU/SISREG/SUSFácil) onde aplicável; notificação compulsória e violência.
- [ ] 10.8 Indicadores: tempo porta-médico, porta-alta, tempo por cor, taxa de retorno em 24/72 h, superlotação.

## ÉPICO 11 — Internação, leitos e estadia (ADT) (P3)

Fluxo: **admissão → alocação de leito → estadia (prescrição, enfermagem, exames, procedimentos) → transferências internas → alta**.
- [ ] 11.1 **Estrutura física:** unidade/andar → ala → quarto/enfermaria → leito, com tipo (enfermaria, apartamento, isolamento, UTI adulto/pediátrica/neonatal, semi-intensiva, centro obstétrico), gênero, capacidade, equipamentos, acomodação (convênio/SUS/particular).
- [ ] 11.2 **Mapa de leitos em tempo real:** status (livre, ocupado, reservado, bloqueado, em higienização, manutenção, isolamento), ocupação, previsão de alta, fila de espera por leito, regras de alocação (sexo, isolamento, idade, especialidade).
- [ ] 11.3 **ADT:** admissão (AIH/guia de internação, autorização do convênio, termo de internação), transferência interna, transferência externa, alta (administrativa, médica, a pedido, óbito, evasão), **sumário de alta** e receita de alta.
- [ ] 11.4 **Higienização e rouparia:** solicitação automática pós-alta, status de limpeza e tempo de giro de leito.
- [ ] 11.5 **Acompanhantes e visitas:** cadastro, controle de acesso e horários.
- [ ] 11.6 **Prescrição hospitalar:** itens contínuos, horários, via, diluição, "se necessário", suspensão, prescrição por protocolo, pré-checagem farmacêutica, integração com farmácia.
- [ ] 11.7 **Enfermagem:** SAE/processo de enfermagem, evolução, anotações, **sinais vitais seriados com gráficos**, balanço hídrico, curativos, dispositivos (sonda, acesso venoso) com tempo de uso, escalas de risco (queda — Morse, lesão por pressão — Braden, dor, delirium), checklist de segurança do paciente, **checagem de medicação à beira-leito** (ver Épico 13).
- [ ] 11.8 **Diárias, taxas e contas hospitalares:** lançamento automático de diária, materiais e medicamentos na conta; conta aberta com visão em tempo real.
- [ ] 11.9 Visão multiprofissional: nutrição (dietas, restrições), fisioterapia, psicologia, serviço social, fonoaudiologia, com evolução própria.
- [ ] 11.10 Passagem de plantão estruturada (SBAR) e visão de pendências por paciente.
- [ ] 11.11 CCIH (controle de infecção): notificação de infecção relacionada à assistência, precauções/isolamento, culturas e antibiograma, indicadores.
- [ ] 11.12 Indicadores: taxa de ocupação, média de permanência, giro de leito, mortalidade, reinternação, infecção.

## ÉPICO 12 — Centro Cirúrgico e Obstétrico (P3)

- [ ] 12.1 **Mapa cirúrgico:** agendamento por sala/horário/equipe (cirurgião, auxiliares, anestesista, instrumentador, circulante), duração prevista, conflitos de sala/equipe/equipamento, urgência, lateralidade, tipo de anestesia.
- [ ] 12.2 **Fluxo perioperatório:** pré-operatório (avaliação pré-anestésica, exames, jejum, consentimento cirúrgico e anestésico, reserva de sangue/UTI), **checklist de cirurgia segura (OMS)** em três momentos (antes da anestesia, antes da incisão, antes de sair da sala), transoperatório, recuperação pós-anestésica (RPA, escala de Aldrete), pós-operatório.
- [ ] 12.3 **Registros:** descrição cirúrgica, ficha anestésica (sinais vitais, drogas, tempos), contagem de compressas/instrumentais, intercorrências, peças anatomopatológicas enviadas, cronômetros (entrada, indução, incisão, fim, saída).
- [ ] 12.4 **OPME e materiais:** solicitação, cotação, autorização do convênio, rastreabilidade por lote, conferência, consumo e cobrança; kits cirúrgicos por procedimento.
- [ ] 12.5 **CME (Central de Material e Esterilização):** rastreio de instrumentais por ciclo de esterilização, indicadores biológicos/químicos, validade.
- [ ] 12.6 **Obstetrícia:** partograma, centro obstétrico, registro do recém-nascido, DNV (Declaração de Nascido Vivo), alojamento conjunto.
- [ ] 12.7 Faturamento cirúrgico: tabela de honorários, via de acesso, porte, equipe, **TISS** de internação/ honorários, taxas de sala.
- [ ] 12.8 Indicadores: tempo de giro de sala, cancelamento, atraso, infecção de sítio cirúrgico, uso da sala.

## ÉPICO 13 — Farmácia hospitalar e beira-leito (P3)

- [ ] 13.1 **Cadastro de medicamentos e materiais** (padronização, princípio ativo, DCB, classe, controlado, alto risco/"MAV", fotossensível, termolábil), lotes e validades, fornecedores, **estoques por local** (central, satélites, carrinhos de emergência, kits).
- [ ] 13.2 **Dispensação por dose unitária** ligada à prescrição (picking por paciente/horário), reposição por reposição de estoque, devolução e desperdício.
- [ ] 13.3 **Validação farmacêutica da prescrição:** interação, alergia, dose máxima, ajuste renal/hepático, duplicidade, intervenções registradas e comunicadas ao prescritor.
- [ ] 13.4 **Checagem beira-leito:** leitura de código de barras da pulseira do paciente + medicamento + dose + via + horário (os "9 certos"), registro de administração, atrasos, recusas, alerta de erro; funciona em tablet/mobile com modo de contingência.
- [ ] 13.5 **Controlados:** livro/registro eletrônico, balanço, **SNGPC**, receita retida, auditoria reforçada.
- [ ] 13.6 Rastreabilidade de lote ponta a ponta (recall) e farmacovigilância (notificação de reação adversa).
- [ ] 13.7 Manipulação/quimioterapia e nutrição parenteral (se escopo incluir), com protocolos e dupla checagem.
- [ ] 13.8 Pulseira de identificação (código de barras/QR) impressa na admissão, com dupla identificação do paciente.

## ÉPICO 14 — Apoio diagnóstico: Laboratório (LIS) e Imagem (RIS/PACS) (P3)

- [ ] 14.1 **LIS:** pedido, etiqueta com código de barras, coleta (com rastreio de quem coletou/quando), triagem de amostras, interface com analisadores (HL7/ASTM), validação técnica e biomédica de resultados, delta check, valores críticos, laudo, mapa de trabalho, controle de qualidade interno/externo, apoio (laboratório terceirizado).
- [ ] 14.2 **RIS/PACS:** agendamento por modalidade (RX, US, TC, RM, mamografia), worklist DICOM, armazenamento DICOM (Orthanc/dcm4chee), visualizador web (OHIF), laudo estruturado com modelos, assinatura digital do radiologista, comparação com exames anteriores, entrega ao paciente (link seguro).
- [ ] 14.3 Banco de sangue/hemoterapia (opcional, regulado): solicitação de hemocomponentes, rastreabilidade, reações transfusionais.
- [ ] 14.4 Anatomia patológica (opcional): macroscopia, microscopia, blocos/lâminas, laudo.

## ÉPICO 15 — Faturamento hospitalar e SUS (P3)

- [ ] 15.1 **Conta hospitalar** consolidada (diárias, taxas, materiais, medicamentos, honorários, SADT, OPME), com fechamento, auditoria interna e revisão pré-faturamento.
- [ ] 15.2 **TISS** completo para internação e SADT, **gestão de glosas** e recursos, pacotes e tabelas (Brasíndice/Simpro/CBHPM, confirmar fontes atuais).
- [ ] 15.3 **SUS:** AIH, **SIH/SUS**, BPA, APAC, SIGTAP, CNES e SIA; regras de crítica e compatibilidade de procedimentos.
- [ ] 15.4 Custos hospitalares: custo por procedimento/paciente/centro de custo e margem.
- [ ] 15.5 Auditoria de contas, autorização de procedimentos de alto custo, contestação de negativas.

## ÉPICO 16 — Qualidade, certificação e preparo para venda (P1/P2)

- [ ] 16.1 **Certificação SBIS/CFM (NGS1/NGS2)** para o prontuário eletrônico — planejar requisitos e evidências (auditoria, assinatura, rastreabilidade, segurança); sem isso muitos hospitais não compram.
- [ ] 16.2 ISO 27001 / ISO 27701 / SOC 2 (programa de segurança e privacidade) — documentar controles desde já; acreditação hospitalar (ONA/JCI) exige prontuário e rastreabilidade bons.
- [ ] 16.3 Validação de software de saúde (documentação de requisitos, rastreabilidade requisito→teste, gestão de mudanças); avaliar se algum módulo se enquadra como software como dispositivo médico (SaMD) na ANVISA (principalmente apoio à decisão clínica).
- [ ] 16.4 SLA (disponibilidade, suporte, tempo de resposta a incidentes), status page, plano de continuidade, **modo de contingência/downtime** (formulários de papel, relatórios de leitura offline do paciente em atendimento).
- [ ] 16.5 Documentação comercial/técnica: manual do usuário por perfil, guia de implantação, treinamento em vídeo, FAQ, API docs, release notes, política de suporte.
- [ ] 16.6 Ambiente de demonstração com dados fictícios e script de demo por perfil; ambiente de homologação para clientes.
- [ ] 16.7 Telemetria de uso e feedback in-app; teste de usabilidade com profissionais de saúde reais (médico, enfermeiro, recepcionista) e correção de fluxos lentos (meta: poucos cliques por tarefa frequente).

---

## ORDEM SUGERIDA DE EXECUÇÃO

1. **Épico 0** (correções P0), depois **Épico 2** itens P0 (2.1, 2.3, 2.7, 2.9, 2.10, 2.20).
2. **Épico 1** (multi-tenant) e **Épico 3** (outbox, observabilidade, testes de carga).
3. **Épicos 4, 5, 6, 7** (clínica completa vendável), incluindo assinatura ICP-Brasil e convênios/TISS.
4. **Épicos 8, 9, 16** (gestão, interoperabilidade FHIR/RNDS, certificação).
5. **Épico 10** (pronto atendimento), depois **11 a 15** (internação, centro cirúrgico, farmácia, apoio diagnóstico, faturamento hospitalar), somente se houver decisão de produto para hospital.

## ENTREGA FINAL

Gerar `docs/GAP_ANALYSIS_STATUS.md` com a tabela: item → status (feito/parcial/não feito) → arquivos/PRs → testes → riscos. Atualizar a cada épico concluído.
