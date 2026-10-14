# Evidências e Aplicação Prática de Java Core no MedFlow

Este documento mapeia e detalha como os conceitos fundamentais e avançados de **Java Core (Java 21)** foram implementados na arquitetura do MedFlow, demonstrando proficiência exigida para atuação como Engenheiro de Software Fullstack / Backend Sênior.

---

## 1. Java I/O e Streams de Dados (`java.io` e `java.nio.file`)

### A. Leitura em Streaming com `BufferedReader` e `StandardCharsets`
- **Onde encontrar**: `com.medflow.service.ImportacaoCsvService`
- **Padrão Utilizado**: `try-with-resources` garantindo o fechamento determinístico de descritores de arquivo/streams mesmo em caso de falha de parsing.
- **Implementação**:
  ```java
  try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String linha;
      while ((linha = reader.readLine()) != null) {
          // Processamento linha a linha em O(1) de memória
      }
  }
  ```
- **Resiliência e Tolerância a Falhas**: Validação granular linha a linha com relatório de inconsistências (`LinhaErroDTO`), garantindo que linhas corrompidas não abortem os registros válidos do lote.

### B. Exportação HTTP em Streaming com `StreamingResponseBody`
- **Onde encontrar**: `com.medflow.service.RelatorioExportService` e controladores `ConsultaController`, `FaturamentoController`.
- **Implementação**: Escrita em chunks diretamente no `OutputStream` da conexão HTTP utilizando `BufferedWriter` e injeção do caractere BOM (`\uFEFF`) para compatibilidade internacional com o Microsoft Excel.

### C. Geração e Manipulação de Binários (PDF)
- **Onde encontrar**: `com.medflow.service.DocumentoPdfService`
- **Implementação**: Construção programática de documentos PDF (Receituários e Atestados) utilizando OpenPDF, manipulando streams binários em memória (`ByteArrayOutputStream`) e retornando `byte[]` com cabeçalhos MIME corretos (`application/pdf`).

---

## 2. Concorrência e Programação Multithread (`java.util.concurrent`)

### A. Simulação e Teste de Concorrência
- **Onde encontrar**: `com.medflow.service.ConsultaConcurrencyTest`
- **Mecanismos Utilizados**:
  - `ExecutorService` e `Executors.newFixedThreadPool(10)`: Pool de threads simulando requisições paralelas simultâneas.
  - `CountDownLatch(1)` e `CountDownLatch(totalThreads)`: Bloqueio de partida sincronizado (*barrier*) para liberar todas as threads exatamente no mesmo instante de tempo.
  - `AtomicInteger` e `AtomicBoolean`: Operações atômicas de comparação e troca (`compareAndSet`) livres de travas manuais (*lock-free*).

### B. Deduplicação Concorrente em Memória (Idempotência)
- **Onde encontrar**: `com.medflow.service.NotificacaoListener`
- **Mecanismo**: `ConcurrentHashMap.newKeySet()` operando como cache L1 concorrente para descarte imediato de eventos duplicados sem contenção de threads.

---

## 3. Collections Framework e Generics Avançados

### A. Generics Reutilizáveis
- **Onde encontrar**: `com.medflow.dto.PageResponse<T>` e `com.medflow.dto.ApiResult<T>`
- **Conceito**: Estruturas imutáveis genéricas com tipagem forte para envelopar listagens paginadas e retornos padronizados da API.

### B. Collections e Ordenações Customizadas
- **Onde encontrar**: `ConsultaService.agendaHoje(email)`
- **Implementação**: Ordenação por pesos de prioridade de triagem e data/hora com `List.sort` e `Comparator` customizado:
  ```java
  consultas.sort((c1, c2) -> {
      int p1 = getPriorityWeight(c1.getId());
      int p2 = getPriorityWeight(c2.getId());
      if (p1 != p2) {
          return Integer.compare(p2, p1); // Prioridade descendente
      }
      return c1.getDataHora().compareTo(c2.getDataHora());
  });
  ```

---

## 4. Lambdas e Stream API

- **Mapeamento de Entidades para DTOs**: Utilização extensiva de `.stream().map(...).collect(Collectors.toList())` ou `.toList()`.
- **Filtragem e Predicados**: `.filter(...)`, `.anyMatch(...)`, `.findFirst()`.
- **Optional Idiomático**: Substituição de verificações de `null` por `.map(...)`, `.orElseThrow(...)` e `.ifPresent(...)`.

---

## 5. Hierarquia de Exceções e Tratamento de Erros

- **Hierarquia Selada (`sealed / permits`)**:
  - `MedflowException` (abstrata selada)
    - `RecursoNaoEncontradoException` (HTTP 404)
    - `RegraDeNegocioException` (HTTP 422)
    - `ConflitoAgendaException` (HTTP 409)
    - `AcessoNegadoException` (HTTP 403)
- **RFC 7807 (`ProblemDetail`)**: Centralização no `GlobalExceptionHandler` sem vazamento de stacktraces ou mensagens internas de infraestrutura para o cliente.

---

## 6. Recursos Modernos do Java 21

- **Records Imutáveis**: Uso extensivo de `record` para DTOs, Eventos de Domínio (`ConsultaEvento`), propriedades de configuração (`ConsultaProperties`) e Value Objects (`Dinheiro`, `Periodo`).
- **Switch Expressions e Pattern Matching**: Avaliação de enums e prioridades de triagem (`switch (prioridade) { case EMERGENCIA -> 4; ... }`).
- **Text Blocks (`"""`)**: Utilizado em consultas JPQL multilinhas legíveis em repositórios Spring Data.
