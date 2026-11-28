package com.smartstock.backend.inventory.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.InvalidRequestException;
import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.StockAdjustmentDirection;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.entity.StockMovementType;
import com.smartstock.backend.inventory.mapper.InventoryMapper;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.inventory.repository.StockMovementProjection;
import com.smartstock.backend.inventory.repository.StockMovementRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryMapper inventoryMapper;

    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(
            inventoryBalanceRepository, stockMovementRepository, productRepository, inventoryMapper
        );
        User actor = new User("inventory-user", "inventory@example.test", "hash", "Inventory", "User");
        ReflectionTestUtils.setField(actor, "id", 41L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(actor, "hash", actor.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adjustmentInWritesPositiveMovementUsingAuthenticatedActor() {
        Product product = product();
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("4.0000"));
        when(stockMovementRepository.findByIdempotencyKey("key-123456789012345678901234567890123")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(7L)).thenReturn(Optional.of(balance));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 12L);
            return movement;
        });

        service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.IN, new BigDecimal("2.5000"), " count correction "),
            "key-123456789012345678901234567890123"
        );

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertEquals(41L, movement.getValue().getPerformedBy());
        assertEquals(StockMovementType.ADJUSTMENT_IN, movement.getValue().getMovementType());
        assertEquals(new BigDecimal("4.0000"), movement.getValue().getQuantityBefore());
        assertEquals(new BigDecimal("6.5000"), movement.getValue().getQuantityAfter());
        assertEquals("count correction", movement.getValue().getReason());
        assertEquals(new BigDecimal("6.5000"), balance.getQuantityOnHand());
    }

    @Test
    void adjustmentOutCannotConsumeReservedStock() {
        Product product = product();
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("4.0000"));
        ReflectionTestUtils.setField(balance, "quantityReserved", new BigDecimal("2.0000"));
        when(stockMovementRepository.findByIdempotencyKey("key-123456789012345678901234567890123")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(7L)).thenReturn(Optional.of(balance));

        assertThrows(BusinessRuleException.class, () -> service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.OUT, new BigDecimal("2.0001"), "correction"),
            "key-123456789012345678901234567890123"
        ));

        assertEquals(new BigDecimal("4.0000"), balance.getQuantityOnHand());
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void rejectsQuantityWithMoreThanFourFractionalDigitsWithoutAccessingRepositories() {
        assertThrows(InvalidRequestException.class, () -> service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.IN, new BigDecimal("1.00001"), "correction"),
            "key-123456789012345678901234567890123"
        ));

        verify(productRepository, never()).findByIdForUpdate(7L);
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void balanceReadReturnsZeroWhenNoBalanceRowExists() {
        Product product = product();
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductId(7L)).thenReturn(Optional.empty());
        InventoryServiceImpl readService = new InventoryServiceImpl(
            inventoryBalanceRepository,
            stockMovementRepository,
            productRepository,
            new InventoryMapper()
        );

        InventoryBalanceResponse response = readService.getBalance(7L);

        assertEquals(new BigDecimal("0.0000"), response.quantityOnHand());
        assertEquals(new BigDecimal("0.0000"), response.quantityReserved());
        assertEquals(new BigDecimal("0.0000"), response.availableQuantity());
        assertNull(response.updatedAt());
    }

    @Test
    void adjustmentOutCreatesMovementWithCorrectEquation() {
        Product product = product();
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("5.0000"));
        when(stockMovementRepository.findByIdempotencyKey("key-123456789012345678901234567890123"))
            .thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(7L)).thenReturn(Optional.of(balance));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 13L);
            return movement;
        });

        service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.OUT, new BigDecimal("1.2500"), "count correction"),
            "key-123456789012345678901234567890123"
        );

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertEquals(StockMovementType.ADJUSTMENT_OUT, movement.getValue().getMovementType());
        assertEquals(new BigDecimal("5.0000"), movement.getValue().getQuantityBefore());
        assertEquals(new BigDecimal("3.7500"), movement.getValue().getQuantityAfter());
        assertEquals(new BigDecimal("3.7500"), balance.getQuantityOnHand());
    }

    @Test
    void purchaseReceiptAggregatesProductsAndLocksThemInAscendingOrder() {
        Product product3 = product(3L);
        Product product7 = product(7L);
        when(productRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(product3));
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product7));
        when(inventoryBalanceRepository.findByProductIdForUpdate(3L)).thenReturn(Optional.empty());
        when(inventoryBalanceRepository.findByProductIdForUpdate(7L)).thenReturn(Optional.empty());
        when(inventoryBalanceRepository.save(any(InventoryBalance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryReceivingResult result = service.applyPurchaseReceipt(
            20L,
            30L,
            41L,
            List.of(
                new PurchaseReceiptLine(7L, new BigDecimal("1.0000"), "GR-30"),
                new PurchaseReceiptLine(3L, new BigDecimal("2.0000"), "GR-30"),
                new PurchaseReceiptLine(7L, new BigDecimal("3.0000"), "GR-30")
            )
        );

        assertEquals(Map.of(3L, new BigDecimal("2.0000"), 7L, new BigDecimal("4.0000")),
            result.resultingOnHand());
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(movementCaptor.capture());
        assertEquals(List.of(3L, 7L), movementCaptor.getAllValues().stream()
            .map(movement -> movement.getProduct().getId()).toList());
        assertEquals(new BigDecimal("4.0000"), movementCaptor.getAllValues().get(1).getQuantity());
        assertEquals(StockMovementType.PURCHASE_RECEIPT, movementCaptor.getAllValues().getFirst().getMovementType());

        InOrder locks = inOrder(productRepository);
        locks.verify(productRepository).findByIdForUpdate(3L);
        locks.verify(productRepository).findByIdForUpdate(7L);
    }

    @Test
    void idempotencyReplayReturnsOriginalMovementWithoutMutatingAgain() {
        Product product = product();
        InventoryBalance balance = new InventoryBalance(product);
        ReflectionTestUtils.setField(balance, "quantityOnHand", new BigDecimal("4.0000"));
        AtomicReference<StockMovement> persistedMovement = new AtomicReference<>();
        when(stockMovementRepository.findByIdempotencyKey("key-123456789012345678901234567890123"))
            .thenAnswer(invocation -> Optional.ofNullable(persistedMovement.get()));
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(inventoryBalanceRepository.findByProductIdForUpdate(7L)).thenReturn(Optional.of(balance));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 14L);
            persistedMovement.set(movement);
            return movement;
        });
        when(stockMovementRepository.findProjectionById(14L)).thenReturn(Optional.empty());
        StockMovementProjection projection = mock(StockMovementProjection.class);
        StockMovementResponse expected = new StockMovementResponse(
            14L, 7L, "STOCK-7", "Stock item", StockMovementType.ADJUSTMENT_IN,
            new BigDecimal("2.0000"), new BigDecimal("4.0000"), new BigDecimal("6.0000"),
            "INVENTORY_ADJUSTMENT", null, "recount", 41L, "inventory-user", null
        );
        when(stockMovementRepository.findProjectionByIdempotencyKey(
            "key-123456789012345678901234567890123"
        )).thenReturn(Optional.of(projection));
        when(inventoryMapper.toResponse(projection)).thenReturn(expected);
        when(inventoryMapper.toResponse(any(StockMovement.class), eq("inventory-user"))).thenReturn(expected);

        InventoryAdjustmentResult first = service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.IN, new BigDecimal("2.0000"), "recount"),
            "key-123456789012345678901234567890123"
        );
        InventoryAdjustmentResult replay = service.adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.IN, new BigDecimal("2.0000"), "recount"),
            "key-123456789012345678901234567890123"
        );

        assertEquals(false, first.replayed());
        assertEquals(true, replay.replayed());
        assertEquals(expected, replay.movement());
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
        verify(productRepository, times(1)).findByIdForUpdate(7L);
    }

    private Product product() {
        return product(7L);
    }

    private Product product(Long id) {
        Product product = new Product("STOCK-7", null, "Stock item", null, BigDecimal.ONE,
            BigDecimal.TEN, BigDecimal.ZERO, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
