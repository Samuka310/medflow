# MedFlow — Protocolo de execução (colar ANTES do backlog)

Este protocolo vale para TODAS as tarefas do arquivo `MedFlow_Backlog_Completo_Hospital_Clinica.md`. Em caso de conflito, **este protocolo prevalece**. O objetivo é nunca empilhar um erro sobre outro.

## 1. Linha de base (obrigatório antes de qualquer mudança)

1. Crie `docs/PROGRESSO.md` (modelo no fim deste arquivo).
2. Descubra e registre nele os comandos reais do projeto: build, testes unitários, testes de integração, lint, build do frontend, testes do frontend, subir o ambiente (`docker compose up`) e o fluxo mínimo de verificação manual (smoke test).
3. Rode tudo no estado atual e registre o resultado (verde/vermelho, quantidade de testes, tempo).
4. Se a linha de base estiver vermelha: **não comece nenhum épico**. Corrija ou reporte o que está quebrado, em um commit separado e identificado.
5. Crie a tag git `baseline-antes-evolucao`.

## 2. Unidade de trabalho = ITEM, não épico

- Trabalhe **um item por vez** (ex.: 0.3, depois 0.4). Nunca inicie o item seguinte com o anterior incompleto ou vermelho.
- Se um item for grande (ex.: 1.2 Row-Level Security, 2.7 criptografia de campo, 7.2 TISS, 5.5 assinatura digital, 10.2 Manchester completo), **divida em subitens pequenos antes de começar**, escreva a divisão no `PROGRESSO.md` e execute um subitem por vez.
- Um item só começa quando seus pré-requisitos (seção 7) estão concluídos.

## 3. Ciclo obrigatório de cada item

1. **Ler** o código afetado e escrever no `PROGRESSO.md`: o que existe hoje, o que será alterado, riscos, suposições.
2. **Plano curto** (arquivos a tocar, migrations, testes a criar, como reverter).
3. **Teste primeiro quando possível:** escreva o teste que falha e comprova o problema (ex.: médico A lê prontuário de paciente do médico B), depois corrija.
4. **Implementar** só o que o item pede.
5. **Testes do item:** unitário, integração (Testcontainers com Flyway rodando as migrations reais) e teste de permissão negativa (perfil sem acesso recebe 401/403).
6. **Suíte completa** (backend + frontend). Tudo verde.
7. **Smoke test:** subir o ambiente do zero e percorrer o fluxo principal (cadastro → login → agendar → atender → pagar). Registrar o resultado.
8. **Auto-revisão do diff** (`git diff`): procurar código morto, TODO/FIXME novos, `System.out`, segredos, testes desabilitados, mudanças fora do escopo, logs com dado pessoal. Corrigir antes de commitar.
9. **Commit** (Conventional Commits), atualizar `PROGRESSO.md` e `docs/` e o OpenAPI.

## 4. Regras de parada (anti-propagação de erro)

- **Nunca** avançar com teste, build ou lint vermelho.
- **Nunca** desabilitar, pular (`@Disabled`, `skip`, `xit`), apagar ou enfraquecer um teste/asserção para "passar". Se um teste antigo falha por causa da sua mudança, explique no `PROGRESSO.md` por que o comportamento mudou e ajuste o teste com justificativa explícita, ou reverta a mudança.
- Corrigir a **causa raiz**, não o sintoma. Sem `try/catch` que engole erro, sem `@SuppressWarnings` para esconder problema.
- Após **3 tentativas** sem sucesso no mesmo problema: pare, descreva o que tentou, o erro exato, hipóteses, e aguarde orientação. Não improvise soluções arriscadas.
- Se descobrir um bug fora do item atual: **não corrija no mesmo commit**. Registre em "Descobertas" no `PROGRESSO.md` e siga.
- Se algo exigir decisão de produto, jurídica ou de negócio: pergunte, liste as opções e o trade-off. Não presuma.

## 5. Escopo fechado

- Não refatore código fora do item. Sem "aproveitar para melhorar".
- Não atualize dependências, versão do Spring Boot/Spring Cloud/Java, nem troque bibliotecas, a menos que o item peça (e então em commit isolado).
- Não renomeie/mova pacotes em massa junto com mudança de comportamento.
- Não altere contratos públicos (rotas, JSON, eventos do RabbitMQ) sem manter compatibilidade ou versionar (`/v1`, `/v2`) e documentar.

## 6. Mudanças de banco e de dados (as mais perigosas)

