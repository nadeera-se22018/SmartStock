package com.smartstock.backend.purchasing.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.auth.repository.UserRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.purchasing.dto.GoodsReceiptCreateRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptItemRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseCreateRequest;
import com.smartstock.backend.purchasing.dto.PurchaseItemRequest;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseStatus;
import com.smartstock.backend.purchasing.entity.GoodsReceipt;
import com.smartstock.backend.purchasing.entity.PurchaseOrder;
import com.smartstock.backend.purchasing.entity.PurchaseOrderItem;
import com.smartstock.backend.purchasing.mapper.PurchaseMapper;
import com.smartstock.backend.purchasing.port.InventoryReceivingPort;
import com.smartstock.backend.purchasing.repository.GoodsReceiptRepository;
import com.smartstock.backend.purchasing.repository.PurchaseOrderRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.supplier.entity.Supplier;
import com.smartstock.backend.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PurchaseMapper purchaseMapper;

    @Mock
    private InventoryReceivingPort inventoryReceivingPort;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Supplier supplier;
    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        supplier = new Supplier("Acme Supplies", "acme supplies", "Jane",
            "jane@acme.com", "555-0100", "Main Street");
        ReflectionTestUtils.setField(supplier, "id", 1L);
        product = new Product("HAMMER-01", null, "Hammer", null, new BigDecimal("5.0000"),
            new BigDecimal("10.0000"), BigDecimal.ZERO, null);
        ReflectionTestUtils.setField(product, "id", 3L);
        user = new User("manager", "manager@smartstock.local", "hash", "Manager", null);
        ReflectionTestUtils.setField(user, "id", 7L);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("manager", "n/a", List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void receivingOwnsTheTransactionBoundary() throws NoSuchMethodException {
        Transactional transaction = PurchaseServiceImpl.class
            .getMethod("receiveGoods", Long.class, GoodsReceiptCreateRequest.class, String.class)
            .getAnnotation(Transactional.class);

        assertTrue(transaction != null);
        assertEquals(Propagation.REQUIRED, transaction.propagation());
    }

    @Test
    void createCalculatesTotalsAndStartsInDraftWithoutReceivingInventory() {
        PurchaseResponse expected = new PurchaseResponse(
            1L, "PO-2026-test", 2L, "Acme Supplies", LocalDate.of(2026, 11, 25),
            null, PurchaseStatus.DRAFT, null, new BigDecimal("30.0000"),
            new BigDecimal("30.0000"), List.of(), null, null
        );
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(supplierRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(purchaseMapper.toPurchaseResponse(any(PurchaseOrder.class))).thenReturn(expected);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseResponse result = purchaseService.create(new PurchaseCreateRequest(
            2L, LocalDate.of(2026, 11, 25), null, "Restock",
            List.of(new PurchaseItemRequest(3L, new BigDecimal("3.0000"), new BigDecimal("10.0000")))
        ));

        assertSame(expected, result);
        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(captor.capture());
        assertEquals(PurchaseStatus.DRAFT, captor.getValue().getStatus());
        assertEquals(new BigDecimal("30.0000"), captor.getValue().getSubTotal());
        assertEquals(new BigDecimal("30.0000"), captor.getValue().getTotalAmount());
        verify(inventoryReceivingPort, never()).applyPurchaseReceipt(any(), any(), any(), any());
    }

    @Test
    void submitRejectsDraftWithoutItems() {
        PurchaseOrder order = new PurchaseOrder("PO-2026-test", supplier, 7L,
            LocalDate.of(2026, 11, 25), null, null);
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class, () -> purchaseService.submit(1L)
        );

        assertEquals("PURCHASE_SUBMIT_REQUIRES_ITEMS", exception.getCode());
        assertEquals(PurchaseStatus.DRAFT, order.getStatus());
    }

    @Test
    void submitMovesDraftToSubmitted() {
        PurchaseOrder order = draftOrderWithItem();
        PurchaseResponse expected = new PurchaseResponse(
            1L, "PO-2026-test", 1L, "Acme Supplies", order.getOrderDate(),
            null, PurchaseStatus.SUBMITTED, null, new BigDecimal("50.0000"),
            new BigDecimal("50.0000"), List.of(), null, null
        );
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseMapper.toPurchaseResponse(order)).thenReturn(expected);

        assertSame(expected, purchaseService.submit(1L));
        assertEquals(PurchaseStatus.SUBMITTED, order.getStatus());
    }

    @Test
    void receivingPartialQuantitiesUpdatesOrderAndCallsInventoryOnce() {
        PurchaseOrder order = submittedOrder();
        GoodsReceiptResponse expected = new GoodsReceiptResponse(
            9L, "GR-2026-test", 1L, "PO-2026-test", Instant.parse("2026-11-25T10:00:00Z"),
            "manager", PurchaseStatus.PARTIALLY_RECEIVED, "Dock 2", List.of()
        );
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class)))
            .thenAnswer(invocation -> {
                GoodsReceipt receipt = invocation.getArgument(0);
                ReflectionTestUtils.setField(receipt, "id", 9L);
                return receipt;
            });
        when(purchaseMapper.toGoodsReceiptResponse(any(GoodsReceipt.class), eq(PurchaseStatus.PARTIALLY_RECEIVED)))
            .thenReturn(expected);

        GoodsReceiptResponse result = purchaseService.receiveGoods(1L, new GoodsReceiptCreateRequest(
            Instant.parse("2026-11-25T10:00:00Z"), "Dock 2",
            List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("2.0000")))
        ), null);

        assertSame(expected, result);
        assertEquals(PurchaseStatus.PARTIALLY_RECEIVED, order.getStatus());
        assertEquals(new BigDecimal("2.0000"), order.getItems().getFirst().getReceivedQuantity());
        verify(inventoryReceivingPort).applyPurchaseReceipt(eq(1L), eq(9L), eq(7L), any());
    }

    @Test
    void receivingMoreThanRemainingQuantityDoesNotCreateReceiptOrMutateInventory() {
        PurchaseOrder order = submittedOrder();
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> purchaseService.receiveGoods(1L, new GoodsReceiptCreateRequest(
                null, null, List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("11.0000")))
            ), null)
        );

        assertEquals("PURCHASE_RECEIPT_QUANTITY_EXCEEDED", exception.getCode());
        verify(goodsReceiptRepository, never()).saveAndFlush(any());
        verify(inventoryReceivingPort, never()).applyPurchaseReceipt(any(), any(), any(), any());
        assertEquals(BigDecimal.ZERO.setScale(4), order.getItems().getFirst().getReceivedQuantity());
    }

    @Test
    void inventoryFailureLeavesOrderQuantitiesAndStatusUnchanged() {
        PurchaseOrder order = submittedOrder();
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class)))
            .thenAnswer(invocation -> {
                GoodsReceipt receipt = invocation.getArgument(0);
                ReflectionTestUtils.setField(receipt, "id", 9L);
                return receipt;
            });
        when(inventoryReceivingPort.applyPurchaseReceipt(any(), any(), any(), any()))
            .thenThrow(new BusinessRuleException("INVENTORY_CONFLICT", "Inventory write failed."));

        assertThrows(BusinessRuleException.class, () -> purchaseService.receiveGoods(
            1L,
            new GoodsReceiptCreateRequest(
                null, null, List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("2.0000")))
            ),
            null
        ));

        assertEquals(PurchaseStatus.SUBMITTED, order.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(4), order.getItems().getFirst().getReceivedQuantity());
    }

    @Test
    void replayingSameIdempotencyKeyReturnsReceiptWithoutCallingInventoryAgain() {
        PurchaseOrder order = submittedOrder();
        String key = "123e4567-e89b-12d3-a456-426614174000";
        GoodsReceiptResponse expected = new GoodsReceiptResponse(
            9L, "GR-2026-test", 1L, "PO-2026-test", Instant.parse("2026-11-25T10:00:00Z"),
            "manager", PurchaseStatus.PARTIALLY_RECEIVED, "Dock 2", List.of()
        );
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(goodsReceiptRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class)))
            .thenAnswer(invocation -> {
                GoodsReceipt receipt = invocation.getArgument(0);
                ReflectionTestUtils.setField(receipt, "id", 9L);
                return receipt;
            });
        when(purchaseMapper.toGoodsReceiptResponse(any(GoodsReceipt.class), eq(PurchaseStatus.PARTIALLY_RECEIVED)))
            .thenReturn(expected);

        GoodsReceiptResponse firstResponse = purchaseService.receiveGoods(1L, new GoodsReceiptCreateRequest(
            Instant.parse("2026-11-25T10:00:00Z"), "Dock 2",
            List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("2.0000")))
        ), key);

        ArgumentCaptor<GoodsReceipt> receiptCaptor = ArgumentCaptor.forClass(GoodsReceipt.class);
        verify(goodsReceiptRepository).saveAndFlush(receiptCaptor.capture());
        GoodsReceipt savedReceipt = receiptCaptor.getValue();
        when(goodsReceiptRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(savedReceipt));
        when(purchaseMapper.toGoodsReceiptResponse(savedReceipt)).thenReturn(expected);

        GoodsReceiptResponse replay = purchaseService.receiveGoods(1L, new GoodsReceiptCreateRequest(
            Instant.parse("2026-11-25T10:00:00Z"), "Dock 2",
            List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("2.0000")))
        ), key);

        assertSame(expected, firstResponse);
        assertTrue(replay.replayed());
        verify(inventoryReceivingPort,
            org.mockito.Mockito.times(1)).applyPurchaseReceipt(any(), any(), any(), any());
    }

    @Test
    void finalReceiptMovesOrderToReceived() {
        PurchaseOrder order = submittedOrder();
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class)))
            .thenAnswer(invocation -> {
                GoodsReceipt receipt = invocation.getArgument(0);
                ReflectionTestUtils.setField(receipt, "id", 9L);
                return receipt;
            });
        when(purchaseMapper.toGoodsReceiptResponse(any(GoodsReceipt.class), eq(PurchaseStatus.RECEIVED)))
            .thenReturn(new GoodsReceiptResponse(
                9L, "GR-2026-test", 1L, "PO-2026-test", Instant.now(),
                "manager", PurchaseStatus.RECEIVED, null, List.of()
            ));

        GoodsReceiptResponse response = purchaseService.receiveGoods(1L, new GoodsReceiptCreateRequest(
            null, null, List.of(new GoodsReceiptItemRequest(11L, new BigDecimal("10.0000")))
        ), null);

        assertEquals(PurchaseStatus.RECEIVED, response.statusAfterReceipt());
        assertEquals(PurchaseStatus.RECEIVED, order.getStatus());
    }

    private PurchaseOrder draftOrderWithItem() {
        PurchaseOrder order = new PurchaseOrder("PO-2026-test", supplier, 7L,
            LocalDate.of(2026, 11, 25), null, null);
        PurchaseOrderItem item = new PurchaseOrderItem(
            order, product, new BigDecimal("10.0000"), new BigDecimal("5.0000")
        );
        ReflectionTestUtils.setField(item, "id", 11L);
        order.addItem(item);
        order.setSubtotal(new BigDecimal("50.0000"));
        ReflectionTestUtils.setField(order, "id", 1L);
        return order;
    }

    private PurchaseOrder submittedOrder() {
        PurchaseOrder order = draftOrderWithItem();
        order.submit();
        return order;
    }
}
