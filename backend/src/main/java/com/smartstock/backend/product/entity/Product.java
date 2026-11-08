package com.smartstock.backend.product.entity;

import com.smartstock.backend.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_active", columnList = "is_active"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_barcode", columnList = "barcode")
    }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(unique = true, length = 100)
    private String barcode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal costPrice;

    @Column(name = "selling_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal sellingPrice;

    @Column(name = "reorder_level", nullable = false, precision = 19, scale = 4)
    private BigDecimal reorderLevel;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    protected Product() {
    }

    public Product(
        String sku,
        String barcode,
        String name,
        String description,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        BigDecimal reorderLevel,
        Category category
    ) {
        this.sku = sku;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.reorderLevel = reorderLevel;
        this.category = category;
    }

    public void updateDetails(
        String sku,
        String barcode,
        String name,
        String description,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        BigDecimal reorderLevel,
        Category category
    ) {
        this.sku = sku;
        this.barcode = barcode;
        this.name = name;
        this.description = description;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.reorderLevel = reorderLevel;
        this.category = category;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Category getCategory() { return category; }
}
