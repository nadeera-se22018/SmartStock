package com.smartstock.backend.inventory.entity;

import com.smartstock.backend.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_balances")
public class InventoryBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO.setScale(4);

    @Column(name = "quantity_reserved", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityReserved = BigDecimal.ZERO.setScale(4);

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected InventoryBalance() {
    }

    public InventoryBalance(Product product) {
        this.product = product;
    }

    public BigDecimal increaseOnHand(BigDecimal quantity) {
        quantityOnHand = quantityOnHand.add(quantity);
        return quantityOnHand;
    }

    public BigDecimal decreaseOnHand(BigDecimal quantity) {
        quantityOnHand = quantityOnHand.subtract(quantity);
        return quantityOnHand;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public BigDecimal getQuantityReserved() { return quantityReserved; }
    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
}
