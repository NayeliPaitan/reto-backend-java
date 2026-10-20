package com.empresa.gateway.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ErrorResponseWriter writer;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        if (authorization == null || authorization.isBlank()) {
            return writer.write(exchange, 401, "UNAUTHORIZED", "Se requiere un token de acceso");
        }
        return writer.write(exchange, 401, "TOKEN_INVALID_OR_EXPIRED", "Token invalido o expirado");
    }
}
