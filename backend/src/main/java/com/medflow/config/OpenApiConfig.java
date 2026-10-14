package com.medflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medflowOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("MedFlow API - Gestão de Clínicas Médicas")
                        .description("### Documentação da API REST do MedFlow\n\n"
                                + "Sistema corporativo de gestão clínica e agendamento de consultas com arquitetura de microsserviços Spring Cloud.\n\n"
                                + "#### Autenticação\n"
                                + "Esta API utiliza autenticação stateless via tokens **JWT (JSON Web Token)** no padrão Bearer. "
                                + "Para autenticar suas requisições no Swagger UI:\n"
                                + "1. Execute `POST /auth/login` informando `username` e `password`.\n"
                                + "2. Copie o valor do atributo `token` retornado no corpo da resposta.\n"
                                + "3. Clique no botão **Authorize** (topo direito) e cole o token no campo de texto.\n\n"
                                + "#### Formato de Erros\n"
                                + "Todas as falhas e exceções de negócio seguem a especificação **RFC 7807 (ProblemDetail)**.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Engenharia MedFlow")
                                .email("suporte@medflow.com")
                                .url("https://github.com/Samuka310/medflow"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Spring Cloud API Gateway (Porta 8080 - Padrão de Produção/Frontend)"),
                        new Server().url("http://localhost:8081").description("MedFlow Core Service Direto (Porta 8081)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT (sem o prefixo 'Bearer ')")));
    }
}
