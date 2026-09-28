CREATE TABLE funcionario (
    id UUID PRIMARY KEY,
    cargo VARCHAR(50) NOT NULL,
    data_admissao DATE,
    usuario_id UUID NOT NULL UNIQUE,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
