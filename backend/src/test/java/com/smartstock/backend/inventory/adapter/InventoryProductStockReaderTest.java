package com.smartstock.backend.inventory.adapter;

import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryProductStockReaderTest {
    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Test
    void returnsAvailableQuantityAfterReservations() {
        Product product = new Product("HAMMER-01", null, "Hammer", null, BigDecimal.ONE,
            BigDecimal.TEN, BigDecimal.ZERO, null);
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("9.0000"));
        ReflectionTestUtils.setField(balance, "quantityReserved", new BigDecimal("2.0000"));
        when(inventoryBalanceRepository.findByProductId(3L)).thenReturn(Optional.of(balance));

        BigDecimal available = new InventoryProductStockReader(inventoryBalanceRepository).getAvailableQuantity(3L);

        assertEquals(new BigDecimal("7.0000"), available);
    }

    @Test
    void treatsMissingBalanceAsZero() {
        when(inventoryBalanceRepository.findByProductId(3L)).thenReturn(Optional.empty());

        assertEquals(BigDecimal.ZERO,
            new InventoryProductStockReader(inventoryBalanceRepository).getAvailableQuantity(3L));
    }
}
