package com.smartstock.backend.purchasing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseResponse(
    Long id,
    String purchaseNumber,
    Long supplierId,
    String supplierName,
    LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    PurchaseStatus status,
    String notes,
    BigDecimal subTotal,
    BigDecimal totalAmount,
    List<PurchaseItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
}
