package com.smartstock.backend.purchasing.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.auth.repository.UserRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
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
import com.smartstock.backend.supplier.entity.Supplier;
import com.smartstock.backend.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceWorkflowTest {

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
        supplier = new Supplier(
            "Acme Supplies", "acme supplies", "Jane",
            "jane@acme.com", "555-0100", "Main Street"
        );
        ReflectionTestUtils.setField(supplier, "id", 1L);

        product = new Product(
            "HAMMER-01", null, "Hammer", null,
            new BigDecimal("5.0000"), new BigDecimal("10.0000"),
            BigDecimal.ZERO, null
        );
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
    void createSavesDraftWithCalculatedTotalsAndDoesNotMutateInventory() {
        PurchaseResponse expected = new PurchaseResponse(
            1L, "PO-2026-test", 1L, "Acme Supplies",
            LocalDate.of(2026, 11, 25), null, PurchaseStatus.DRAFT, "Restock",
            new BigDecimal("30.0000"), new BigDecimal("30.0000"),
            List.of(), null, null
        );

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseMapper.toPurchaseResponse(any(PurchaseOrder.class))).thenReturn(expected);

        PurchaseResponse result = purchaseService.create(new PurchaseCreateRequest(
            1L,
            LocalDate.of(2026, 11, 25),
            null,
            "Restock",
            List.of(new PurchaseItemRequest(
                3L, new BigDecimal("3.0000"), new BigDecimal("10.0000")
            ))
        ));

        assertSame(expected, result);
        org.mockito.ArgumentCaptor<PurchaseOrder> savedOrder =
            org.mockito.ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(savedOrder.capture());

        PurchaseOrder order = savedOrder.getValue();
        assertEquals(PurchaseStatus.DRAFT, order.getStatus());
        assertEquals(new BigDecimal("30.0000"), order.getSubTotal());
        assertEquals(new BigDecimal("30.0000"), order.getTotalAmount());
        assertEquals(new BigDecimal("30.0000"), order.getItems().get(0).getLineTotal());
        assertEquals(BigDecimal.ZERO.setScale(4), order.getItems().get(0).getReceivedQuantity());
        verify(inventoryReceivingPort, never()).applyPurchaseReceipt(any(), any(), any(), any());
    }

    @Test
    void submitTransitionsDraftToSubmitted() {
        PurchaseOrder order = orderWithItem();
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(purchaseMapper.toPurchaseResponse(order)).thenReturn(
            new PurchaseResponse(
                1L, "PO-2026-test", 1L, "Acme Supplies", order.getOrderDate(),
                null, PurchaseStatus.SUBMITTED, null,
                new BigDecimal("50.0000"), new BigDecimal("50.0000"),
                List.of(), null, null
            )
        );

        PurchaseResponse response = purchaseService.submit(1L);

        assertEquals(PurchaseStatus.SUBMITTED, order.getStatus());
        assertEquals(PurchaseStatus.SUBMITTED, response.status());
        verify(inventoryReceivingPort, never()).applyPurchaseReceipt(any(), any(), any(), any());
    }

    @Test
    void partialThenFinalReceiptTransitionsSubmittedThroughPartiallyReceivedToReceived() {
        PurchaseOrder order = orderWithItem();
        order.submit();
        stubReceiving(order);

        GoodsReceiptResponse partialResponse = receiptResponse(
            101L, PurchaseStatus.PARTIALLY_RECEIVED
        );
        GoodsReceiptResponse finalResponse = receiptResponse(
            102L, PurchaseStatus.RECEIVED
        );

        when(purchaseMapper.toGoodsReceiptResponse(
            any(GoodsReceipt.class), eq(PurchaseStatus.PARTIALLY_RECEIVED)
        )).thenReturn(partialResponse);
        when(purchaseMapper.toGoodsReceiptResponse(
            any(GoodsReceipt.class), eq(PurchaseStatus.RECEIVED)
        )).thenReturn(finalResponse);

        GoodsReceiptResponse firstReceipt = purchaseService.receiveGoods(
            1L,
            receiptRequest(new BigDecimal("3.0000")),
            null
        );

        assertSame(partialResponse, firstReceipt);
        assertEquals(PurchaseStatus.PARTIALLY_RECEIVED, order.getStatus());
        assertEquals(new BigDecimal("3.0000"), order.getItems().get(0).getReceivedQuantity());

        GoodsReceiptResponse secondReceipt = purchaseService.receiveGoods(
            1L,
            receiptRequest(new BigDecimal("7.0000")),
            null
        );

        assertSame(finalResponse, secondReceipt);
        assertEquals(PurchaseStatus.RECEIVED, order.getStatus());
        assertEquals(new BigDecimal("10.0000"), order.getItems().get(0).getReceivedQuantity());
        verify(goodsReceiptRepository, times(2)).saveAndFlush(any(GoodsReceipt.class));
        verify(inventoryReceivingPort, times(2)).applyPurchaseReceipt(any(), any(), any(), any());
    }

    @Test
    void receivingGoodsForDraftOrderIsRejectedWithoutSideEffects() {
        PurchaseOrder order = orderWithItem();
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> purchaseService.receiveGoods(
                1L, receiptRequest(new BigDecimal("2.0000")), null
            )
        );

        assertEquals("PURCHASE_STATUS_TRANSITION_NOT_ALLOWED", exception.getCode());
        assertEquals(PurchaseStatus.DRAFT, order.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(4), order.getItems().get(0).getReceivedQuantity());
        verify(goodsReceiptRepository, never()).saveAndFlush(any(GoodsReceipt.class));
        verify(inventoryReceivingPort, never()).applyPurchaseReceipt(any(), any(), any(), any());
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void submittingAnAlreadySubmittedOrderIsRejected() {
        PurchaseOrder order = orderWithItem();
        order.submit();

        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> purchaseService.submit(1L)
        );

        assertEquals("PURCHASE_STATUS_TRANSITION_NOT_ALLOWED", exception.getCode());
        assertEquals(PurchaseStatus.SUBMITTED, order.getStatus());
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void cancellingPartiallyReceivedOrReceivedOrderIsRejected() {
        for (PurchaseStatus terminalForCancellation : List.of(
            PurchaseStatus.PARTIALLY_RECEIVED, PurchaseStatus.RECEIVED
        )) {
            PurchaseOrder order = orderWithItem();
            order.submit();
            order.recordReceipt(Map.of(
                order.getItems().get(0).getId(),
                terminalForCancellation == PurchaseStatus.RECEIVED
                    ? new BigDecimal("10.0000")
                    : new BigDecimal("2.0000")
            ));

            when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
            when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

            BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> purchaseService.cancel(1L)
            );

            assertEquals("PURCHASE_STATUS_TRANSITION_NOT_ALLOWED", exception.getCode());
            assertEquals(terminalForCancellation, order.getStatus());
            verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));

            org.mockito.Mockito.reset(purchaseOrderRepository);
        }
    }

    private PurchaseOrder orderWithItem() {
        PurchaseOrder order = new PurchaseOrder(
            "PO-2026-test", supplier, 7L,
            LocalDate.of(2026, 11, 25), null, null
        );
        PurchaseOrderItem item = new PurchaseOrderItem(
            order, product, new BigDecimal("10.0000"), new BigDecimal("5.0000")
        );
        ReflectionTestUtils.setField(item, "id", 11L);
        order.addItem(item);
        order.setSubtotal(new BigDecimal("50.0000"));
        ReflectionTestUtils.setField(order, "id", 1L);
        return order;
    }

    private void stubReceiving(PurchaseOrder order) {
        when(purchaseOrderRepository.findLockedById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(order)).thenReturn(order);
        when(supplierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(user));

        AtomicLong receiptIds = new AtomicLong(100L);
        when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class)))
            .thenAnswer(invocation -> {
                GoodsReceipt receipt = invocation.getArgument(0);
                ReflectionTestUtils.setField(receipt, "id", receiptIds.incrementAndGet());
                return receipt;
            });
    }

    private GoodsReceiptCreateRequest receiptRequest(BigDecimal quantity) {
        return new GoodsReceiptCreateRequest(
            Instant.parse("2026-11-25T10:00:00Z"),
            "Dock 2",
            List.of(new GoodsReceiptItemRequest(11L, quantity))
        );
    }

    private GoodsReceiptResponse receiptResponse(Long id, PurchaseStatus status) {
        return new GoodsReceiptResponse(
            id, "GR-2026-test-" + id, 1L, "PO-2026-test",
            Instant.parse("2026-11-25T10:00:00Z"), "manager",
            status, "Dock 2", List.of()
        );
    }
}