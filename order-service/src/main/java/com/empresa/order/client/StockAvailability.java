package com.empresa.order.client;

import java.math.BigDecimal;

public record StockAvailability(boolean available, Integer currentStock, BigDecimal price) {
}
