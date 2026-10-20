package com.empresa.order.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Pedido no encontrado con ID: " + id);
    }
}
