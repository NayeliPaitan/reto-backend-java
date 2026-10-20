package com.empresa.order.client;

public interface InventoryClient {

    StockAvailability checkStock(String productCode, int quantity);

    void reduceStock(String productCode, int quantity);
}
