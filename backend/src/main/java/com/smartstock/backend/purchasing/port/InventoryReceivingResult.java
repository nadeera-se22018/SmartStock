package com.smartstock.backend.purchasing.port;

import java.math.BigDecimal;
import java.util.Map;

public record InventoryReceivingResult(Map<Long, BigDecimal> resultingOnHand) {
    public InventoryReceivingResult {
        resultingOnHand = Map.copyOf(resultingOnHand);
    }
}
