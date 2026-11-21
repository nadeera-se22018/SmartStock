package com.smartstock.backend.supplier.dto;
import java.time.Instant;
public record SupplierResponse(Long id, String name, String contactPerson, String email, String phone, String address,
    boolean active, Long version, Instant createdAt, Instant updatedAt) {}
