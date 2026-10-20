package com.empresa.order.exception;

import com.empresa.order.model.OrderStatus;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }

    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("Transicion invalida: un pedido en estado " + from + " no puede pasar a " + to
                + (from.isTerminal() ? " (estado final)" : ". Permitidas: " + from.allowedTransitions()));
    }
}
