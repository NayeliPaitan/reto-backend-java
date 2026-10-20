package com.empresa.gateway.error;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ErrorResponseWriter writer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return writer.write(exchange, 403, "FORBIDDEN", "No tiene permisos para acceder a este recurso");
    }
}
