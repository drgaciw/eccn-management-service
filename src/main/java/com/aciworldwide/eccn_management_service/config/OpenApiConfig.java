package com.aciworldwide.eccn_management_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI eccnOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ECCN Management Service API")
                        .description("API for managing Export Control Classification Numbers")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ACI Worldwide")
                                .email("support@aciworldwide.com")))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}