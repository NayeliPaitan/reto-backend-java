package com.empresa.order.exception;

public record ErrorResponse(String timestamp, int status, String code, String message, String traceId) {
}
