package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.inventory.service.InventoryService;
import com.smartstock.backend.purchasing.port.InventoryReceivingPort;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PurchaseReceiptInventoryAdapter implements InventoryReceivingPort {
    private final InventoryService inventoryService;

    public PurchaseReceiptInventoryAdapter(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        Long receivedByUserId,
        List<PurchaseReceiptLine> lines
    ) {
        return inventoryService.applyPurchaseReceipt(purchaseOrderId, goodsReceiptId, receivedByUserId, lines);
    }
}
