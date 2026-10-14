# Padrões de Projeto (Design Patterns) e Princípios SOLID no MedFlow

Este documento detalha o catálogo de padrões de projeto orientados a objetos (GoF e corporativos) implementados no MedFlow, com problemas resolvidos, classes envolvidas, trechos de código, trade-offs e correlações práticas com os princípios SOLID.

---

## 1. Padrões Comportamentais (Behavioral Patterns)

### 1.1 Strategy Pattern: Políticas de Cancelamento e Reembolso
- **Problema**: O cálculo de reembolso e as regras de cancelamento de consultas médicas variavam de acordo com a antecedência do pedido. Condicionais aninhadas (`if/else`) violavam o princípio Aberto/Fechado (OCP), dificultando a adição de novas regras (ex.: convênios, feriados ou planos VIP).
- **Classes Envolvidas**:
  - [`PoliticaCancelamentoStrategy`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/strategy/cancelamento/PoliticaCancelamentoStrategy.java) (Interface Strategy)
  - [`ReembolsoIntegralStrategy`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/strategy/cancelamento/ReembolsoIntegralStrategy.java) (Cancelamento > 24h: 100% reembolso)
  - [`ReembolsoParcialTaxaStrategy`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/strategy/cancelamento/ReembolsoParcialTaxaStrategy.java) (Cancelamento entre 2h e 24h: 80% reembolso)
  - [`SemReembolsoBloqueadoStrategy`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/strategy/cancelamento/SemReembolsoBloqueadoStrategy.java) (Cancelamento < 2h: bloqueado)
  - [`PoliticaCancelamentoContext`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/strategy/cancelamento/PoliticaCancelamentoContext.java) (Contexto invocador)
  - [`ConsultaService.cancelar(id)`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/service/ConsultaService.java)
- **Trecho de Código**:
  ```java
  public BigDecimal processarCancelamento(Consulta consulta, LocalDateTime momentoCancelamento) {
      for (PoliticaCancelamentoStrategy strategy : strategies) {
          if (strategy.aplica(consulta, momentoCancelamento)) {
              return strategy.calcularPercentualReembolso(consulta, momentoCancelamento);
          }
      }
      throw new RegraDeNegocioException("Nenhuma política compatível encontrada.");
  }
  ```
- **Trade-offs**: Aumenta o número de classes no projeto, mas desacopla regras de negócio voláteis e simplifica a escrita de testes unitários isolados por regra.

---

### 1.2 Template Method Pattern: Pipeline de Importação CSV em Streaming
- **Problema**: A importação em lote de Médicos e de Pacientes via CSV compartilhava um algoritmo idêntico: abrir leitor UTF-8 com buffer, validar cabeçalho, pular linhas vazias, quebrar colunas por delimitador, validar obrigatoriedades, processar/persistir o registro e registrar falhas por linha. Duplicar essa estrutura violava o princípio DRY.
- **Classes Envolvidas**:
  - [`AbstractCsvImportador<T>`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/template/csv/AbstractCsvImportador.java) (Classe base abstrata com o método template final)
  - [`MedicoCsvImportador`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/template/csv/MedicoCsvImportador.java) (Implementação concreta para médicos)
  - [`PacienteCsvImportador`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/template/csv/PacienteCsvImportador.java) (Implementação concreta para pacientes)
- **Trecho de Código**:
  ```java
  public final ImportacaoResultadoDTO importar(InputStream inputStream) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
          // Esqueleto invariante da iteração e tratamento de erros
          validarCamposObrigatorios(colunas); // Gancho (hook)
          processarRegistro(totalLinhas, colunas); // Gancho (hook)
      }
      return ImportacaoResultadoDTO.criar(totalLinhas, sucessos, erros);
  }
  ```
- **Trade-offs**: Exige herança clássica, mas garante reutilização uniforme de I/O e relatórios sem redundância de código.

---

### 1.3 Observer / Domain Event Pattern: Notificações e Auditoria Pós-Commit
- **Problema**: O envio direto para filas do RabbitMQ ou tabelas de auditoria dentro do serviço de consulta causava acoplamento indevido e disparava mensagens de transações que eventualmente sofriam rollback no banco.
- **Classes Envolvidas**:
  - [`ConsultaEvento`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/event/ConsultaEvento.java) (Evento de domínio)
  - `ApplicationEventPublisher` (Spring Framework)
  - [`DomainEventListener`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/event/DomainEventListener.java) (Subscriber com `@TransactionalEventListener(phase = AFTER_COMMIT)`)
- **Trade-offs**: A publicação torna-se assíncrona ao fluxo central de persistência; exige garantia de idempotência no receptor caso o listener necessite de reenvio.

---

### 1.4 State Pattern: Ciclo de Vida da Consulta Médica
- **Problema**: Múltiplas verificações condicionais de strings mágicas (`if ("AGENDADA".equals(status))`) espalhadas pelos serviços permitiam transições de estado inválidas (ex: realizar consulta já cancelada).
- **Classes Envolvidas**:
  - [`ConsultaStatus`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/entity/enums/ConsultaStatus.java) (Enum com máquina de estados encapsulada)
  - [`Consulta.confirmar()`, `Consulta.cancelar()`, etc.](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/entity/Consulta.java)
