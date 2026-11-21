package com.smartstock.backend.supplier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "suppliers", indexes = {
    @Index(name = "idx_suppliers_name", columnList = "name"),
    @Index(name = "idx_suppliers_normalized_name", columnList = "normalized_name"),
    @Index(name = "idx_suppliers_email", columnList = "email"),
    @Index(name = "idx_suppliers_phone", columnList = "phone"),
    @Index(name = "idx_suppliers_active", columnList = "is_active")
})
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 180) private String name;
    @Column(name = "normalized_name", nullable = false, length = 180, unique = true) private String normalizedName;
    @Column(name = "contact_person", length = 180) private String contactPerson;
    @Column(length = 255) private String email;
    @Column(length = 30) private String phone;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Version @Column(nullable = false) private Long version;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private Instant updatedAt;

    protected Supplier() {}
    public Supplier(String name, String normalizedName, String contactPerson, String email, String phone, String address) {
        this.name = name; this.normalizedName = normalizedName; this.contactPerson = contactPerson;
        this.email = email; this.phone = phone; this.address = address;
    }
    public void updateDetails(String name, String normalizedName, String contactPerson, String email, String phone, String address) {
        this.name = name; this.normalizedName = normalizedName; this.contactPerson = contactPerson;
        this.email = email; this.phone = phone; this.address = address;
    }
    public void deactivate() { active = false; }
    public void activate() { active = true; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
