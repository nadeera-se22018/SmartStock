package com.smartstock.backend.purchasing.entity;

import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {
    private static final int DECIMAL_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQuantity = BigDecimal.ZERO.setScale(DECIMAL_SCALE);

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected PurchaseOrderItem() {
    }

    public PurchaseOrderItem(PurchaseOrder purchaseOrder, Product product, BigDecimal quantity, BigDecimal unitCost) {
        this.purchaseOrder = purchaseOrder;
        this.product = product;
        this.quantity = quantity.setScale(DECIMAL_SCALE, RoundingMode.UNNECESSARY);
        this.unitCost = unitCost.setScale(DECIMAL_SCALE, RoundingMode.UNNECESSARY);
        this.lineTotal = this.quantity.multiply(this.unitCost).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public void receive(BigDecimal quantity) {
        BigDecimal updatedQuantity = receivedQuantity.add(quantity);
        if (quantity.signum() <= 0 || updatedQuantity.compareTo(this.quantity) > 0) {
            throw new BusinessRuleException(
                "PURCHASE_RECEIPT_QUANTITY_EXCEEDED",
                "Received quantity must be positive and no greater than the remaining ordered quantity."
            );
        }
        receivedQuantity = updatedQuantity.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public Product getProduct() { return product; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
