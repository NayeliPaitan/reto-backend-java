package com.empresa.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockCheckResponse {
    private String productCode;
    private boolean available;
    private Integer currentStock;
    private BigDecimal price;
}