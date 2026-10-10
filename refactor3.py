import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'backend/src/main/java/com/medflow/dto/'

replace_in_file(base_dir + 'ConsultaDTO.java', [
    ('private String status;', 'private com.medflow.entity.enums.ConsultaStatus status;')
])

replace_in_file(base_dir + 'PagamentoDTO.java', [
    ('private String status;', 'private com.medflow.entity.enums.PagamentoStatus status;'),
    ('private String formaPagamento;', 'private com.medflow.entity.enums.FormaPagamento formaPagamento;')
])

print("Substituições 3 realizadas.")
