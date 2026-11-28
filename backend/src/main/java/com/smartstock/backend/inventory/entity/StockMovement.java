package com.smartstock.backend.inventory.entity;

import com.smartstock.backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 32)
    private StockMovementType movementType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "quantity_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String reason;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", columnDefinition = "CHAR(64)", length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(
        Product product,
        Long performedBy,
        StockMovementType movementType,
        BigDecimal quantity,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String referenceType,
        Long referenceId,
        String reason,
        String idempotencyKey,
        String requestHash
    ) {
        this.product = product;
        this.performedBy = performedBy;
        this.movementType = movementType;
        this.quantity = quantity;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Long getPerformedBy() { return performedBy; }
    public StockMovementType getMovementType() { return movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getQuantityBefore() { return quantityBefore; }
    public BigDecimal getQuantityAfter() { return quantityAfter; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
}
