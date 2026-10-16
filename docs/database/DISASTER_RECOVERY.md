# MedFlow — Plano de Continuidade, Alta Disponibilidade e Recuperação de Desastres (DR / PITR)

Este documento estabelece a política operacional, topologia de Alta Disponibilidade (HA) e procedimentos de Recuperação em Ponto no Tempo (**Point-in-Time Recovery - PITR**) do banco de dados do **MedFlow**, em estrita conformidade com a **LGPD (Art. 46)**, as normas do **CFM** e os padrões de acreditação hospitalar (**ONA / HIMSS**).

---

## 1. Métricas e Metas de Resiliência (SLAs)

| Métrica | Meta Operacional | Descrição |
| :--- | :--- | :--- |
| **RPO (Recovery Point Objective)** | **0 segundos** (failover de réplica síncrona)<br>**< 1 minuto** (desastre catastrófico com perda total do cluster) | Perda máxima tolerada de transações médicas e financeiras. |
| **RTO (Recovery Time Objective)** | **< 30 segundos** (failover automático dentro do cluster)<br>**< 15 minutos** (provisionamento e restauração completa de novo cluster) | Tempo máximo para restabelecimento total da disponibilidade dos serviços clínicos. |
| **Disponibilidade Alvo (SLO)** | **99.95%** (High Availability) | Menos de 4.38 horas de indisponibilidade não programada por ano. |

---

## 2. Topologia de Alta Disponibilidade (CloudNativePG)

Em conformidade com a auditoria interna (Item 0.8 do Backlog Comercial), o PostgreSQL não deve ser executado em produção como StatefulSet de réplica única. O cluster de produção é orquestrado pelo operador **CloudNativePG** (`k8s/postgres-ha-cluster.yaml`):

```
                                  [ Clientes / Backend MedFlow ]
                                                │
                                                ▼
                          ┌───────────────────────────────────────────┐
                          │    PgBouncer Pooler (2 pods redundantes)   │
                          │        modo: TRANSACTION, porta: 5432      │
                          └─────────────────────┬─────────────────────┘
                                                │
                     ┌──────────────────────────┴──────────────────────────┐
                     │                                                     │
                     ▼ (Leitura/Escrita)                                   ▼ (Streaming Replication)
      ┌─────────────────────────────┐                       ┌─────────────────────────────┐
      │     Instância 1 (Líder)     │◄═════════════════════►│    Instância 2 (Standby)    │
      │  Zona: us-east-1a (Node 1)  │  Replicação Síncrona  │  Zona: us-east-1b (Node 2)  │
      └──────────────┬──────────────┘                       └─────────────────────────────┘
                     │
                     │ (Streaming Replication Assíncrona)
                     ▼
      ┌─────────────────────────────┐
      │    Instância 3 (Standby)    │
      │  Zona: us-east-1c (Node 3)  │
      └──────────────┬──────────────┘
                     │
                     ▼ (Continuous WAL Archiving & Backups Físicos)
      ┌─────────────────────────────────────────────────────────────┐
      │  Object Store S3 / MinIO (Bucket: s3://medflow-backups/)    │
      │   - Backups diários completos (02:00 UTC)                   │
      │   - Arquivamento contínuo de WALs com compressão Gzip       │
      │   - Criptografia em repouso AES-256                         │
      │   - Retenção: 30 dias                                       │
      └─────────────────────────────────────────────────────────────┘
```

### Características do Cluster HA:
1. **Quorum com 3 Instâncias:** Evita o problema de split-brain (cérebro bipartido) e garante eleições consistentes de novo líder por consenso RAFT em até 10 a 30 segundos.
2. **Anti-Afinidade de Pods (`PodAntiAffinity`):** Distribui as instâncias em diferentes nós físicos e zonas de disponibilidade (`topology.kubernetes.io/zone`).
3. **Pool de Conexões PgBouncer (`k8s/postgres-pgbouncer.yaml`):** Absorve picos de conexões concorrentes gerados pelo auto-scaling (HPA) do backend, operando em modo `transaction` e protegendo o kernel do banco.
4. **Dimensionamento HikariCP:** O pool de conexões da aplicação (`application-prod.yml`) é calibrado com `maximum-pool-size: 20`, `leak-detection-threshold: 5000ms` e compatibilidade JDBC via `prepareThreshold=0`.

