package com.smartstock.backend.purchasing.port;

import java.util.List;

public interface InventoryReceivingPort {
    InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        Long receivedByUserId,
        List<PurchaseReceiptLine> lines
    );
}
