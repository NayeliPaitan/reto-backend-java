package com.empresa.order.controller;

import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.exception.InvalidOrderTransitionException;
import com.empresa.order.exception.InventoryServiceException;
import com.empresa.order.exception.OrderNotFoundException;
import com.empresa.order.exception.StockInsufficientException;
import com.empresa.order.model.OrderStatus;
import com.empresa.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    private static final String TRACE = "trace-test-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private OrderResponse response(OrderStatus status) {
        return new OrderResponse(1L, "PROD-001", 2, new BigDecimal("2400.00"), status, LocalDateTime.now());
    }

    @Test
    void creaPedido201() throws Exception {
        when(orderService.createOrder(any())).thenReturn(response(OrderStatus.CONFIRMED));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"PROD-001\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void rechazaDatosInvalidosConFormatoDeError() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Trace-Id", TRACE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("productCode")))
                .andExpect(jsonPath("$.message").value(containsString("quantity")))
                .andExpect(jsonPath("$.traceId").value(TRACE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rechazaCantidadNula() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"PROD-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rechazaJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"PROD-001\",\"quantity\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void stockInsuficiente400() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new StockInsufficientException("sin stock"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Trace-Id", TRACE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"PROD-002\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STOCK_INSUFFICIENT"))
                .andExpect(jsonPath("$.traceId").value(TRACE));
    }

    @Test
    void inventarioNoDisponible503() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new InventoryServiceException("caido", null));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"PROD-001\",\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("INVENTORY_UNAVAILABLE"));
    }

    @Test
    void pedidoInexistente404() throws Exception {
        when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/v1/orders/99").header("X-Trace-Id", TRACE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value(TRACE));
    }

    @Test
    void idNoNumerico400() throws Exception {
        mockMvc.perform(get("/api/v1/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void historialDePedidoInexistente404() throws Exception {
        when(orderService.getOrderHistory(5L)).thenThrow(new OrderNotFoundException(5L));

        mockMvc.perform(get("/api/v1/orders/5/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void consultaHistorial200() throws Exception {
        when(orderService.getOrderHistory(1L)).thenReturn(List.of(
                new OrderHistoryResponse(1L, 1L, "NONE", "PENDING", "r", LocalDateTime.now()),
                new OrderHistoryResponse(2L, 1L, "PENDING", "CONFIRMED", "r", LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/orders/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].newStatus").value("CONFIRMED"));
    }

    @Test
    void cancelacionDuplicada409() throws Exception {
        when(orderService.cancelOrder(1L))
                .thenThrow(new InvalidOrderTransitionException(OrderStatus.CANCELLED, OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/v1/orders/1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void errorInternoNoExponeDetalle() throws Exception {
        when(orderService.getOrderById(1L)).thenThrow(new RuntimeException("detalle-interno-secreto"));

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(not(containsString("secreto"))));
    }
}
