package com.vidasalud.notify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI de la API (Swagger UI en /swagger-ui.html), con autenticación Bearer JWT. */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VidaSalud MS Notify")
                        .version("1.0.0")
                        .description("Administración de RabbitMQ y actividad de los consumidores de eventos de citas."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Access token de Microsoft Entra ID")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
