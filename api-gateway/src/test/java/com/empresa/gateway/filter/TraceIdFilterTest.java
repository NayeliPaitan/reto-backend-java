package com.empresa.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generaTraceIdSiNoViene() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders/1").build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            seen.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String forwarded = seen.get().getRequest().getHeaders().getFirst("X-Trace-Id");
        assertNotNull(forwarded);
        assertEquals(forwarded, exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void respetaTraceIdRecibido() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders/1").header("X-Trace-Id", "trace-abc").build());
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            seen.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("trace-abc", seen.get().getRequest().getHeaders().getFirst("X-Trace-Id"));
        assertEquals("trace-abc", exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
    }
}
