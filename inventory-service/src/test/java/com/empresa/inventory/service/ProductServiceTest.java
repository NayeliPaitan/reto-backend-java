package com.empresa.inventory.service;

import com.empresa.inventory.exception.InsufficientStockException;
import com.empresa.inventory.exception.ProductNotFoundException;
import com.empresa.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void descuentoExitoso() {
        when(repository.decrementStock("P1", 2)).thenReturn(1);
        assertDoesNotThrow(() -> service.reduceStock("P1", 2));
    }

    @Test
    void sinStockLanzaInsufficientStock() {
        when(repository.decrementStock("P1", 99)).thenReturn(0);
        when(repository.existsByCode("P1")).thenReturn(true);
        assertThrows(InsufficientStockException.class, () -> service.reduceStock("P1", 99));
    }

    @Test
    void productoInexistenteLanzaNotFound() {
        when(repository.decrementStock("X", 1)).thenReturn(0);
        when(repository.existsByCode("X")).thenReturn(false);
        assertThrows(ProductNotFoundException.class, () -> service.reduceStock("X", 1));
    }
}
