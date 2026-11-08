package com.smartstock.backend.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProductSearchCriteria(
    String search,
    Long categoryId,
    Boolean active,
    @Min(0) int page,
    @Min(1) @Max(100) int size,
    String sort
) {
    public ProductSearchCriteria {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
    }

    public ProductSearchCriteria() {
        this(null, null, true, 0, 20, "name");
    }
}
