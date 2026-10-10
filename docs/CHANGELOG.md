# Changelog

## [Unreleased]
### Fase 3 - Camada de Serviço Limpa (Streams, Lambdas, Optional, Collections)
- Extração da regra de autorização 'paciente só acessa o que é seu' para um componente de segurança (AcessoSecurity), removendo o acoplamento do SecurityContextHolder e transferindo validações para as anotações @PreAuthorize.
- Refatoração profunda do ConsultaService, removendo as dependências de envio direto de mensagens via RabbitTemplate.
- Implementação de comunicação via Eventos de Domínio (ConsultaEvento) e publicação via ApplicationEventPublisher.
- Criação do ouvinte (DomainEventListener) executado após a confirmação da transação do banco (@TransactionalEventListener(phase = AFTER_COMMIT)), garantindo que mensagens só sejam publicadas no RabbitMQ se a transação do banco for bem sucedida.
- Ajuste e limpeza de Serviços secundários (PacienteService, ProntuarioService, PagamentoService) para adequar as assinaturas aos novos padrões.


- Fase 2 concluída: Exceções customizadas, validação e contrato de erro (RFC 7807 via ProblemDetail).
- Fase 1 concluída: Implementação de Modelo de domínio forte (OOP + Generics + enums), remoção de Magic Strings e criação de migration para tabela Especialidade.
- Fase 0 concluída: Higiene do repositório.
