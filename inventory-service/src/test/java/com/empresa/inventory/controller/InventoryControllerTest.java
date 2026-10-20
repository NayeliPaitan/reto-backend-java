package com.empresa.inventory.controller;

import com.empresa.inventory.model.Product;
import com.empresa.inventory.repository.ProductRepository;
import com.empresa.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductService.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    private Product product(int stock) {
        Product p = new Product();
        p.setId(1L);
        p.setCode("PROD-001");
        p.setName("Laptop");
        p.setStock(stock);
        p.setPrice(new BigDecimal("1200.00"));
        return p;
    }

    @Test
    void hayStockSuficiente() throws Exception {
        when(productRepository.findByCode("PROD-001")).thenReturn(Optional.of(product(10)));

        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "PROD-001").param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.currentStock").value(10))
                .andExpect(jsonPath("$.price").value(1200.00));
    }

    @Test
    void stockInsuficiente() throws Exception {
        when(productRepository.findByCode("PROD-001")).thenReturn(Optional.of(product(1)));

        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "PROD-001").param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void productoInexistenteNoEstaDisponible() throws Exception {
        when(productRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "NOPE").param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.currentStock").value(0));
    }

    @Test
    void cantidadInvalida400() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/check")
                        .header("X-Trace-Id", "trace-inv-1")
                        .param("productCode", "PROD-001").param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").value("trace-inv-1"));
    }

    @Test
    void parametroFaltante400() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "PROD-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void descuentaStock() throws Exception {
        when(productRepository.decrementStock("PROD-001", 3)).thenReturn(1);

        mockMvc.perform(put("/api/v1/inventory/reduce-stock").param("productCode", "PROD-001").param("quantity", "3"))
                .andExpect(status().isOk());

        verify(productRepository).decrementStock("PROD-001", 3);
    }

    @Test
    void descontarSinStockSuficiente409() throws Exception {
        when(productRepository.decrementStock("PROD-001", 50)).thenReturn(0);
        when(productRepository.existsByCode("PROD-001")).thenReturn(true);

        mockMvc.perform(put("/api/v1/inventory/reduce-stock").param("productCode", "PROD-001").param("quantity", "50"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_INSUFFICIENT"));
    }

    @Test
    void descontarProductoInexistente404() throws Exception {
        when(productRepository.decrementStock("NOPE", 1)).thenReturn(0);
        when(productRepository.existsByCode("NOPE")).thenReturn(false);

        mockMvc.perform(put("/api/v1/inventory/reduce-stock").param("productCode", "NOPE").param("quantity", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
