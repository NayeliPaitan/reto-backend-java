package com.empresa.order.service;

import com.empresa.order.dto.OrderRequestDto;
import com.empresa.order.exception.InvalidOrderTransitionException;
import com.empresa.order.exception.OrderNotFoundException;
import com.empresa.order.model.Order;
import com.empresa.order.model.OrderHistory;
import com.empresa.order.model.OrderStatus;
import com.empresa.order.repository.OrderHistoryRepository;
import com.empresa.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTransitionServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderHistoryRepository historyRepository;
    @InjectMocks
    private OrderTransitionService service;

    private Order orderIn(OrderStatus status) {
        return Order.builder().id(1L).productCode("PROD-001").quantity(2)
                .status(status).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void registraPedidoEnPendingConHistorialInicial() {
        OrderRequestDto request = new OrderRequestDto();
        request.setProductCode("PROD-001");
        request.setQuantity(2);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Order order = service.register(request);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("NONE", captor.getValue().getPreviousStatus());
        assertEquals("PENDING", captor.getValue().getNewStatus());
        assertEquals(1L, captor.getValue().getOrderId());
    }

    @Test
    void confirmaPedidoPendienteYGuardaTotal() {
        when(orderRepository.findWithLockById(1L)).thenReturn(Optional.of(orderIn(OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).then(AdditionalAnswers.returnsFirstArg());

        Order order = service.confirm(1L, new BigDecimal("2400.00"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(new BigDecimal("2400.00"), order.getTotalPrice());
        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("PENDING", captor.getValue().getPreviousStatus());
        assertEquals("CONFIRMED", captor.getValue().getNewStatus());
    }

    @Test
    void cancelaPedidoConfirmado() {
        when(orderRepository.findWithLockById(1L)).thenReturn(Optional.of(orderIn(OrderStatus.CONFIRMED)));
        when(orderRepository.save(any(Order.class))).then(AdditionalAnswers.returnsFirstArg());

        Order order = service.cancel(1L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("CONFIRMED", captor.getValue().getPreviousStatus());
        assertEquals("CANCELLED", captor.getValue().getNewStatus());
    }

    @Test
    void noPermiteCancelarDosVeces() {
        when(orderRepository.findWithLockById(1L)).thenReturn(Optional.of(orderIn(OrderStatus.CANCELLED)));

        assertThrows(InvalidOrderTransitionException.class, () -> service.cancel(1L));

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(historyRepository);
    }

    @Test
    void noPermiteCancelarPedidoFallido() {
        when(orderRepository.findWithLockById(1L)).thenReturn(Optional.of(orderIn(OrderStatus.FAILED)));

        assertThrows(InvalidOrderTransitionException.class, () -> service.cancel(1L));
    }

    @Test
    void marcaPedidoPendienteComoFallido() {
        when(orderRepository.findWithLockById(1L)).thenReturn(Optional.of(orderIn(OrderStatus.PENDING)));
        when(orderRepository.save(any(Order.class))).then(AdditionalAnswers.returnsFirstArg());

        Order order = service.fail(1L, "Stock insuficiente");

        assertEquals(OrderStatus.FAILED, order.getStatus());
        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("Stock insuficiente", captor.getValue().getReason());
    }

    @Test
    void lanzaNotFoundSiNoExiste() {
        when(orderRepository.findWithLockById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.cancel(99L));
    }
}
