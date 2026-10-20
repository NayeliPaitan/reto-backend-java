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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductService.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    private Product product(String code, int stock) {
        return Product.builder().id(1L).code(code).name("Mouse").stock(stock)
                .price(new BigDecimal("35.50")).build();
    }

    @Test
    void creaProducto201() throws Exception {
        when(productRepository.existsByCode("PROD-003")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product("PROD-003", 25));

        mockMvc.perform(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PROD-003\",\"name\":\"Mouse\",\"stock\":25,\"price\":35.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PROD-003"))
                .andExpect(jsonPath("$.stock").value(25));
    }

    @Test
    void productoDuplicado409() throws Exception {
        when(productRepository.existsByCode("PROD-001")).thenReturn(true);

        mockMvc.perform(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PROD-001\",\"name\":\"Laptop\",\"stock\":1,\"price\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_EXISTS"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void datosInvalidos400() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/products").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-p-1")
                        .content("{\"code\":\"\",\"name\":\"X\",\"stock\":-1,\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").value("trace-p-1"));
    }

    @Test
    void listaProductos() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of(product("PROD-001", 10), product("PROD-002", 0)));

        mockMvc.perform(get("/api/v1/inventory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("PROD-001"));
    }

    @Test
    void consultaProductoInexistente404() throws Exception {
        when(productRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/inventory/products/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void ajustaStock() throws Exception {
        Product p = product("PROD-001", 10);
        when(productRepository.findByCode("PROD-001")).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/inventory/products/PROD-001/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(99));
    }

    @Test
    void stockNegativo400() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/products/PROD-001/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
