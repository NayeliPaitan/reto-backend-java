package com.empresa.inventory.dto;

import com.empresa.inventory.model.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String code, String name, Integer stock, BigDecimal price) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getCode(), p.getName(), p.getStock(), p.getPrice());
    }
}
