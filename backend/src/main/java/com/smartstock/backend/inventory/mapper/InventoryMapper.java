package com.smartstock.backend.inventory.mapper;

import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.repository.InventoryBalanceProjection;
import com.smartstock.backend.inventory.repository.StockMovementProjection;
import com.smartstock.backend.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryMapper {
    public InventoryBalanceResponse toResponse(InventoryBalanceProjection balance) {
        BigDecimal onHand = balance.getQuantityOnHand();
        BigDecimal reserved = balance.getQuantityReserved();
        return new InventoryBalanceResponse(
            balance.getProductId(),
            balance.getSku(),
            balance.getBarcode(),
            balance.getProductName(),
            onHand,
            reserved,
            balance.getAvailableQuantity(),
            balance.getReorderLevel(),
            onHand.compareTo(balance.getReorderLevel()) <= 0,
            onHand.signum() == 0,
            balance.getUpdatedAt()
        );
    }

    public InventoryBalanceResponse toResponse(Product product, InventoryBalance balance) {
        BigDecimal onHand = balance == null ? BigDecimal.ZERO.setScale(4) : balance.getQuantityOnHand();
        BigDecimal reserved = balance == null ? BigDecimal.ZERO.setScale(4) : balance.getQuantityReserved();
        return new InventoryBalanceResponse(
            product.getId(),
            product.getSku(),
            product.getBarcode(),
            product.getName(),
            onHand,
            reserved,
            onHand.subtract(reserved),
            product.getReorderLevel(),
            onHand.compareTo(product.getReorderLevel()) <= 0,
            onHand.signum() == 0,
            balance == null ? null : balance.getUpdatedAt()
        );
    }

    public StockMovementResponse toResponse(StockMovementProjection movement) {
        return new StockMovementResponse(
            movement.getId(),
            movement.getProductId(),
            movement.getSku(),
            movement.getProductName(),
            movement.getMovementType(),
            movement.getQuantity(),
            movement.getQuantityBefore(),
            movement.getQuantityAfter(),
            movement.getReferenceType(),
            movement.getReferenceId(),
            movement.getReason(),
            movement.getPerformedByUserId(),
            movement.getPerformedByUsername(),
            movement.getCreatedAt()
        );
    }

    public StockMovementResponse toResponse(StockMovement movement, String username) {
        return new StockMovementResponse(
            movement.getId(),
            movement.getProduct().getId(),
            movement.getProduct().getSku(),
            movement.getProduct().getName(),
            movement.getMovementType(),
            movement.getQuantity(),
            movement.getQuantityBefore(),
            movement.getQuantityAfter(),
            movement.getReferenceType(),
            movement.getReferenceId(),
            movement.getReason(),
            movement.getPerformedBy(),
            username,
            movement.getCreatedAt()
        );
    }
}
