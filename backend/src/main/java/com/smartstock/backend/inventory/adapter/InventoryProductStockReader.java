package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.product.port.ProductStockReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class InventoryProductStockReader implements ProductStockReader {
    private final InventoryBalanceRepository inventoryBalanceRepository;

    public InventoryProductStockReader(InventoryBalanceRepository inventoryBalanceRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(Long productId) {
        return inventoryBalanceRepository.findByProductId(productId)
            .map(balance -> balance.getQuantityOnHand().subtract(balance.getQuantityReserved()))
            .orElse(BigDecimal.ZERO);
    }
}
