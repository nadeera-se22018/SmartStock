package com.smartstock.backend.inventory.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface InventoryBalanceProjection {
    Long getProductId();
    String getSku();
    String getBarcode();
    String getProductName();
    BigDecimal getQuantityOnHand();
    BigDecimal getQuantityReserved();
    BigDecimal getAvailableQuantity();
    BigDecimal getReorderLevel();
    Instant getUpdatedAt();
}
