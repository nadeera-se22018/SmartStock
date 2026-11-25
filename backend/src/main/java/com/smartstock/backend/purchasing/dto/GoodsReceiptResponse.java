package com.smartstock.backend.purchasing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;

public record GoodsReceiptResponse(
    Long id,
    String receiptNumber,
    Long purchaseOrderId,
    String purchaseOrderNumber,
    Instant receivedAt,
    String receivedBy,
    PurchaseStatus statusAfterReceipt,
    String notes,
    List<GoodsReceiptItemResponse> items,
    @JsonIgnore boolean replayed
) {
    public GoodsReceiptResponse(
        Long id,
        String receiptNumber,
        Long purchaseOrderId,
        String purchaseOrderNumber,
        Instant receivedAt,
        String receivedBy,
        PurchaseStatus statusAfterReceipt,
        String notes,
        List<GoodsReceiptItemResponse> items
    ) {
        this(id, receiptNumber, purchaseOrderId, purchaseOrderNumber, receivedAt, receivedBy,
            statusAfterReceipt, notes, items, false);
    }

    public GoodsReceiptResponse asReplay() {
        return new GoodsReceiptResponse(id, receiptNumber, purchaseOrderId, purchaseOrderNumber,
            receivedAt, receivedBy, statusAfterReceipt, notes, items, true);
    }
}
