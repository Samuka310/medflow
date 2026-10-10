import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

filepath = 'backend/src/main/java/com/medflow/dto/RegisterRequest.java'
replace_in_file(filepath, [
    ('@NotBlank(message = "A role', '@jakarta.validation.constraints.NotNull(message = "A role'),
    ('private String role;', 'private com.medflow.entity.enums.Role role;')
])

print("Substituições 4 realizadas.")
