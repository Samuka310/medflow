CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE especialidade (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT
);

CREATE TABLE medico (
    id UUID PRIMARY KEY,
    crm VARCHAR(50) NOT NULL UNIQUE,
    especialidade_id UUID NOT NULL,
    usuario_id UUID NOT NULL UNIQUE,
    FOREIGN KEY (especialidade_id) REFERENCES especialidade(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE paciente (
    id UUID PRIMARY KEY,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    data_nascimento DATE NOT NULL,
    usuario_id UUID NOT NULL UNIQUE,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE consulta (
    id UUID PRIMARY KEY,
    medico_id UUID NOT NULL,
    paciente_id UUID NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    FOREIGN KEY (medico_id) REFERENCES medico(id),
    FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT uk_medico_data_hora UNIQUE (medico_id, data_hora)
);

CREATE TABLE pagamento (
    id UUID PRIMARY KEY,
    consulta_id UUID NOT NULL UNIQUE,
    valor DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    forma_pagamento VARCHAR(50) NOT NULL,
    FOREIGN KEY (consulta_id) REFERENCES consulta(id)
);

CREATE TABLE prontuario (
    id UUID PRIMARY KEY,
    consulta_id UUID NOT NULL UNIQUE,
    observacoes TEXT,
    FOREIGN KEY (consulta_id) REFERENCES consulta(id)
);

CREATE TABLE exame (
    id UUID PRIMARY KEY,
    prontuario_id UUID NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    url_arquivo VARCHAR(255) NOT NULL,
    FOREIGN KEY (prontuario_id) REFERENCES prontuario(id)
);
