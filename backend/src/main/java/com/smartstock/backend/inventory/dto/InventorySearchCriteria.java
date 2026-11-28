package com.smartstock.backend.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record InventorySearchCriteria(
    String search,
    Long categoryId,
    Boolean active,
    InventoryStockStatus stockStatus,
    @Min(0) int page,
    @Min(1) @Max(100) int size,
    String sort
) {
}
