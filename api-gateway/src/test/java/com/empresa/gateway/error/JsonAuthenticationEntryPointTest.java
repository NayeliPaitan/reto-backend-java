package com.empresa.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAuthenticationEntryPointTest {

    private final JsonAuthenticationEntryPoint entryPoint =
            new JsonAuthenticationEntryPoint(new ErrorResponseWriter(new ObjectMapper()));

    @Test
    void sinTokenResponde401Unauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders/1").header("X-Trace-Id", "t-1").build());

        entryPoint.commence(exchange, new BadCredentialsException("x")).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""));
        assertTrue(body.contains("\"traceId\":\"t-1\""));
        assertTrue(body.contains("\"status\":401"));
    }

    @Test
    void conTokenInvalidoResponde401TokenInvalidOrExpired() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders/1")
                        .header("Authorization", "Bearer abc")
                        .header("X-Trace-Id", "t-2").build());

        entryPoint.commence(exchange, new BadCredentialsException("x")).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"code\":\"TOKEN_INVALID_OR_EXPIRED\""));
        assertTrue(body.contains("\"traceId\":\"t-2\""));
    }
}
