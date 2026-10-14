# ADR 005: Transição para Microsserviços Reais com Spring Cloud

## Contexto
O MedFlow era originalmente documentado como "arquitetura de microsserviços", mas na prática operava como um monólito Spring Boot modular com mensageria assíncrona RabbitMQ. Para comprovar competências sênior em sistemas distribuídos, padrões de nuvem e Spring Cloud, fazia-se indispensável desacoplar responsabilidades em serviços autônomos, unificados por um Gateway com resiliência e observabilidade ponta a ponta.

## Decisão
Implementamos uma arquitetura de microsserviços real baseada no ecossistema Spring Cloud 2023.0.3 (Leyton) com Java 21:

1. **Estrutura Multi-Module Maven**:
   - `medflow-aggregator` (POM raiz): orquestrador central do ciclo de vida de compilação e testes de todos os módulos.
   - `libs/medflow-events`: biblioteca compartilhada com contratos tipados de eventos de domínio (`ConsultaEvento`).
   - `services/api-gateway`: ponto único de entrada (Porta 8080) usando Spring Cloud Gateway, com filtro global de correlação (`CorrelationIdFilter` injetando `X-Request-Id`), autenticação JWT edge (`JwtAuthGlobalFilter`) e CORS centralizado.
   - `services/notification-service`: serviço desacoplado (Porta 8082) com consumo assíncrono via RabbitMQ (`fila.notificacoes`), persistência de histórico de entregas (`NotificacaoHistorico`) e API REST paginada.
   - `backend` (MedFlow Core - Porta 8081): gestão de prontuários, agendamentos, faturamento e integrações com o core clínico.

2. **Comunicação Inter-serviços Síncrona e Resiliência**:
   - Adoção de **Spring Cloud OpenFeign** (`NotificationFeignClient`) no core para consulta sob demanda do histórico de notificações de consultas.
   - Proteção de resiliência com **Resilience4j Circuit Breaker / Fallback** (`NotificationClientFallback`), garantindo que instabilidades ou indisponibilidade no `notification-service` não degradem o atendimento clínico no backend principal.

3. **Orquestração de Containers e Observabilidade**:
   - Atualização do `docker-compose.yml` e manifestos `k8s/` para orquestrar Gateway, Core, Notification Service, PostgreSQL, RabbitMQ, Prometheus e Grafana.
   - Métricas do Actuator integradas ao Prometheus (`/actuator/prometheus`) em todos os microsserviços.

## Consequências
- **Vantagens**:
  - Arquitetura distribuída real, auditável e defensável tecnicamente em avaliações de código.
  - Tolerância a falhas via Circuit Breaker e contingência transparente ao usuário.
  - Rastreabilidade distribuída com `X-Request-Id` através de toda a malha de serviços.
  - Escalabilidade independente: o serviço de notificações pode escalar horizontalmente sob pico de disparos sem onerar o core clínico.
- **Desvantagens**:
  - Complexidade operacional adicional inerente a ambientes distribuídos, mitigada pela padronização com Docker Compose e Kubernetes.
