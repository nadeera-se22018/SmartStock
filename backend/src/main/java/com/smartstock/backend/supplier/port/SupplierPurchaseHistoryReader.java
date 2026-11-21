package com.smartstock.backend.supplier.port;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import java.time.LocalDate;

public interface SupplierPurchaseHistoryReader {
    PageResponse<SupplierPurchaseSummaryResponse> getHistory(
        Long supplierId, LocalDate from, LocalDate to, String status,
        int page, int size, String sort
    );
}
