-- V12: Otimização de consultas de auditoria e conformidade (LGPD / CFM)
CREATE INDEX IF NOT EXISTS idx_auditoria_data_hora ON auditoria(data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidade_id ON auditoria(entidade, entidade_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario_id ON auditoria(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_acao ON auditoria(acao);
