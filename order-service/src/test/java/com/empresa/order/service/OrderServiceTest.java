package com.empresa.order.service;

import com.empresa.order.client.InventoryClient;
import com.empresa.order.client.StockAvailability;
import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderRequestDto;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.exception.InventoryServiceException;
import com.empresa.order.exception.OrderNotFoundException;
import com.empresa.order.exception.StockInsufficientException;
import com.empresa.order.mapper.OrderMapperImpl;
import com.empresa.order.model.Order;
import com.empresa.order.model.OrderHistory;
import com.empresa.order.model.OrderStatus;
import com.empresa.order.repository.OrderHistoryRepository;
import com.empresa.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderTransitionService transitions;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderHistoryRepository historyRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(transitions, inventoryClient, orderRepository, historyRepository, new OrderMapperImpl());
    }

    private OrderRequestDto request() {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setProductCode("PROD-001");
        dto.setQuantity(2);
        return dto;
    }

    private Order order(OrderStatus status) {
        return Order.builder().id(1L).productCode("PROD-001").quantity(2)
                .status(status).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void creaPedidoConfirmadoCuandoHayStock() {
        Order confirmed = order(OrderStatus.CONFIRMED);
        confirmed.setTotalPrice(new BigDecimal("2400.00"));
        when(transitions.register(any(OrderRequestDto.class))).thenReturn(order(OrderStatus.PENDING));
        when(inventoryClient.checkStock("PROD-001", 2))
                .thenReturn(new StockAvailability(true, 10, new BigDecimal("1200.00")));
        when(transitions.confirm(eq(1L), eq(new BigDecimal("2400.00")))).thenReturn(confirmed);

        OrderResponse response = service.createOrder(request());

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(new BigDecimal("2400.00"), response.totalPrice());
        InOrder inOrder = inOrder(transitions, inventoryClient);
        inOrder.verify(transitions).register(any(OrderRequestDto.class));
        inOrder.verify(inventoryClient).checkStock("PROD-001", 2);
        inOrder.verify(inventoryClient).reduceStock("PROD-001", 2);
        inOrder.verify(transitions).confirm(eq(1L), eq(new BigDecimal("2400.00")));
    }

    @Test
    void marcaFallidoYLanzaErrorSiNoHayStock() {
        when(transitions.register(any(OrderRequestDto.class))).thenReturn(order(OrderStatus.PENDING));
        when(inventoryClient.checkStock("PROD-001", 2)).thenReturn(new StockAvailability(false, 0, null));

        assertThrows(StockInsufficientException.class, () -> service.createOrder(request()));

        verify(transitions).fail(1L, "Stock insuficiente");
        verify(inventoryClient, never()).reduceStock(anyString(), anyInt());
        verify(transitions, never()).confirm(any(), any());
    }

    @Test
    void marcaFallidoSiInventarioNoResponde() {
        when(transitions.register(any(OrderRequestDto.class))).thenReturn(order(OrderStatus.PENDING));
        when(inventoryClient.checkStock("PROD-001", 2)).thenThrow(new InventoryServiceException("caido", null));

        assertThrows(InventoryServiceException.class, () -> service.createOrder(request()));

        verify(transitions).fail(eq(1L), anyString());
        verify(transitions, never()).confirm(any(), any());
    }

    @Test
    void marcaFallidoSiFallaElDescuentoDeStock() {
        when(transitions.register(any(OrderRequestDto.class))).thenReturn(order(OrderStatus.PENDING));
        when(inventoryClient.checkStock("PROD-001", 2))
                .thenReturn(new StockAvailability(true, 10, new BigDecimal("1200.00")));
        org.mockito.Mockito.doThrow(new InventoryServiceException("caido", null))
                .when(inventoryClient).reduceStock("PROD-001", 2);

        assertThrows(InventoryServiceException.class, () -> service.createOrder(request()));

        verify(transitions).fail(eq(1L), anyString());
        verify(transitions, never()).confirm(any(), any());
    }

    @Test
    void marcaFallidoSiElDescuentoSeRechazaPorStockInsuficiente() {
        when(transitions.register(any(OrderRequestDto.class))).thenReturn(order(OrderStatus.PENDING));
        when(inventoryClient.checkStock("PROD-001", 2))
                .thenReturn(new StockAvailability(true, 10, new BigDecimal("1200.00")));
        org.mockito.Mockito.doThrow(new StockInsufficientException("otro pedido consumio el stock"))
                .when(inventoryClient).reduceStock("PROD-001", 2);

        assertThrows(StockInsufficientException.class, () -> service.createOrder(request()));

        verify(transitions).fail(eq(1L), anyString());
        verify(transitions, never()).confirm(any(), any());
    }

    @Test
    void consultaPedidoExistente() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        OrderResponse response = service.getOrderById(1L);

        assertEquals(1L, response.id());
        assertEquals(OrderStatus.PENDING, response.status());
    }

    @Test
    void lanzaNotFoundSiElPedidoNoExiste() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.getOrderById(9L));
    }

    @Test
    void devuelveHistorialDelPedido() {
        OrderHistory h1 = OrderHistory.builder().id(1L).orderId(1L).previousStatus("NONE").newStatus("PENDING")
                .reason("r").timestamp(LocalDateTime.now()).build();
        OrderHistory h2 = OrderHistory.builder().id(2L).orderId(1L).previousStatus("PENDING").newStatus("CONFIRMED")
                .reason("r").timestamp(LocalDateTime.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        when(historyRepository.findByOrderIdOrderByIdAsc(1L)).thenReturn(List.of(h1, h2));

        List<OrderHistoryResponse> history = service.getOrderHistory(1L);

        assertEquals(2, history.size());
        assertEquals("PENDING", history.get(0).newStatus());
        assertEquals("CONFIRMED", history.get(1).newStatus());
    }

    @Test
    void historialDePedidoInexistenteLanzaNotFound() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.getOrderHistory(9L));
    }

    @Test
    void cancelaDelegandoEnElServicioDeTransiciones() {
        when(transitions.cancel(1L)).thenReturn(order(OrderStatus.CANCELLED));

        OrderResponse response = service.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, response.status());
    }
}
