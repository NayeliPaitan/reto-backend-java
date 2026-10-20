package com.empresa.order.exception;

public class StockInsufficientException extends RuntimeException {
    public StockInsufficientException(String message) {
        super(message);
    }
}