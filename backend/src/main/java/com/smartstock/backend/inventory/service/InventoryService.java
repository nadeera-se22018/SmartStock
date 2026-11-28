package com.smartstock.backend.inventory.service;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.InventorySearchCriteria;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.dto.StockMovementSearchCriteria;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;

import java.util.List;

public interface InventoryService {
    PageResponse<InventoryBalanceResponse> searchBalances(InventorySearchCriteria criteria);

    InventoryBalanceResponse getBalance(Long productId);

    PageResponse<StockMovementResponse> searchMovements(StockMovementSearchCriteria criteria);

    StockMovementResponse getMovement(Long movementId);

    InventoryAdjustmentResult adjust(StockAdjustmentRequest request, String idempotencyKey);

    InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        Long receivedByUserId,
        List<PurchaseReceiptLine> lines
    );
}
