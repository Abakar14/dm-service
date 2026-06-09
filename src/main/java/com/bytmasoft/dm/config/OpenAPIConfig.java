package com.bytmasoft.dm.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "DM Service API",
        version = "v1",
        description = """
            Document Service API.
            
            SECURITY MODEL:
            - Gateway validates JWT
            - Gateway forwards trusted identity headers
            - This service trusts requests ONLY when X-Gateway-Auth=trusted
            """,
        contact = @Contact(name = "Mahamat Abakar", email = "abakar61@web.de")
    )
)
public class OpenAPIConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            // JWT only for local dev / testing via Gateway
            .addSecuritySchemes("BearerAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT (validated by Gateway in real deployments)")
            )
            // Document forwarded headers (informational)
            .addSecuritySchemes("GatewayHeaders",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                    .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                    .name("X-Gateway-Auth")
                    .description("""
                            Forwarded by Gateway.
                            Must be 'trusted'.
                            Requests without this header are rejected.
                        """)
            )
        );
  }
}
