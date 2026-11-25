package com.smartstock.backend.purchasing.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record PurchaseSearchCriteria(
    @Positive Long supplierId,
    PurchaseStatus status,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    @Pattern(regexp = "(orderDate|purchaseNumber|status|totalAmount|updatedAt)(,(asc|desc))?")
    String sort
) {
    public PurchaseSearchCriteria {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sort = sort == null || sort.isBlank() ? "orderDate,desc" : sort;
    }

    @AssertTrue(message = "The from date must not be after the to date.")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
