package com.smartstock.backend.inventory.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.InventorySearchCriteria;
import com.smartstock.backend.inventory.dto.InventoryStockStatus;
import com.smartstock.backend.inventory.dto.StockAdjustmentDirection;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.entity.StockMovementType;
import com.smartstock.backend.inventory.mapper.InventoryMapper;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.inventory.repository.StockMovementRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceBehaviorTest {
    private static final Long PRODUCT_ID = 7L;
    private static final Long ACTOR_ID = 41L;
    private static final String IDEMPOTENCY_KEY = "key-123456789012345678901234567890123";

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductRepository productRepository;

    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(
            inventoryBalanceRepository,
            stockMovementRepository,
            productRepository,
            new InventoryMapper()
        );

        User actor = new User("inventory-user", "inventory@example.test", "hash", "Inventory", "User");
        ReflectionTestUtils.setField(actor, "id", ACTOR_ID);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(actor, "hash", actor.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBalanceComputesAvailableAndUsesInclusiveLowStockBoundary() {
        Product product = product(new BigDecimal("5.0000"));
        InventoryBalance balance = balance(product, "5.0000", "1.0000");
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(balance));

        InventoryBalanceResponse response = service.getBalance(PRODUCT_ID);

        assertEquals(new BigDecimal("5.0000"), response.quantityOnHand());
        assertEquals(new BigDecimal("1.0000"), response.quantityReserved());
        assertEquals(new BigDecimal("4.0000"), response.availableQuantity());
        assertTrue(response.lowStock());
        assertFalse(response.outOfStock());

        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("5.0001"));
        assertFalse(service.getBalance(PRODUCT_ID).lowStock());
    }

    @Test
    void getBalanceReturnsZeroAndMarksOutOfStockWhenBalanceIsMissing() {
        Product product = product(new BigDecimal("5.0000"));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        InventoryBalanceResponse response = service.getBalance(PRODUCT_ID);

        assertEquals(new BigDecimal("0.0000"), response.quantityOnHand());
        assertEquals(new BigDecimal("0.0000"), response.quantityReserved());
        assertEquals(new BigDecimal("0.0000"), response.availableQuantity());
        assertTrue(response.lowStock());
        assertTrue(response.outOfStock());
        assertNull(response.updatedAt());
    }

    @Test
    void searchBalancesPassesLowAndOutOfStockFiltersToRepository() {
        when(inventoryBalanceRepository.search(
            nullable(String.class),
            nullable(Long.class),
            nullable(Boolean.class),
            any(InventoryStockStatus.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());

        service.searchBalances(criteria(InventoryStockStatus.LOW_STOCK));
        service.searchBalances(criteria(InventoryStockStatus.OUT_OF_STOCK));

        verify(inventoryBalanceRepository).search(
            eq(null), eq(null), eq(null), eq(InventoryStockStatus.LOW_STOCK), any(Pageable.class)
        );
        verify(inventoryBalanceRepository).search(
            eq(null), eq(null), eq(null), eq(InventoryStockStatus.OUT_OF_STOCK), any(Pageable.class)
        );
    }

    @Test
    void adjustmentInUpdatesBalanceAndRecordsAdjustmentInMovement() {
        Product product = product(new BigDecimal("5.0000"));
        InventoryBalance balance = balance(product, "4.0000", "1.0000");
        stubAdjustment(product, balance);
        stubMovementSave(501L);

        StockMovementResponse response = service.adjust(
            new StockAdjustmentRequest(
                PRODUCT_ID, StockAdjustmentDirection.IN, new BigDecimal("2.5000"), " count correction "
            ),
            IDEMPOTENCY_KEY
        ).movement();

        assertEquals(new BigDecimal("6.5000"), balance.getQuantityOnHand());
        assertEquals(StockMovementType.ADJUSTMENT_IN, response.movementType());
        assertEquals(new BigDecimal("2.5000"), response.quantity());
        assertEquals(new BigDecimal("4.0000"), response.quantityBefore());
        assertEquals(new BigDecimal("6.5000"), response.quantityAfter());
        assertEquals("INVENTORY_ADJUSTMENT", response.referenceType());
        assertNull(response.referenceId());
        assertEquals("count correction", response.reason());
        assertEquals(ACTOR_ID, response.performedByUserId());
        assertEquals("inventory-user", response.performedByUsername());

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(IDEMPOTENCY_KEY, captor.getValue().getIdempotencyKey());
        assertEquals(ACTOR_ID, captor.getValue().getPerformedBy());
    }

    @Test
    void adjustmentOutUpdatesBalanceAndRecordsAdjustmentOutMovement() {
        Product product = product(new BigDecimal("5.0000"));
        InventoryBalance balance = balance(product, "5.0000", "1.0000");
        stubAdjustment(product, balance);
        stubMovementSave(502L);

        StockMovementResponse response = service.adjust(
            new StockAdjustmentRequest(
                PRODUCT_ID, StockAdjustmentDirection.OUT, new BigDecimal("1.2500"), "count correction"
            ),
            IDEMPOTENCY_KEY
        ).movement();

        assertEquals(new BigDecimal("3.7500"), balance.getQuantityOnHand());
        assertEquals(StockMovementType.ADJUSTMENT_OUT, response.movementType());
        assertEquals(new BigDecimal("1.2500"), response.quantity());
        assertEquals(new BigDecimal("5.0000"), response.quantityBefore());
        assertEquals(new BigDecimal("3.7500"), response.quantityAfter());

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(
            captor.getValue().getQuantityBefore().subtract(captor.getValue().getQuantity()),
            captor.getValue().getQuantityAfter()
        );

        InOrder writes = inOrder(inventoryBalanceRepository, stockMovementRepository);
        writes.verify(inventoryBalanceRepository).save(balance);
        writes.verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void adjustmentOutRejectsNegativeStockWithoutSavingBalanceOrMovement() {
        Product product = product(new BigDecimal("5.0000"));
        InventoryBalance balance = balance(product, "1.0000", "0.0000");
        stubAdjustment(product, balance);

        assertThrows(BusinessRuleException.class, () -> service.adjust(
            new StockAdjustmentRequest(
                PRODUCT_ID, StockAdjustmentDirection.OUT, new BigDecimal("1.0001"), "correction"
            ),
            IDEMPOTENCY_KEY
        ));

        assertEquals(new BigDecimal("1.0000"), balance.getQuantityOnHand());
        verify(inventoryBalanceRepository, never()).save(any(InventoryBalance.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void adjustmentOutRejectsConsumingReservedStock() {
        Product product = product(new BigDecimal("5.0000"));
        InventoryBalance balance = balance(product, "5.0000", "2.0000");
        stubAdjustment(product, balance);

        assertThrows(BusinessRuleException.class, () -> service.adjust(
            new StockAdjustmentRequest(
                PRODUCT_ID, StockAdjustmentDirection.OUT, new BigDecimal("3.0001"), "correction"
            ),
            IDEMPOTENCY_KEY
        ));

        assertEquals(new BigDecimal("5.0000"), balance.getQuantityOnHand());
        verify(inventoryBalanceRepository, never()).save(any(InventoryBalance.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    private void stubAdjustment(Product product, InventoryBalance balance) {
        when(stockMovementRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(stockMovementRepository.findByIdempotencyKeyForUpdate(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(balance));
    }

    private void stubMovementSave(Long movementId) {
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", movementId);
            return movement;
        });
    }

    private InventorySearchCriteria criteria(InventoryStockStatus status) {
        return new InventorySearchCriteria(null, null, null, status, 0, 20, null);
    }

    private Product product(BigDecimal reorderLevel) {
        Product product = new Product(
            "STOCK-7", null, "Stock item", null, BigDecimal.ONE, BigDecimal.TEN, reorderLevel, null
        );
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    private InventoryBalance balance(Product product, String onHand, String reserved) {
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal(onHand));
        ReflectionTestUtils.setField(balance, "quantityReserved", new BigDecimal(reserved));
        return balance;
    }
}