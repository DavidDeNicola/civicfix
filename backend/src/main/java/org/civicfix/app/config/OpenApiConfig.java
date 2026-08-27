package org.civicfix.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Espone /swagger-ui.html e /v3/api-docs. Lo schema "bearerAuth" aggiunge il
 * pulsante "Authorize" alla UI, così le rotte protette da JWT si possono
 * provare direttamente da Swagger incollando il token ottenuto da /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CivicFix API",
                version = "v1",
                description = "API per la segnalazione e gestione di problemi civici."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
