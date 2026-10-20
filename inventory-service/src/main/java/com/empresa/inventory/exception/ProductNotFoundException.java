package com.empresa.inventory.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String code) {
        super("Producto no encontrado: " + code);
    }
}
