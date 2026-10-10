import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'backend/src/main/java/com/medflow/service/'

# ConsultaService specific updates for FormaPagamento and PrioridadeTriagem
replace_in_file(base_dir + 'ConsultaService.java', [
    ('pagamento.setFormaPagamento("CARTAO");', 'pagamento.setFormaPagamento(com.medflow.entity.enums.FormaPagamento.CARTAO);'),
    ('case "EMERGENCIA"', 'case EMERGENCIA'),
    ('case "ALTA"', 'case ALTA'),
    ('case "MEDIA"', 'case MEDIA'),
    ('case "BAIXA"', 'case BAIXA')
])

# TriagemService updates
replace_in_file(base_dir + 'TriagemService.java', [
    ('dto.getPrioridade() != null ? dto.getPrioridade() : "BAIXA"', 'dto.getPrioridade() != null ? dto.getPrioridade() : com.medflow.entity.enums.PrioridadeTriagem.BAIXA')
])

print("Substituições 2 realizadas com sucesso.")
