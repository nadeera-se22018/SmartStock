package com.smartstock.backend.inventory.repository;

import com.smartstock.backend.inventory.entity.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;

public interface StockMovementProjection {
    Long getId();
    Long getProductId();
    String getSku();
    String getProductName();
    StockMovementType getMovementType();
    BigDecimal getQuantity();
    BigDecimal getQuantityBefore();
    BigDecimal getQuantityAfter();
    String getReferenceType();
    Long getReferenceId();
    String getReason();
    Long getPerformedByUserId();
    String getPerformedByUsername();
    Instant getCreatedAt();
}
