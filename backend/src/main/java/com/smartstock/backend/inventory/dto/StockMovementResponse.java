package com.smartstock.backend.inventory.dto;

import com.smartstock.backend.inventory.entity.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementResponse(
    Long id,
    Long productId,
    String sku,
    String productName,
    StockMovementType movementType,
    BigDecimal quantity,
    BigDecimal quantityBefore,
    BigDecimal quantityAfter,
    String referenceType,
    Long referenceId,
    String reason,
    Long performedByUserId,
    String performedByUsername,
    Instant createdAt
) {
}
