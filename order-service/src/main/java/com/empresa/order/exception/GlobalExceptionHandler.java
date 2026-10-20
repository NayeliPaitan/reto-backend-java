package com.empresa.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import com.empresa.order.config.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidOrderTransitionException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<ErrorResponse> handleInventoryUnavailable(InventoryServiceException ex, HttpServletRequest request) {
        log.error("Inventory Service no disponible", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_UNAVAILABLE",
                "El servicio de inventario no esta disponible, intente nuevamente", request);
    }

    @ExceptionHandler(StockInsufficientException.class)
    public ResponseEntity<ErrorResponse> handleStockInsufficient(StockInsufficientException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "STOCK_INSUFFICIENT", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map((FieldError fe) -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                detail.isBlank() ? "Datos de entrada invalidos" : detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "El cuerpo de la solicitud es invalido o esta mal formado", request);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(TypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "El valor '" + ex.getValue() + "' no es valido para el parametro", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Metodo HTTP no permitido para este recurso", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso no encontrado", request);
    }
 
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error interno no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get(TraceIdFilter.MDC_KEY);
        }
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(), status.value(), code, message,
                traceId != null && !traceId.isBlank() ? traceId : "N/A");
        return ResponseEntity.status(status).body(body);
    }
}