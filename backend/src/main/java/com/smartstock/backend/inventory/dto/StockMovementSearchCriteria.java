package com.smartstock.backend.inventory.dto;

import com.smartstock.backend.inventory.entity.StockMovementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record StockMovementSearchCriteria(
    Long productId,
    StockMovementType movementType,
    String referenceType,
    Long referenceId,
    Instant from,
    Instant to,
    @Min(0) int page,
    @Min(1) @Max(100) int size,
    String sort
) {
}
