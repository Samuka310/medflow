package com.medflow.service;

import com.medflow.dto.ImportacaoResultadoDTO;
import com.medflow.template.csv.MedicoCsvImportador;
import com.medflow.template.csv.PacienteCsvImportador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ImportacaoCsvService {

    private final MedicoCsvImportador medicoCsvImportador;
    private final PacienteCsvImportador pacienteCsvImportador;

    @Transactional
    public ImportacaoResultadoDTO importarMedicosCsv(InputStream inputStream) {
        return medicoCsvImportador.importar(inputStream);
    }

    @Transactional
    public ImportacaoResultadoDTO importarPacientesCsv(InputStream inputStream) {
        return pacienteCsvImportador.importar(inputStream);
    }
}
