package com.empresa.order.exception;

/** Falla al comunicarse con Inventory Service (red, timeout o error HTTP). */
public class InventoryServiceException extends RuntimeException {
    public InventoryServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