- **Trecho de Código**:
  ```java
  public boolean podeTransitarPara(ConsultaStatus novo) {
      return switch (this) {
          case AGENDADA -> novo == CONFIRMADA || novo == CANCELADA || novo == AGUARDANDO_TRIAGEM;
          case CONFIRMADA -> novo == AGUARDANDO_TRIAGEM || novo == CANCELADA;
          case AGUARDANDO_TRIAGEM -> novo == EM_ATENDIMENTO || novo == CANCELADA;
          case EM_ATENDIMENTO -> novo == REALIZADA;
          case REALIZADA, CANCELADA -> false;
      };
  }
  ```

---

## 2. Padrões Criacionais (Creational Patterns)

### 2.1 Factory / Factory Method: Resolução Dinâmica de Canais de Notificação
- **Problema**: O sistema deve despachar notificações para diferentes canais (E-mail, SMS, WhatsApp) sem que o cliente consumidor (`NotificacaoListener`) precise conhecer as classes concretas de cada canal.
- **Classes Envolvidas**:
  - [`NotificadorCanal`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/factory/notificacao/NotificadorCanal.java) (Interface do produto)
  - [`EmailNotificador`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/factory/notificacao/EmailNotificador.java), [`SmsNotificador`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/factory/notificacao/SmsNotificador.java), [`WhatsAppNotificador`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/factory/notificacao/WhatsAppNotificador.java) (Produtos concretos)
  - [`NotificadorFactory`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/factory/notificacao/NotificadorFactory.java) (Fábrica que resolve canais via injeção de dependência)
- **Trecho de Código**:
  ```java
  public NotificadorCanal obterNotificador(TipoCanalNotificacao canal) {
      NotificadorCanal notificador = notificadores.get(canal);
      if (notificador == null) {
          throw new IllegalArgumentException("Canal de notificação não suportado: " + canal);
      }
      return notificador;
  }
  ```

---

### 2.2 Builder Pattern: Construção Fluente de Filtros de Consulta
- **Problema**: Montar critérios de busca de consultas por múltiplos parâmetros opcionais (médico, paciente, status, intervalo de datas) gerava construtores com muitos parâmetros (`telescoping constructor`) ou mutabilidade indesejada.
- **Classes Envolvidas**:
  - [`FiltroConsulta`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/builder/FiltroConsulta.java)
- **Trecho de Código**:
  ```java
  FiltroConsulta filtro = FiltroConsulta.builder()
          .comMedico(medicoId)
          .comStatus(ConsultaStatus.AGENDADA)
          .noPeriodo(inicio, fim)
          .build();
  ```

---

## 3. Padrões Estruturais e Corporativos (Enterprise Patterns)

- **Repository Pattern**: [`ConsultaRepository`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/repository/ConsultaRepository.java), [`MedicoRepository`](file:///c:/Users/Pax%20Primavera/Desktop/MedFlow/medflow/backend/src/main/java/com/medflow/repository/MedicoRepository.java), etc., encapsulando o acesso a dados e isolando consultas SQL/JPQL.
- **Data Transfer Object (DTO)**: Isolamento das entidades JPA da API pública (ex.: `ConsultaDTO`, `ImportacaoResultadoDTO`).
- **Value Object**: Records imutáveis sem identidade própria representando conceitos do domínio (ex.: `Dinheiro`, `Periodo`).

---

## 4. Aplicação dos Princípios SOLID

| Princípio | Descrição | Exemplo Concreto no MedFlow |
| :--- | :--- | :--- |
| **S** - *Single Responsibility Principle* (SRP) | Uma classe deve ter um e apenas um motivo para mudar. | `ConsultaService` cuida exclusivamente das regras de negócio de agendamento; autorização foi extraída para `AcessoSecurity`, persistência de arquivo para `DocumentoPdfService` e mensageria para `DomainEventListener`. |
| **O** - *Open/Closed Principle* (OCP) | Entidades de software devem estar abertas para extensão, mas fechadas para modificação. | Para adicionar uma nova política de cancelamento (ex.: convênio empresarial), basta criar uma nova classe que implementa `PoliticaCancelamentoStrategy`, sem alterar `ConsultaService` ou `PoliticaCancelamentoContext`. |
| **L** - *Liskov Substitution Principle* (LSP) | Subtipos devem ser substituíveis pelos seus tipos base sem quebrar o sistema. | Qualquer subtipo de `AbstractCsvImportador<T>` (`MedicoCsvImportador`, `PacienteCsvImportador`) pode ser executado uniformemente pelo método template `importar(...)`. |
| **I** - *Interface Segregation Principle* (ISP) | Clientes não devem ser forçados a depender de interfaces que não utilizam. | Interfaces específicas e focadas (`NotificadorCanal`, `PoliticaCancelamentoStrategy`) com responsabilidade granular, ao invés de interfaces monolíticas. |
| **D** - *Dependency Inversion Principle* (DIP) | Módulos de alto nível não devem depender de módulos de baixo nível; ambos devem depender de abstrações. | `ConsultaService` depende da interface `PoliticaCancelamentoStrategy` (via contexto) e de `ApplicationEventPublisher`, nunca de implementações concretas de mensageria ou cálculo. |
