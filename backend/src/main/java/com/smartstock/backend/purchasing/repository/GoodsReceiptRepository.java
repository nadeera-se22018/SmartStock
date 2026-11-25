package com.smartstock.backend.purchasing.repository;

import com.smartstock.backend.purchasing.entity.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    Optional<GoodsReceipt> findByReceiptNumberIgnoreCase(String receiptNumber);

    boolean existsByReceiptNumberIgnoreCase(String receiptNumber);

    @EntityGraph(attributePaths = {"purchaseOrder", "items", "items.purchaseOrderItem", "items.product"})
    Optional<GoodsReceipt> findById(Long id);

    @EntityGraph(attributePaths = {"purchaseOrder", "items", "items.purchaseOrderItem", "items.product"})
    Optional<GoodsReceipt> findByIdAndPurchaseOrderId(Long id, Long purchaseOrderId);

    @EntityGraph(attributePaths = {"purchaseOrder", "items", "items.purchaseOrderItem", "items.product"})
    Optional<GoodsReceipt> findByIdempotencyKey(String idempotencyKey);

    Page<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId, Pageable pageable);
}
