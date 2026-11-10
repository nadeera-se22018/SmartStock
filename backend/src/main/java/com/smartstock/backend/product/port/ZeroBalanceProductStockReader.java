package com.smartstock.backend.product.port;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ZeroBalanceProductStockReader implements ProductStockReader {
    @Override
    public BigDecimal getAvailableQuantity(Long productId) {
        return BigDecimal.ZERO;
    }
}
