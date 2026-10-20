package com.empresa.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String code) {
        super("Stock insuficiente para el producto: " + code);
    }
}
