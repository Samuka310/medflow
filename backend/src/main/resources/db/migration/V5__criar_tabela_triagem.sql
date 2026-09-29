CREATE TABLE triagem (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id UUID NOT NULL,
    consulta_id UUID NOT NULL,
    realizado_por_id UUID NOT NULL,
    pressao_arterial VARCHAR(20),
    temperatura DECIMAL(4,2),
    peso DECIMAL(5,2),
    altura DECIMAL(3,2),
    queixa_principal TEXT,
    prioridade VARCHAR(20) NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    CONSTRAINT fk_triagem_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT fk_triagem_consulta FOREIGN KEY (consulta_id) REFERENCES consulta(id),
    CONSTRAINT fk_triagem_funcionario FOREIGN KEY (realizado_por_id) REFERENCES funcionario(id)
);
