package com.smartstock.backend.supplier.service;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.supplier.dto.SupplierCreateRequest;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import com.smartstock.backend.supplier.dto.SupplierResponse;
import com.smartstock.backend.supplier.dto.SupplierSearchCriteria;
import com.smartstock.backend.supplier.dto.SupplierUpdateRequest;
import java.time.LocalDate;

public interface SupplierService {
    SupplierResponse create(SupplierCreateRequest request);
    SupplierResponse getById(Long id);
    PageResponse<SupplierResponse> search(SupplierSearchCriteria criteria);
    SupplierResponse update(Long id, SupplierUpdateRequest request);
    void deactivate(Long id);
    void activate(Long id);
    PageResponse<SupplierPurchaseSummaryResponse> getPurchaseHistory(
        Long supplierId, LocalDate from, LocalDate to, String status,
        int page, int size, String sort
    );
}
