package com.smartstock.backend.supplier.port;

import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class UnimplementedSupplierPurchaseHistoryReader implements SupplierPurchaseHistoryReader {
    @Override
    public PageResponse<SupplierPurchaseSummaryResponse> getHistory(
        Long supplierId, LocalDate from, LocalDate to, String status,
        int page, int size, String sort
    ) {
        throw new BusinessRuleException(
            "PURCHASE_HISTORY_NOT_IMPLEMENTED",
            "Supplier purchase history is not available until EPIC-04 is implemented."
        );
    }
}
