package com.contacts.agenda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contactsAgendaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Contacts Agenda API")
                        .description("""
                                A resilient contact management API that fetches contacts from an external service
                                with built-in fault tolerance mechanisms.
                                """)
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}

