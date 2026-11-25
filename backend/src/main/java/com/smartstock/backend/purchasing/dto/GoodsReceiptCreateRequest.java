package com.smartstock.backend.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record GoodsReceiptCreateRequest(
    Instant receivedAt,
    @Size(max = 2000) String notes,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid GoodsReceiptItemRequest> items
) {
}
