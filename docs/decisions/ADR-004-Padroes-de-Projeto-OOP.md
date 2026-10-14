# ADR 004: Adoção de Padrões de Projeto (GoF) e Princípios SOLID

## Contexto
O MedFlow continha acoplamentos em regras críticas, como cancelamento de consultas baseado em comparações rígidas de tempo, repetição de algoritmos de leitura de arquivos CSV, e acoplamento a canais de notificação diretos. Para elevar a maturidade do código ao padrão de Engenheiro Fullstack/Backend Sênior, fez-se necessária a aplicação fundamentada de padrões de projeto clássicos orientados a objetos.

## Decisão
Adotamos quatro padrões principais onde havia problema arquitetural real:

1. **Strategy Pattern** para Políticas de Cancelamento e Reembolso (`PoliticaCancelamentoStrategy`), permitindo variação de percentual de reembolso e bloqueio conforme a antecedência da consulta.
2. **Template Method Pattern** para o Pipeline de Importação CSV (`AbstractCsvImportador<T>`), encapsulando o esqueleto de I/O em streaming, tratamento de cabeçalho e coleta de erros, delegando às subclasses a validação e persistência específicas de Médicos e Pacientes.
3. **Factory Method Pattern** para Resolução de Canais de Notificação (`NotificadorFactory`), desacoplando o ouvinte RabbitMQ dos canais concretos (Email, SMS, WhatsApp).
4. **Builder Pattern** para Construção de Filtros de Consulta (`FiltroConsulta.builder()`), oferecendo API fluente e validação de invariantes (ex.: data inicial posterior à final).

## Consequências
- **Vantagens**:
  - Código aderente aos princípios SOLID (SRP, OCP, LSP, ISP, DIP).
  - Facilidade de extensibilidade para novos canais ou regras de convênio.
  - Cobertura completa por testes unitários dedicados a cada estratégia e fábrica.
- **Desvantagens**:
  - Aumento na quantidade de classes e interfaces, amplamente compensado pelo isolamento de responsabilidades.
