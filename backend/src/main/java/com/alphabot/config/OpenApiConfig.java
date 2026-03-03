package com.alphabot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alphaBotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alpha-Bot API")
                        .description("AI-powered Stock Trading & Analysis Platform API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alpha-Bot Team")
                                .email("support@alphabot.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://api.alphabot.com").description("Production Server (Mocked)")));
    }
}
