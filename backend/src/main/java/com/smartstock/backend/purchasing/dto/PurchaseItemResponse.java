package com.smartstock.backend.purchasing.dto;

import java.math.BigDecimal;

public record PurchaseItemResponse(
    Long id,
    Long productId,
    String productName,
    BigDecimal quantity,
    BigDecimal receivedQuantity,
    BigDecimal unitCost,
    BigDecimal lineTotal
) {
}
