package com.smartstock.backend.purchasing.repository;

import com.smartstock.backend.purchasing.entity.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository
    extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByPurchaseNumberIgnoreCase(String purchaseNumber);

    boolean existsByPurchaseNumberIgnoreCase(String purchaseNumber);

    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findLockedById(@Param("id") Long id);
}
