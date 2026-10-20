package com.empresa.order.mapper;

import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.model.Order;
import com.empresa.order.model.OrderHistory;
import com.empresa.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapperImpl();

    @Test
    void mapeaPedido() {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder().id(7L).productCode("PROD-001").quantity(2)
                .totalPrice(new BigDecimal("2400.00")).status(OrderStatus.CONFIRMED).createdAt(now).build();

        OrderResponse response = mapper.toOrderResponse(order);

        assertEquals(7L, response.id());
        assertEquals("PROD-001", response.productCode());
        assertEquals(2, response.quantity());
        assertEquals(new BigDecimal("2400.00"), response.totalPrice());
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(now, response.createdAt());
    }

    @Test
    void mapeaHistorial() {
        LocalDateTime now = LocalDateTime.now();
        OrderHistory history = OrderHistory.builder().id(3L).orderId(7L).previousStatus("PENDING")
                .newStatus("CONFIRMED").reason("ok").timestamp(now).build();

        OrderHistoryResponse response = mapper.toHistoryResponse(history);

        assertEquals(3L, response.id());
        assertEquals(7L, response.orderId());
        assertEquals("PENDING", response.previousStatus());
        assertEquals("CONFIRMED", response.newStatus());
        assertEquals("ok", response.reason());
        assertEquals(now, response.timestamp());
    }
}
