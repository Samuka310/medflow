-- V9: Alinhamento de integridade e concorrência no agendamento de consultas
ALTER TABLE consulta DROP CONSTRAINT IF EXISTS uk_medico_data_hora;

CREATE UNIQUE INDEX IF NOT EXISTS uk_medico_data_hora_ativa ON consulta (medico_id, data_hora) WHERE (status != 'CANCELADA');

CREATE INDEX IF NOT EXISTS idx_consulta_medico_data ON consulta (medico_id, data_hora);
CREATE INDEX IF NOT EXISTS idx_consulta_paciente_data ON consulta (paciente_id, data_hora);

CREATE TABLE IF NOT EXISTS evento_processado (
    id UUID PRIMARY KEY,
    tipo VARCHAR(100) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
