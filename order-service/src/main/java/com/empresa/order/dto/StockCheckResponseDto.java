package com.empresa.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockCheckResponseDto {
    private String productCode;
    private boolean available;
    private Integer currentStock;
    private BigDecimal price;
}