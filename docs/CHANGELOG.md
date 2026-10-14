# Changelog

## [Unreleased]
### Fase 5 - I/O e Integrações de Arquivo (Java I/O)
- **Importação em Lote via CSV**:
  - Implementação do serviço `ImportacaoCsvService` utilizando `BufferedReader`, `InputStreamReader` e `StandardCharsets.UTF_8` com `try-with-resources`.
  - Processamento em streaming linha a linha com validação de dados cadastrais (CRM, CPF, formato de datas, e-mail) e relatório granular de erros (`ImportacaoResultadoDTO` contendo `LinhaErroDTO`).
  - Endpoints multipart: `POST /api/medicos/importar-csv` e `POST /api/pacientes/importar-csv`.
- **Exportação de Relatórios CSV em Streaming**:
  - Implementação do serviço `RelatorioExportService` utilizando `StreamingResponseBody` para envio dos dados em chunks diretamente na resposta HTTP sem sobrecarregar a memória heap.
  - Inclusão do Byte Order Mark (BOM `\uFEFF`) para compatibilidade nativa com acentuação no Microsoft Excel.
  - Endpoints: `GET /api/consultas/relatorio-csv` e `GET /api/faturamento/relatorio-csv`.
- **Geração de Documentos Médicos em PDF**:
  - Integração da biblioteca OpenPDF 2.0.3 para emissão profissional de documentos com cabeçalho institucional, dados do médico/paciente e linha de assinatura.
  - Implementação do serviço `DocumentoPdfService`.
  - Endpoints: `GET /api/pacientes/receitas/{id}/pdf` e `GET /api/pacientes/atestados/{id}/pdf`.
- **Amostras e Documentação**:
  - Criação de arquivos de exemplo em `docs/samples/` (`medicos_valido.csv`, `pacientes_valido.csv`, `medicos_malformado.csv`, `pacientes_malformado.csv`).
  - Registro da decisão de arquitetura em `docs/decisions/ADR-003-Estrategia-de-IO-CSV-e-PDF.md`.
  - Criação do documento `docs/JAVA_CORE.md` mapeando todas as evidências de Java Core (I/O, Concorrência, Streams, Generics, Records, Exceções).
  - Testes unitários cobrindo todos os fluxos válidos e malformados (`ImportacaoCsvServiceTest`, `DocumentoPdfServiceTest`, `RelatorioExportServiceTest`).

### Fase 4 - Concorrência e Integridade do Agendamento
- Alinhamento da regra de negócio com o banco de dados (Duração dinâmica de consultas via `ConsultaProperties` e índice único parcial no PostgreSQL `uk_medico_data_hora_ativa`).
- Bloqueio pessimista (`Pessimistic Write Lock`) no médico para serializar agendamentos simultâneos e eliminar *race conditions*.
- Tratamento de `DataIntegrityViolationException` no `GlobalExceptionHandler` convertendo em `409 Conflict`.
- Idempotência no consumo de mensagens assíncronas no `NotificacaoListener` com cache L1 e persistência L2.
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
