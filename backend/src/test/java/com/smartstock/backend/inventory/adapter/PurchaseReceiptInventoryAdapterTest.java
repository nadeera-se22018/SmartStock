package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.inventory.repository.StockMovementRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseReceiptInventoryAdapterTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Test
    void appliesReceiptAndPersistsOneMovementForEachProduct() {
        Product product = new Product("HAMMER-01", null, "Hammer", null, BigDecimal.ONE,
            BigDecimal.TEN, BigDecimal.ZERO, null);
        ReflectionTestUtils.setField(product, "id", 3L);
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("4.0000"));
        when(productRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(3L)).thenReturn(Optional.of(balance));

        InventoryReceivingResult result = new PurchaseReceiptInventoryAdapter(
            productRepository, inventoryBalanceRepository, stockMovementRepository
        ).applyPurchaseReceipt(
            20L, 30L, 7L, List.of(new PurchaseReceiptLine(3L, new BigDecimal("2.0000"), "GR-30"))
        );

        assertEquals(new BigDecimal("6.0000"), result.resultingOnHand().get(3L));
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertEquals(new BigDecimal("2.0000"), movementCaptor.getValue().getQuantity());
        assertEquals(new BigDecimal("4.0000"), movementCaptor.getValue().getQuantityBefore());
        assertEquals(new BigDecimal("6.0000"), movementCaptor.getValue().getQuantityAfter());
        assertEquals(30L, movementCaptor.getValue().getReferenceId());
    }

    @Test
    void requiresAnExistingPurchaseTransaction() throws NoSuchMethodException {
        Transactional transaction = PurchaseReceiptInventoryAdapter.class
            .getMethod("applyPurchaseReceipt", Long.class, Long.class, Long.class, List.class)
            .getAnnotation(Transactional.class);

        assertNotNull(transaction);
        assertEquals(Propagation.MANDATORY, transaction.propagation());
    }
}
