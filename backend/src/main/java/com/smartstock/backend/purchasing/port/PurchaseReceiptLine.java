package com.smartstock.backend.purchasing.port;

import java.math.BigDecimal;

public record PurchaseReceiptLine(Long productId, BigDecimal quantity, String reference) {
}
