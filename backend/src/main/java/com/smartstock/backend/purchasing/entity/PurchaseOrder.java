package com.smartstock.backend.purchasing.entity;

import com.smartstock.backend.purchasing.dto.PurchaseStatus;
import com.smartstock.backend.supplier.entity.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.smartstock.backend.common.exception.BusinessRuleException;

@Entity
@Table(
    name = "purchase_orders",
    indexes = {
        @Index(name = "idx_purchase_orders_supplier_date", columnList = "supplier_id, order_date"),
        @Index(name = "idx_purchase_orders_status_date", columnList = "status, order_date"),
        @Index(name = "idx_purchase_orders_order_date", columnList = "order_date")
    }
)
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String purchaseNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PurchaseStatus status = PurchaseStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(4);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(4);

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder")
    @OrderBy("receivedAt DESC")
    private List<GoodsReceipt> receipts = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(
        String purchaseNumber,
        Supplier supplier,
        Long createdBy,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        String notes
    ) {
        this.purchaseNumber = purchaseNumber;
        this.supplier = supplier;
        this.createdBy = createdBy;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.notes = notes;
    }

    public void addItem(PurchaseOrderItem item) {
        items.add(item);
    }

    public void updateDraft(
        Supplier supplier,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        String notes,
        List<PurchaseOrderItem> replacementItems,
        BigDecimal subtotal
    ) {
        requireStatus(PurchaseStatus.DRAFT);
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.notes = notes;
        items.clear();
        replacementItems.forEach(this::addItem);
        setSubtotal(subtotal);
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal.setScale(4);
        this.totalAmount = this.subtotal;
    }

    public void submit() {
        requireStatus(PurchaseStatus.DRAFT);
        if (items.isEmpty()) {
            throw new BusinessRuleException(
                "PURCHASE_SUBMIT_REQUIRES_ITEMS", "A purchase order must contain at least one item before submission."
            );
        }
        status = PurchaseStatus.SUBMITTED;
    }

    public void cancel() {
        if (status != PurchaseStatus.DRAFT && status != PurchaseStatus.SUBMITTED) {
            throw transitionNotAllowed();
        }
        status = PurchaseStatus.CANCELLED;
    }

    public PurchaseStatus statusAfterReceipt(Map<Long, BigDecimal> receivedByItemId) {
        if (status != PurchaseStatus.SUBMITTED && status != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw transitionNotAllowed();
        }
        boolean fullyReceived = true;
        for (PurchaseOrderItem item : items) {
            BigDecimal newReceived = item.getReceivedQuantity().add(
                receivedByItemId.getOrDefault(item.getId(), BigDecimal.ZERO)
            );
            if (newReceived.compareTo(item.getQuantity()) < 0) {
                fullyReceived = false;
            }
        }
        return fullyReceived ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED;
    }

    public void recordReceipt(Map<Long, BigDecimal> receivedByItemId) {
        statusAfterReceipt(receivedByItemId);
        items.stream()
            .filter(item -> receivedByItemId.containsKey(item.getId()))
            .forEach(item -> item.receive(receivedByItemId.get(item.getId())));
        status = items.stream().allMatch(item ->
            item.getReceivedQuantity().compareTo(item.getQuantity()) == 0
        ) ? PurchaseStatus.RECEIVED : PurchaseStatus.PARTIALLY_RECEIVED;
    }

    private void requireStatus(PurchaseStatus expected) {
        if (status != expected) {
            throw transitionNotAllowed();
        }
    }

    private BusinessRuleException transitionNotAllowed() {
        return new BusinessRuleException(
            "PURCHASE_STATUS_TRANSITION_NOT_ALLOWED",
            "The purchase order cannot transition from " + status + " for this operation."
        );
    }

    public Long getId() { return id; }
    public String getPurchaseNumber() { return purchaseNumber; }
    public Supplier getSupplier() { return supplier; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDate getOrderDate() { return orderDate; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public PurchaseStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public BigDecimal getSubTotal() { return subtotal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PurchaseOrderItem> getItems() { return Collections.unmodifiableList(items); }
    public List<GoodsReceipt> getReceipts() { return Collections.unmodifiableList(receipts); }
}
