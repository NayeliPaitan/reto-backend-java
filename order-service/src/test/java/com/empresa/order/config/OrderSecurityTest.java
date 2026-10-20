package com.empresa.order.config;

import com.empresa.order.controller.OrderController;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.model.OrderStatus;
import com.empresa.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestSecurityErrorHandler.class})
@TestPropertySource(properties = {
        "app.security.issuer=http://localhost:8180/realms/reto",
        "app.security.jwk-set-uri=http://localhost:8180/realms/reto/protocol/openid-connect/certs"
})
class OrderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void sinTokenResponde401ConFormatoDeError() throws Exception {
        mockMvc.perform(get("/api/v1/orders/1").header("X-Trace-Id", "trace-sec-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").value("trace-sec-1"));
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/1")
                        .header("Authorization", "Bearer esto.no.es-un-jwt")
                        .header("X-Trace-Id", "trace-sec-2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID_OR_EXPIRED"))
                .andExpect(jsonPath("$.traceId").value("trace-sec-2"));
    }

    @Test
    void conTokenValidoPermiteElAcceso() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(
                new OrderResponse(1L, "PROD-001", 2, new BigDecimal("2400.00"), OrderStatus.CONFIRMED, LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/orders/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
