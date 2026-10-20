package com.empresa.inventory.config;

import com.empresa.inventory.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Errores 401/403 con el formato estandar de la API (timestamp, status, code, message, traceId).
 */
@Component
@RequiredArgsConstructor
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            write(request, response, 401, "UNAUTHORIZED", "Se requiere un token de acceso");
        } else {
            write(request, response, 401, "TOKEN_INVALID_OR_EXPIRED", "Token invalido o expirado");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, 403, "FORBIDDEN", "No tiene permisos para acceder a este recurso");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = "N/A";
        }
        ErrorResponse body = new ErrorResponse(Instant.now().toString(), status, code, message, traceId);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
