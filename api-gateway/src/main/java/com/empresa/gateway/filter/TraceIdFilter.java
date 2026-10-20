package com.empresa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements WebFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        final String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(TRACE_ID_HEADER, traceId))
                .build();

        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        final String method = request.getMethod().name();
        final String path = request.getURI().getPath();
        log.info("traceId={} --> {} {}", traceId, method, path);

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("traceId={} <-- {} {} status={}",
                        traceId, method, path, exchange.getResponse().getStatusCode()));
    }
}