package com.empresa.inventory.config;

import com.empresa.inventory.controller.InventoryController;
import com.empresa.inventory.repository.ProductRepository;
import com.empresa.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, RestSecurityErrorHandler.class, ProductService.class})
@TestPropertySource(properties = {
        "app.security.issuer=http://localhost:8180/realms/reto",
        "app.security.jwk-set-uri=http://localhost:8180/realms/reto/protocol/openid-connect/certs"
})
class InventorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "PROD-001").param("quantity", "1")
                        .header("X-Trace-Id", "trace-inv-sec"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("trace-inv-sec"));
    }

    @Test
    void conTokenValidoPermiteElAcceso() throws Exception {
        when(productRepository.findByCode("PROD-001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/inventory/check").param("productCode", "PROD-001").param("quantity", "1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