---

## 3. Procedimento de Failover Automático e Manual

### Failover Automático:
Quando a instância primária sofre pane de hardware ou perda de rede:
1. O operador CloudNativePG detecta a falha através dos probes e do heartbeat.
2. A réplica síncrona com o menor LSN (Log Sequence Number) é promovida automaticamente a nova primária.
3. O Service interno `medflow-db-ha-rw` e os pods do PgBouncer redirecionam o tráfego de escrita imediatamente para o novo líder.
4. Nenhuma intervenção humana é necessária; o tempo total de transição é tipicamente de **15 a 30 segundos**.

### Switchover Manual (Manutenção Programada):
Para drenar um nó ou realizar upgrade sem indisponibilidade:
```bash
# Executa switchover gracioso para a réplica mais atualizada
kubectl cnpg promote medflow-db-ha --instance medflow-db-ha-2
```

---

## 4. Recuperação em Ponto no Tempo (Point-in-Time Recovery - PITR)

O PITR permite restaurar o banco de dados exatamente no estado em que se encontrava em um segundo específico antes de um evento catastrófico (exemplo: script com `DROP TABLE` acidental ou corrupção de migração executada às `2026-10-16 14:45:00 UTC`).

### Procedimento Passo a Passo:

#### Passo 1: Determinar o Timestamp Alvo
Identifique o momento exato do incidente através dos logs estruturados da auditoria (`medflow.auditoria.gravadas`) ou do log do Postgres. Suponha que o incidente ocorreu às `2026-10-16 14:45:00 UTC`. O alvo de recuperação será `2026-10-16 14:44:30.000000+00` (30 segundos antes do evento).

#### Passo 2: Criar o Manifesto de Restauração (`restore-cluster.yaml`)
Crie um novo cluster isolado apontando para o backup do cluster principal com o `targetTime`:

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: medflow-db-restored
  namespace: default
spec:
  instances: 3
  storage:
    size: 20Gi
  bootstrap:
    recovery:
      source: medflow-db-ha
      recoveryTarget:
        targetTime: "2026-10-16 14:44:30.000000+00"
  backup:
    barmanObjectStore:
      destinationPath: "s3://medflow-backups/postgres-wal/"
      endpointURL: "https://s3.amazonaws.com"
      s3Credentials:
        accessKeyId:
          name: medflow-secrets
          key: s3-access-key
        secretAccessKey:
          name: medflow-secrets
          key: s3-secret-key
```

#### Passo 3: Aplicar e Acompanhar a Restauração
```bash
kubectl apply -f restore-cluster.yaml
kubectl cnpg status medflow-db-restored --watch
```
O operador CloudNativePG:
1. Baixará o backup físico mais recente anterior ao timestamp alvo.
2. Aplicará sequencialmente os arquivos WAL até o instante `14:44:30 UTC`.
3. Promoverá a instância e inicializará as réplicas em streaming.

#### Passo 4: Validar os Dados e Chavear Tráfego
Após verificar a integridade dos dados clínicos e o checklist de migrações (`flyway_schema_history`):
```bash
# Apontar o Service de produção ou ConfigMap para o novo cluster restaurado
kubectl patch configmap medflow-config -p '{"data":{"SPRING_DATASOURCE_URL":"jdbc:postgresql://medflow-db-restored-rw:5432/medflow?prepareThreshold=0"}}'
kubectl rollout restart deployment/medflow-backend
```

---

## 5. Simulação e Teste de Restauração Automatizado

Para garantir a prontidão do plano de recuperação de desastres, é obrigatório executar o script de teste de restauração (`scripts/test-db-restore.sh` / `scripts/test-db-restore.ps1`) antes de novos releases em produção e mensalmente em auditorias de conformidade.

O script executa de forma autônoma:
1. Extração de dump lógico e metadados de migrações Flyway.
2. Criação de banco de teste temporário em ambiente isolado.
3. Simulação de restauração completa de schema e dados.
4. Verificação de integridade referencial, contagem de registros e soma hash das tabelas críticas (`paciente`, `prontuario`, `consulta`, `auditoria`).
5. Emissão de relatório de sucesso com carimbo de data/hora para auditoria.
