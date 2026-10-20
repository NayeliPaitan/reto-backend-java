package com.empresa.gateway.error;

import com.empresa.gateway.filter.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerWebExchange exchange, int status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        String traceId = exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        if (traceId == null) {
            traceId = exchange.getRequest().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = "N/A";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        body.put("traceId", traceId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"status\":" + status + ",\"code\":\"" + code + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        response.setStatusCode(HttpStatus.resolve(status) != null ? HttpStatus.valueOf(status) : HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(TraceIdFilter.TRACE_ID_HEADER, traceId);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
