package com.smartstock.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
    @NotBlank @Size(max = 50) String sku,
    @Size(max = 100) String barcode,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin(value = "0.00") BigDecimal costPrice,
    @NotNull @DecimalMin(value = "0.00") BigDecimal sellingPrice,
    @NotNull @DecimalMin(value = "0.00") BigDecimal reorderLevel,
    Long categoryId
) {
}
