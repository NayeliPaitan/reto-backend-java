package com.empresa.order.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} permitido = {2}")
    @CsvSource({
            "PENDING,CONFIRMED,true",
            "PENDING,CANCELLED,true",
            "PENDING,FAILED,true",
            "PENDING,PENDING,false",
            "CONFIRMED,CANCELLED,true",
            "CONFIRMED,PENDING,false",
            "CONFIRMED,FAILED,false",
            "CONFIRMED,CONFIRMED,false",
            "CANCELLED,CANCELLED,false",
            "CANCELLED,CONFIRMED,false",
            "FAILED,CANCELLED,false",
            "FAILED,CONFIRMED,false"
    })
    void validaTransiciones(OrderStatus from, OrderStatus to, boolean esperado) {
        assertEquals(esperado, from.canTransitionTo(to));
    }

    @Test
    void estadosFinales() {
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertTrue(OrderStatus.FAILED.isTerminal());
        assertFalse(OrderStatus.PENDING.isTerminal());
        assertFalse(OrderStatus.CONFIRMED.isTerminal());
    }
}
