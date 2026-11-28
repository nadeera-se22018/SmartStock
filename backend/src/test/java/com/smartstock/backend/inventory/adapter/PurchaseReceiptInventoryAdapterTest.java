package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.inventory.service.InventoryService;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseReceiptInventoryAdapterTest {
    @Mock
    private InventoryService inventoryService;

    @Test
    void delegatesReceiptMutationToInventoryApplicationService() {
        List<PurchaseReceiptLine> lines = List.of(
            new PurchaseReceiptLine(3L, new BigDecimal("2.0000"), "GR-30")
        );
        InventoryReceivingResult expected = new InventoryReceivingResult(
            Map.of(3L, new BigDecimal("6.0000"))
        );
        when(inventoryService.applyPurchaseReceipt(20L, 30L, 7L, lines)).thenReturn(expected);

        InventoryReceivingResult result = new PurchaseReceiptInventoryAdapter(inventoryService)
            .applyPurchaseReceipt(20L, 30L, 7L, lines);

        assertEquals(expected, result);
        verify(inventoryService).applyPurchaseReceipt(20L, 30L, 7L, lines);
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
