package com.smartstock.backend.inventory.service;

import com.smartstock.backend.inventory.dto.StockMovementResponse;

public record InventoryAdjustmentResult(StockMovementResponse movement, boolean replayed) {
}