- Nova migration Flyway para cada mudança; **nunca** editar migration já aplicada; nunca `ddl-auto` diferente de `validate`.
- Padrão **expandir → migrar → contrair**: adicionar coluna/tabela nova (nullable ou com default), preencher dados em etapas, mudar o código para usar o novo, e só **em item posterior** remover o antigo. Nunca `DROP`/`ALTER ... NOT NULL` destrutivo no mesmo passo.
- Backfill em lotes, idempotente e reexecutável.
- Teste obrigatório de **upgrade**: subir banco na versão anterior com dados de exemplo, aplicar as novas migrations, validar integridade. Teste também banco vazio.
- Documente para cada migration arriscada o **plano de reversão** (script ou procedimento) em `docs/rollback/`.
- Antes de migração de dados sensível (criptografia de campo, multi-tenant), exigir backup verificado e ensaio em cópia.

## 7. Ordem e dependências (não pular)

- **Épico 0** vem antes de tudo. Depois, itens P0 do Épico 2.
- **Épico 1 (multi-tenant)** e **Épico 3** (outbox, observabilidade) vêm antes dos épicos funcionais 4 a 9, porque mexem em todas as tabelas e rotas; fazer depois multiplicaria o retrabalho e o risco.
- 2.3 (vínculo assistencial) antes de qualquer novo endpoint clínico.
- 5.5 (assinatura) depende de 5.8 (finalização imutável).
- 6 depende de 5 e 7 (parte de convênio) quando houver cobrança por exame.
- Épicos 10 a 15 só após os épicos 1 a 7 estarem estáveis.
- Antes de iniciar um épico, verifique no `PROGRESSO.md` que os pré-requisitos estão "concluído e verificado".

## 8. Mudanças arriscadas entram com rede de segurança

Para JWT RS256, multi-tenant/RLS, criptografia de campo, outbox, mudança de contratos de eventos e alteração do gateway:
- Use **feature flag** (`medflow.feature.*`) ou período de compatibilidade (ex.: aceitar token antigo e novo durante a transição, com data para remover).
- Entregue em passos reversíveis, cada um com testes e smoke.
- Documente como desligar/voltar em minutos.

## 9. Portão de saída de cada ÉPICO (Definition of Done)

Só declare o épico concluído se TODOS forem verdadeiros, com evidência no `PROGRESSO.md`:
- [ ] Todos os itens do épico concluídos, ou explicitamente adiados com motivo.
- [ ] Build e suíte completa verdes (backend e frontend); número de testes não diminuiu; cobertura não caiu.
- [ ] Migrations aplicam em banco vazio **e** em banco com dados da versão anterior.
- [ ] `docker compose up` do zero sobe saudável (health checks OK) e o smoke test passa.
- [ ] Testes de permissão por perfil e (após Épico 1) de isolamento entre tenants passam.
- [ ] Nenhum segredo/credencial/dado real no repositório (rodar gitleaks).
- [ ] Nenhum teste desabilitado, nenhum TODO crítico novo.
- [ ] OpenAPI e `docs/` atualizados; CHANGELOG atualizado.
- [ ] Tag git criada (`epico-N-concluido`) e merge feito só após tudo acima.

Depois do portão: **pare e entregue o relatório (seção 11). Não inicie o próximo épico sem autorização explícita.**

## 10. Memória entre sessões

Você perde contexto entre sessões. Por isso:
- No início de **toda** sessão, leia `docs/PROGRESSO.md` e `git log --oneline -20` antes de agir.
- Ao fim de cada item, atualize o `PROGRESSO.md` (estado, decisões, armadilhas, comandos, descobertas, próximos passos).
- Nunca confie em memória sobre o código: releia o arquivo antes de editar.

## 11. Relatório ao fim de cada item e de cada épico

- O que foi feito (arquivos principais) e o que **não** foi feito.
- Resultado dos testes (comandos executados e saída resumida, não apenas "passou").
- Resultado do smoke test.
- Riscos conhecidos, dívida técnica criada, descobertas fora de escopo.
- Como reverter.
- Suposições feitas e perguntas pendentes para o responsável.

## 12. Segurança do próprio processo

- Nunca commitar segredos, `.env`, dumps ou dados reais de pacientes. Usar apenas dados fictícios.
- Não executar comandos destrutivos (`rm -rf` fora de `target/` e `node_modules/`, `git push --force`, `git reset --hard`, `DROP DATABASE`) sem autorização explícita.
- Não acessar produção. Trabalhar apenas em ambiente local/dev.
- Não instalar dependências desconhecidas sem justificar; preferir versões fixas e conhecidas.

---

## MODELO de `docs/PROGRESSO.md`

```
# Progresso da evolução do MedFlow

## Comandos verificados
- Build backend: ...
- Testes backend: ...
- Frontend (build/lint/test): ...
- Subir ambiente: ...
- Smoke test (passos): ...

## Linha de base
- Data / commit / resultado dos testes: ...

## Estado dos épicos
| Épico | Item | Status (não iniciado / em andamento / concluído e verificado / adiado) | Commit/PR | Evidência |

## Decisões (ADRs resumidos)

## Descobertas fora de escopo

## Armadilhas conhecidas

## Próximo passo
```
