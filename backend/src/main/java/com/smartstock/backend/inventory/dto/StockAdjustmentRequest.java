package com.smartstock.backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockAdjustmentRequest(
    @NotNull Long productId,
    @NotNull StockAdjustmentDirection direction,
    @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
    @NotBlank @Size(max = 255) String reason
) {
}
