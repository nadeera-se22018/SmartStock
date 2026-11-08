package com.smartstock.backend.product.port;

import java.math.BigDecimal;

public interface ProductStockReader {

    BigDecimal getAvailableQuantity(Long productId);
}
