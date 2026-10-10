# ADR 001: Escolha do Padrão de Mapeamento (DTO para Entidade e Entidade para DTO)

## Contexto
O projeto MedFlow precisa converter entidades em DTOs e vice-versa de forma sistemática para evitar exposição do modelo de domínio na API e facilitar o recebimento de requisições. Havia a opção de usar bibliotecas de geração de código (como MapStruct ou ModelMapper) ou implementar um padrão de mapeamento base (BaseMapper).

## Decisão
Optamos por criar uma interface genérica BaseMapper<E, D> e realizar o mapeamento de forma explícita no código.

## Consequências
- **Vantagens**: Reduz a complexidade do build (não exige anotações de processamento no Maven), código 100% explícito, fácil de entender e debugar, além de total compatibilidade com Java ecord.
- **Desvantagens**: Mais código boilerplate (manual) para escrever cada conversor de DTO.
