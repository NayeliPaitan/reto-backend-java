package com.empresa.inventory.exception;

public record ErrorResponse(String timestamp, int status, String code, String message, String traceId) {
}
