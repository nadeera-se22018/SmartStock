package com.smartstock.backend.purchasing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseItemRequest(
    @NotNull @Positive Long productId,
    @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0") @Digits(integer = 15, fraction = 4) BigDecimal unitCost
) {
}
