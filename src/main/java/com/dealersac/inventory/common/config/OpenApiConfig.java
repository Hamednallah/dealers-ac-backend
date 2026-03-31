package com.dealersac.inventory.common.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dealers AC — Inventory API")
                        .description("""
                            Multi-tenant Dealer & Vehicle Inventory Module.
                            
                            **Authentication:** Pass `Authorization: Bearer <JWT>` header.
                            **Multi-tenancy:** Pass `X-Tenant-Id: <tenantId>` header on all protected routes.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ahmed Abdulafiz")
                                .email("ahmed.abdulafiz@example.com")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
