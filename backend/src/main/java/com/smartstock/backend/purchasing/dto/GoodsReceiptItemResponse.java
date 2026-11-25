package com.smartstock.backend.purchasing.dto;

import java.math.BigDecimal;

public record GoodsReceiptItemResponse(
    Long id,
    Long purchaseOrderItemId,
    Long productId,
    String productName,
    BigDecimal receivedQuantity
) {
}
