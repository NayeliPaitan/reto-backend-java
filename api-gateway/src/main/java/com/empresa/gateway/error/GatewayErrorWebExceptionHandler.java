package com.empresa.gateway.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ErrorResponseWriter writer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode().value();
        }

        String code;
        String message;
        switch (status) {
            case 404 -> { code = "ROUTE_NOT_FOUND"; message = "Ruta no encontrada"; }
            case 405 -> { code = "METHOD_NOT_ALLOWED"; message = "Metodo HTTP no permitido"; }
            case 502, 503 -> { code = "SERVICE_UNAVAILABLE"; message = "Servicio no disponible"; }
            case 504 -> { code = "GATEWAY_TIMEOUT"; message = "Tiempo de espera agotado"; }
            default -> {
                if (status >= 500) {
                    log.error("Error interno en el Gateway", ex);
                    code = "INTERNAL_ERROR";
                    message = "Error interno del servidor";
                } else {
                    code = "INVALID_REQUEST";
                    message = "Solicitud invalida";
                }
            }
        }
        return writer.write(exchange, status, code, message);
    }
}
