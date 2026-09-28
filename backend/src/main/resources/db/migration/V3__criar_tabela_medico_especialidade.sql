CREATE TABLE medico_especialidade (
    medico_id UUID NOT NULL,
    especialidade_id UUID NOT NULL,
    PRIMARY KEY (medico_id, especialidade_id),
    FOREIGN KEY (medico_id) REFERENCES medico(id),
    FOREIGN KEY (especialidade_id) REFERENCES especialidade(id)
);

INSERT INTO medico_especialidade (medico_id, especialidade_id)
SELECT id, especialidade_id FROM medico WHERE especialidade_id IS NOT NULL;
