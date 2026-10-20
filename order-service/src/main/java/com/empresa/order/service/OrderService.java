package com.empresa.order.service;

import com.empresa.order.client.InventoryClient;
import com.empresa.order.client.StockAvailability;
import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderRequestDto;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.exception.InventoryServiceException;
import com.empresa.order.exception.OrderNotFoundException;
import com.empresa.order.exception.StockInsufficientException;
import com.empresa.order.mapper.OrderMapper;
import com.empresa.order.model.Order;
import com.empresa.order.repository.OrderHistoryRepository;
import com.empresa.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderTransitionService transitions;
    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository historyRepository;
    private final OrderMapper mapper;

    public OrderResponse createOrder(OrderRequestDto request) {
        Order order = transitions.register(request);
        Long orderId = order.getId();

        StockAvailability stock;
        try {
            stock = inventoryClient.checkStock(request.getProductCode(), request.getQuantity());
        } catch (InventoryServiceException ex) {
            transitions.fail(orderId, "Inventario no disponible al validar stock");
            throw ex;
        }

        if (!stock.available()) {
            transitions.fail(orderId, "Stock insuficiente");
            throw new StockInsufficientException(
                    "El producto " + request.getProductCode() + " no cuenta con stock suficiente.");
        }

        try {
            inventoryClient.reduceStock(request.getProductCode(), request.getQuantity());
        } catch (StockInsufficientException ex) {
            transitions.fail(orderId, "Stock insuficiente al descontar");
            throw ex;
        } catch (InventoryServiceException ex) {
            transitions.fail(orderId, "Inventario no disponible al descontar stock");
            throw ex;
        }

        BigDecimal total = stock.price().multiply(BigDecimal.valueOf(request.getQuantity()));
        return mapper.toOrderResponse(transitions.confirm(orderId, total));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return mapper.toOrderResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getOrderHistory(Long orderId) {
        findOrder(orderId); // 404 si el pedido no existe
        return historyRepository.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    public OrderResponse cancelOrder(Long id) {
        return mapper.toOrderResponse(transitions.cancel(id));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
