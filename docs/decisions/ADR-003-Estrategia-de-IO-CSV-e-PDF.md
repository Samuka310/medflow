# ADR 003: Estratégia de I/O para Processamento em Streaming de CSV e Geração de PDF

## Contexto
O MedFlow necessita de integração via arquivos para duas operações essenciais:
1. Importação em lote de profissionais de saúde e pacientes (arquivos CSV potencialmente volumosos).
2. Exportação de relatórios gerenciais/financeiros em CSV e documentos médicos legais (Receitas e Atestados) em PDF.

O processamento inadequado de I/O em memória pode esgotar a heap da JVM (`OutOfMemoryError`) caso arquivos grandes sejam carregados inteiramente em strings ou arrays de bytes sem suporte a streaming.

## Decisão

### 1. Importação CSV com Streaming e Resiliência Linha a Linha
- Utilização de `java.io.BufferedReader` com `InputStreamReader` e especificação explícita de `StandardCharsets.UTF_8` através de bloco `try-with-resources`.
- Processamento em streaming linha a linha (`reader.readLine()`), permitindo leitura de arquivos sem retenção integral do arquivo na memória.
- Relatório de erros granular (`ImportacaoResultadoDTO` contendo `LinhaErroDTO` com número da linha, motivo e conteúdo da linha rejeitada), garantindo que linhas com erros não abortem o processamento de registros válidos.

### 2. Exportação de Relatórios CSV via `StreamingResponseBody`
- Utilização da classe `StreamingResponseBody` do Spring Web, que escreve os registros diretamente no `OutputStream` da conexão HTTP em chunks.
- Adição do Byte Order Mark (BOM `\uFEFF`) no início do fluxo para compatibilidade nativa com acentuação e caracteres especiais no Microsoft Excel.

### 3. Geração de PDF com OpenPDF
- Avaliamos as alternativas de mercado:
  - **iText 7**: Requer licença comercial AGPL (restritiva para diversos ambientes corporativos).
  - **Apache PDFBox**: Mais baixo nível, exige cálculo manual de coordenadas e quebras de página.
  - **OpenPDF (`com.github.librepdf:openpdf`)**: Fork ativo, moderno e sob licença aberta LGPL/MPL, com abstrações de alto nível (`Document`, `Paragraph`, `PdfWriter`, `FontFactory`).
- **Decisão**: Adoção do **OpenPDF 2.0.3** para emissão de Receitas e Atestados Médicos com cabeçalho institucional, dados do médico (CRM), paciente (CPF), posologia/afastamento e linha de assinatura.

## Consequências
- **Vantagens**:
  - Consumo de memória constante (O(1)) na exportação de CSV via streaming.
  - Tolerância a falhas na importação, com feedback claro para o operador do sistema.
  - Documentos PDF gerados em conformidade com o padrão visual da clínica.
  - Biblioteca de PDF leve, estável e livre de restrições de licenças comerciais.
