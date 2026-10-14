package com.medflow.template.csv;

import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.dto.LinhaErroDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractCsvImportador<T> {

    /**
     * Método Template: define o esqueleto invariante do algoritmo de importação em streaming via I/O.
     */
    public final ImportacaoResultadoDTO importar(InputStream inputStream) {
        List<LinhaErroDTO> erros = new ArrayList<>();
        int totalLinhas = 0;
        int sucessos = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            boolean primeiraLinha = true;

            while ((linha = reader.readLine()) != null) {
                totalLinhas++;
                String linhaTrim = linha.trim();
                if (linhaTrim.isEmpty()) {
                    continue;
                }

                if (primeiraLinha) {
                    primeiraLinha = false;
                    if (isCabecalho(linhaTrim)) {
                        continue;
                    }
                }

                String[] colunas = separarColunas(linhaTrim);
                if (colunas.length < getQuantidadeMinimaColunas()) {
                    erros.add(new LinhaErroDTO(totalLinhas,
                            "Quantidade insuficiente de colunas (mínimo " + getQuantidadeMinimaColunas() + ").",
                            linhaTrim));
                    continue;
                }

                try {
                    validarCamposObrigatorios(colunas);
                    processarRegistro(totalLinhas, colunas);
                    sucessos++;
                } catch (Exception ex) {
                    erros.add(new LinhaErroDTO(totalLinhas, ex.getMessage(), linhaTrim));
                }
            }
        } catch (IOException e) {
            log.error("Falha de I/O ao ler arquivo CSV", e);
            throw new RuntimeException("Erro ao processar arquivo CSV: " + e.getMessage(), e);
        }

        return ImportacaoResultadoDTO.criar(totalLinhas, sucessos, erros);
    }

    protected String[] separarColunas(String linha) {
        if (linha.contains(";")) {
            return linha.split(";");
        }
        return linha.split(",");
    }

    // Métodos gancho (hook methods) do padrão Template Method
    protected abstract boolean isCabecalho(String linha);

    protected abstract int getQuantidadeMinimaColunas();

    protected abstract void validarCamposObrigatorios(String[] colunas);

    protected abstract void processarRegistro(int numeroLinha, String[] colunas) throws Exception;
}
