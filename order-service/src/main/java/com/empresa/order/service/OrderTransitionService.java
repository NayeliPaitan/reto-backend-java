package com.empresa.order.service;

import com.empresa.order.dto.OrderRequestDto;
import com.empresa.order.exception.InvalidOrderTransitionException;
import com.empresa.order.exception.OrderNotFoundException;
import com.empresa.order.model.Order;
import com.empresa.order.model.OrderHistory;
import com.empresa.order.model.OrderStatus;
import com.empresa.order.repository.OrderHistoryRepository;
import com.empresa.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransitionService {

    public static final String NO_PREVIOUS_STATUS = "NONE";

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository historyRepository;

    @Transactional
    public Order register(OrderRequestDto request) {
        Order order = orderRepository.save(Order.builder()
                .productCode(request.getProductCode())
                .quantity(request.getQuantity())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        recordHistory(order.getId(), NO_PREVIOUS_STATUS, OrderStatus.PENDING,
                "Pedido registrado, pendiente de validacion de stock");
        log.info("Pedido {} registrado en estado PENDING", order.getId());
        return order;
    }

    @Transactional
    public Order confirm(Long orderId, BigDecimal totalPrice) {
        Order order = lockOrder(orderId);
        order.setTotalPrice(totalPrice);
        return applyTransition(order, OrderStatus.CONFIRMED, "Stock validado y descontado; pedido confirmado");
    }

    @Transactional
    public Order fail(Long orderId, String reason) {
        return applyTransition(lockOrder(orderId), OrderStatus.FAILED, reason);
    }

    @Transactional
    public Order cancel(Long orderId) {
        return applyTransition(lockOrder(orderId), OrderStatus.CANCELLED, "Cancelacion solicitada por el usuario");
    }

    private Order applyTransition(Order order, OrderStatus target, String reason) {
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidOrderTransitionException(current, target);
        }
        order.setStatus(target);
        Order saved = orderRepository.save(order);
        recordHistory(saved.getId(), current.name(), target, reason);
        log.info("Pedido {}: {} -> {}", saved.getId(), current, target);
        return saved;
    }

    private Order lockOrder(Long orderId) {
        return orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void recordHistory(Long orderId, String previous, OrderStatus next, String reason) {
        historyRepository.save(OrderHistory.builder()
                .orderId(orderId)
                .previousStatus(previous)
                .newStatus(next.name())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
