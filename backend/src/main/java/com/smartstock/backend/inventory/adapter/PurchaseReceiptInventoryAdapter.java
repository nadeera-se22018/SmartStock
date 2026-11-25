package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.inventory.repository.StockMovementRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.purchasing.port.InventoryReceivingPort;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseReceiptInventoryAdapter implements InventoryReceivingPort {
    private final ProductRepository productRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;

    public PurchaseReceiptInventoryAdapter(
        ProductRepository productRepository,
        InventoryBalanceRepository inventoryBalanceRepository,
        StockMovementRepository stockMovementRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        Long receivedByUserId,
        List<PurchaseReceiptLine> lines
    ) {
        Map<Long, Product> products = new LinkedHashMap<>();
        for (PurchaseReceiptLine line : lines.stream()
            .sorted(java.util.Comparator.comparing(PurchaseReceiptLine::productId)).toList()) {
            Product product = productRepository.findByIdForUpdate(line.productId()).orElseThrow(() ->
                new ResourceNotFoundException(
                    "PRODUCT_NOT_FOUND", "Product with id '" + line.productId() + "' was not found."
                ));
            if (!product.isActive()) {
                throw new BusinessRuleException(
                    "PURCHASE_PRODUCT_INACTIVE", "Goods cannot be received for an inactive product."
                );
            }
            products.put(line.productId(), product);
        }

        Map<Long, BigDecimal> resultingOnHand = new LinkedHashMap<>();
        for (PurchaseReceiptLine line : lines.stream()
            .sorted(java.util.Comparator.comparing(PurchaseReceiptLine::productId)).toList()) {
            Product product = products.get(line.productId());
            InventoryBalance balance = inventoryBalanceRepository.findByProductIdForUpdate(line.productId())
                .orElseGet(() -> inventoryBalanceRepository.save(new InventoryBalance(product)));
            BigDecimal before = balance.getQuantityOnHand();
            BigDecimal after = balance.increaseOnHand(line.quantity());
            inventoryBalanceRepository.save(balance);
            stockMovementRepository.save(new StockMovement(
                product, receivedByUserId, line.quantity(), before, after, goodsReceiptId, purchaseOrderId
            ));
            resultingOnHand.put(line.productId(), after);
        }
        return new InventoryReceivingResult(resultingOnHand);
    }
}
