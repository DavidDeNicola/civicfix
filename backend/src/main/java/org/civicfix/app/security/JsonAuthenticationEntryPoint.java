package org.civicfix.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Senza questo bean, Spring Security risponde a ogni richiesta non
 * autenticata con il suo {@code Http403ForbiddenEntryPoint} predefinito: un
 * 403 vuoto, indistinguibile da un 403 "sei autenticato ma non autorizzato"
 * (ruolo insufficiente, autore diverso). Il frontend non può reagire in modo
 * mirato a un token mancante o scaduto se il codice HTTP è lo stesso di un
 * divieto legittimo. Qui si risponde invece con 401 in JSON, nello stesso
 * formato di {@link org.civicfix.app.exception.GlobalExceptionHandler}.
 *
 * <p>Il JSON è scritto a mano invece di passare da Jackson: questo modulo
 * usa {@code spring-boot-starter-webmvc}, che qui non porta Jackson in
 * dipendenza transitiva come farebbe {@code spring-boot-starter-web}.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String messaggio = "Accesso non autorizzato: effettua di nuovo l'accesso.";
        String corpo = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s","fieldErrors":null}""".formatted(
                LocalDateTime.now(), messaggio);

        response.getWriter().write(corpo);
    }
}
