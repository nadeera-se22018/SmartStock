package com.smartstock.backend.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryBalanceResponse(
    Long productId,
    String sku,
    String barcode,
    String productName,
    BigDecimal quantityOnHand,
    BigDecimal quantityReserved,
    BigDecimal availableQuantity,
    BigDecimal reorderLevel,
    boolean lowStock,
    boolean outOfStock,
    Instant updatedAt
) {
}
