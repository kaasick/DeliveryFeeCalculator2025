package com.fujitsu.deliveryfeecalculator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation.
 * This sets up Swagger UI with proper information about the API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Delivery Fee Calculator API",
                version = "1.0",
                description = "API for calculating delivery fees based on city, vehicle type, and weather conditions",
                contact = @Contact(name = "Fujitsu Development Team", email = "dev@fujitsu.com"),
                license = @License(name = "Fujitsu License")
        ),
        servers = {
                @Server(url = "/", description = "Default Server URL")
        }
)
public class OpenApiConfig {

    /**
     * Configures OpenAPI components.
     * @return the OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("basicScheme",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}