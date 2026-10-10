import os
import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'backend/src/main/java/com/medflow/service/'

# RecursoNaoEncontradoException
for file in os.listdir(base_dir):
    if file.endswith('.java'):
        filepath = os.path.join(base_dir, file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # We also have "orElseThrow(() -> new RuntimeException("..."))"
        content = re.sub(r'throw new RuntimeException\("([^"]*não encontrad[oa][^"]*)"\);', 
                         r'throw new com.medflow.exception.RecursoNaoEncontradoException("\1");', content)
        
        content = re.sub(r'throw new RuntimeException\("([^"]*já possui uma consulta neste horário[^"]*)"\);', 
                         r'throw new com.medflow.exception.ConflitoAgendaException("\1");', content)
        
        content = re.sub(r'throw new RuntimeException\("([^"]*Acesso negado[^"]*)"\);', 
                         r'throw new com.medflow.exception.AcessoNegadoException("\1");', content)

        content = re.sub(r'throw new RuntimeException\("([^"]*Autenticação obrigatória[^"]*)"\);', 
                         r'throw new com.medflow.exception.AcessoNegadoException("\1");', content)

        content = re.sub(r'throw new RuntimeException\("([^"]*Role.*)"\);', 
                         r'throw new com.medflow.exception.AcessoNegadoException("\1");', content)
                         
        content = re.sub(r'throw new RuntimeException\(', 
                         r'throw new com.medflow.exception.RegraDeNegocioException(', content)
                         
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

print("Exceptions substituídas com sucesso nos services.")
