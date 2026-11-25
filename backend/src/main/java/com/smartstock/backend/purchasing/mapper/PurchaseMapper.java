package com.smartstock.backend.purchasing.mapper;

import com.smartstock.backend.purchasing.dto.GoodsReceiptItemResponse;
import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseItemResponse;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseStatus;
import com.smartstock.backend.purchasing.entity.GoodsReceipt;
import com.smartstock.backend.purchasing.entity.GoodsReceiptItem;
import com.smartstock.backend.purchasing.entity.PurchaseOrder;
import com.smartstock.backend.purchasing.entity.PurchaseOrderItem;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class PurchaseMapper {
    public PurchaseResponse toPurchaseResponse(PurchaseOrder order) {
        return new PurchaseResponse(
            order.getId(),
            order.getPurchaseNumber(),
            order.getSupplier().getId(),
            order.getSupplier().getName(),
            order.getOrderDate(),
            order.getExpectedDeliveryDate(),
            order.getStatus(),
            order.getNotes(),
            order.getSubTotal(),
            order.getTotalAmount(),
            order.getItems().stream().map(this::toPurchaseItemResponse).toList(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    public PurchaseItemResponse toPurchaseItemResponse(PurchaseOrderItem item) {
        return new PurchaseItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getQuantity(),
            item.getReceivedQuantity(),
            item.getUnitCost(),
            item.getLineTotal()
        );
    }

    public GoodsReceiptResponse toGoodsReceiptResponse(GoodsReceipt receipt) {
        return toGoodsReceiptResponse(receipt, receipt.getStatusAfterReceipt());
    }

    public GoodsReceiptResponse toGoodsReceiptResponse(GoodsReceipt receipt, PurchaseStatus statusAfterReceipt) {
        return new GoodsReceiptResponse(
            receipt.getId(),
            receipt.getReceiptNumber(),
            receipt.getPurchaseOrder().getId(),
            receipt.getPurchaseOrder().getPurchaseNumber(),
            receipt.getReceivedAt(),
            receipt.getReceivedBy(),
            statusAfterReceipt,
            receipt.getNotes(),
            receipt.getItems().stream().map(this::toGoodsReceiptItemResponse).toList()
        );
    }

    public GoodsReceiptItemResponse toGoodsReceiptItemResponse(GoodsReceiptItem item) {
        return new GoodsReceiptItemResponse(
            item.getId(),
            item.getPurchaseOrderItem().getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getReceivedQuantity()
        );
    }

    public SupplierPurchaseSummaryResponse toSupplierPurchaseSummary(PurchaseOrder order) {
        return new SupplierPurchaseSummaryResponse(
            order.getId(),
            order.getPurchaseNumber(),
            order.getOrderDate().atStartOfDay().toInstant(ZoneOffset.UTC),
            order.getStatus().name(),
            order.getTotalAmount(),
            null,
            order.getExpectedDeliveryDate() == null
                ? null
                : order.getExpectedDeliveryDate().atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }

}
