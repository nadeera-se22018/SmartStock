package com.smartstock.backend.purchasing.repository;

import com.smartstock.backend.purchasing.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
    List<GoodsReceiptItem> findByPurchaseOrderItemId(Long purchaseOrderItemId);
}
