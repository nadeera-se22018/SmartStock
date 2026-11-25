package com.smartstock.backend.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PurchaseCreateRequest(
    @NotNull @Positive Long supplierId,
    @NotNull LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    @Size(max = 2000) String notes,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid PurchaseItemRequest> items
) {
}
