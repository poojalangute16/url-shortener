package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the OpenAPI specification metadata shown in Swagger UI.
 *
 * Swagger UI is available at: http://localhost:8080/swagger-ui.html
 * Raw OpenAPI JSON is available at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("""
                                A REST API that shortens URLs, redirects short links to their \
                                original destinations, and exposes domain-level usage metrics.
                                
                                **Key behaviours:**
                                - Shortening the same URL twice always returns the same short URL (idempotent)
                                - Short codes are 7 characters drawn from [a-zA-Z0-9]
                                - `www.` is stripped when grouping domains for metrics
                                - All data is stored in-memory; it is lost on restart
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener Team")
                                .email("team@urlshortener.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current server")
                ));
    }
}
