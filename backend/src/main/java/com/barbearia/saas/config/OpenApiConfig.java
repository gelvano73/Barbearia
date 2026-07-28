package com.barbearia.saas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuração do SpringDoc/OpenAPI (Swagger) da API REST. */
@Configuration
public class OpenApiConfig {

    /** Configura o documento OpenAPI/Swagger da API. */
    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Barbearia SaaS API")
                        .description("API multi-tenant para gestão de barbearias: clientes, barbeiros e agendamentos.")
                        .version("1.0.0")
                        .contact(new Contact().name("Barbearia SaaS").email("suporte@barbeariasas.com")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
