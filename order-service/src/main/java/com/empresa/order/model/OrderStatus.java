package com.empresa.order.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del pedido y sus transiciones permitidas (la regla vive en el dominio, no en el servicio).
 * PENDING   -> CONFIRMED | CANCELLED | FAILED
 * CONFIRMED -> CANCELLED
 * CANCELLED -> (terminal)
 * FAILED    -> (terminal)
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    FAILED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED, FAILED);
            case CONFIRMED -> EnumSet.of(CANCELLED);
            case CANCELLED, FAILED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
