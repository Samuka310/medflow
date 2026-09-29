CREATE TABLE receita (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id UUID NOT NULL REFERENCES paciente(id),
    medico_id UUID NOT NULL REFERENCES medico(id),
    consulta_id UUID REFERENCES consulta(id),
    medicamento VARCHAR(255) NOT NULL,
    dosagem VARCHAR(255) NOT NULL,
    validade_data DATE NOT NULL,
    data_emissao TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE atestado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id UUID NOT NULL REFERENCES paciente(id),
    medico_id UUID NOT NULL REFERENCES medico(id),
    consulta_id UUID REFERENCES consulta(id),
    dias_afastamento INT NOT NULL,
    cid_opcional VARCHAR(20),
    data_emissao TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE prontuario
ADD COLUMN diagnostico_cid VARCHAR(20),
ADD COLUMN evolucao_clinica TEXT,
ADD COLUMN conduta_medica TEXT;
