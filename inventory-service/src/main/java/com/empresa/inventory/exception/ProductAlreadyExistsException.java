package com.empresa.inventory.exception;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException(String code) {
        super("Ya existe un producto con el codigo: " + code);
    }
}
