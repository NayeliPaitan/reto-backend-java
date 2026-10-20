package com.empresa.inventory;

import com.empresa.inventory.model.Product;
import com.empresa.inventory.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    // Datos de prueba para validar 
    @Bean
    CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(Product.builder()
                        .code("PROD-001")
                        .name("Laptop Gamer")
                        .stock(10)
                        .price(new BigDecimal("1200.00"))
                        .build());

                repository.save(Product.builder()
                        .code("PROD-002")
                        .name("Teclado Mecánico")
                        .stock(0) // Sin stock para probar el error STOCK_INSUFFICIENT
                        .price(new BigDecimal("80.00"))
                        .build());
            }
        };
    }
}