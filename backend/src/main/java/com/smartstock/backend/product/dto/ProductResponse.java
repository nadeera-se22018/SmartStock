package com.smartstock.backend.product.dto;

import com.smartstock.backend.category.dto.CategorySummary;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String sku,
    String barcode,
    String name,
    String description,
    BigDecimal costPrice,
    BigDecimal sellingPrice,
    BigDecimal reorderLevel,
    boolean active,
    Long version,
    CategorySummary category,
    BigDecimal availableQuantity,
    Instant createdAt,
    Instant updatedAt
) {
}
