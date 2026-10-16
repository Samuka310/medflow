-- ============================================================================
-- V11: Índices de otimização para buscas por usuário e agregações de faturamento
-- ============================================================================

-- Índices para navegação O(1) de entidades especializadas a partir do Usuário autenticado
CREATE INDEX IF NOT EXISTS idx_medico_usuario_id ON medico (usuario_id);
CREATE INDEX IF NOT EXISTS idx_funcionario_usuario_id ON funcionario (usuario_id);

-- Índices para agregação rápida por período e status sem table scan
CREATE INDEX IF NOT EXISTS idx_consulta_data_hora ON consulta (data_hora);
CREATE INDEX IF NOT EXISTS idx_pagamento_status ON pagamento (status);
