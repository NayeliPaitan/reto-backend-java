package com.empresa.order.dto;

import java.time.LocalDateTime;

public record OrderHistoryResponse(
        Long id,
        Long orderId,
        String previousStatus,
        String newStatus,
        String reason,
        LocalDateTime timestamp) {
}
