package com.smartstock.backend.supplier.mapper;
import com.smartstock.backend.supplier.dto.SupplierCreateRequest;
import com.smartstock.backend.supplier.dto.SupplierResponse;
import com.smartstock.backend.supplier.dto.SupplierUpdateRequest;
import com.smartstock.backend.supplier.entity.Supplier;
import org.springframework.stereotype.Component;
@Component
public class SupplierMapper {
    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getContactPerson(), supplier.getEmail(),
            supplier.getPhone(), supplier.getAddress(), supplier.isActive(), supplier.getVersion(),
            supplier.getCreatedAt(), supplier.getUpdatedAt());
    }
    public Supplier toEntity(SupplierCreateRequest request, String normalizedName) {
        return new Supplier(request.name(), normalizedName, request.contactPerson(), request.email(), request.phone(), request.address());
    }
    public void updateEntity(Supplier supplier, SupplierUpdateRequest request, String normalizedName) {
        supplier.updateDetails(request.name(), normalizedName, request.contactPerson(), request.email(), request.phone(), request.address());
    }
}
