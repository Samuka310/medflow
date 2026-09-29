CREATE TABLE auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    usuario_email VARCHAR(255) NOT NULL,
    acao VARCHAR(255) NOT NULL,
    entidade VARCHAR(255) NOT NULL,
    entidade_id UUID,
    detalhes TEXT,
    data_hora TIMESTAMP NOT NULL DEFAULT NOW()
);
