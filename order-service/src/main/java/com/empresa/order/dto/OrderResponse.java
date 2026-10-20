package com.empresa.order.dto;

import com.empresa.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String productCode,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt) {
}
