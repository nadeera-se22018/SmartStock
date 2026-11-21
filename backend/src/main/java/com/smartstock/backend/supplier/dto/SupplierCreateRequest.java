package com.smartstock.backend.supplier.dto;
import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record SupplierCreateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 180) String contactPerson,
    @Email @Size(max = 255) String email,
    @Size(max = 30) String phone,
    @Size(max = 10000) String address
) {}
