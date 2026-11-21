package com.smartstock.backend.supplier.dto;
import java.math.BigDecimal; import java.time.Instant;
public record SupplierPurchaseSummaryResponse(Long purchaseId, String purchaseNumber, Instant orderDate,
    String status, BigDecimal totalAmount, BigDecimal outstandingAmount, Instant dueDate) {}
