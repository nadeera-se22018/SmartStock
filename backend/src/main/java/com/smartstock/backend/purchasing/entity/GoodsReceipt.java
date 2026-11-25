package com.smartstock.backend.purchasing.entity;

import com.smartstock.backend.purchasing.dto.PurchaseStatus;
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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
    name = "goods_receipts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_goods_receipts_idempotency_key", columnNames = "idempotency_key")
    },
    indexes = {
        @Index(name = "idx_goods_receipts_purchase_received", columnList = "purchase_order_id, received_at")
    }
)
public class GoodsReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "received_by", nullable = false, updatable = false)
    private Long receivedById;

    @Column(name = "received_by_username", nullable = false, updatable = false, length = 80)
    private String receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after_receipt", nullable = false, length = 24)
    private PurchaseStatus statusAfterReceipt;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<GoodsReceiptItem> items = new ArrayList<>();

    protected GoodsReceipt() {
    }

    public GoodsReceipt(
        String receiptNumber,
        PurchaseOrder purchaseOrder,
        Long receivedById,
        String receivedBy,
        Instant receivedAt,
        String notes,
        PurchaseStatus statusAfterReceipt,
        String idempotencyKey,
        String requestHash
    ) {
        this.receiptNumber = receiptNumber;
        this.purchaseOrder = purchaseOrder;
        this.receivedById = receivedById;
        this.receivedBy = receivedBy;
        this.receivedAt = receivedAt;
        this.notes = notes;
        this.statusAfterReceipt = statusAfterReceipt;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public void addItem(GoodsReceiptItem item) {
        items.add(item);
    }

    public Long getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Long getReceivedById() { return receivedById; }
    public String getReceivedBy() { return receivedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getNotes() { return notes; }
    public PurchaseStatus getStatusAfterReceipt() { return statusAfterReceipt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<GoodsReceiptItem> getItems() { return Collections.unmodifiableList(items); }
}
