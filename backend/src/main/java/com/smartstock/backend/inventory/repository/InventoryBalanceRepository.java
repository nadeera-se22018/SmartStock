package com.smartstock.backend.inventory.repository;

import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.dto.InventoryStockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    Optional<InventoryBalance> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from InventoryBalance b where b.product.id = :productId")
    Optional<InventoryBalance> findByProductIdForUpdate(@Param("productId") Long productId);

    @Query(
        value = """
            select p.id as productId, p.sku as sku, p.barcode as barcode, p.name as productName,
                coalesce(b.quantityOnHand, 0.0000) as quantityOnHand,
                coalesce(b.quantityReserved, 0.0000) as quantityReserved,
                coalesce(b.quantityOnHand, 0.0000) - coalesce(b.quantityReserved, 0.0000) as availableQuantity,
                p.reorderLevel as reorderLevel, b.updatedAt as updatedAt
            from Product p left join InventoryBalance b on b.product.id = p.id
            where (:search is null or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.name) like lower(concat('%', :search, '%'))
                or lower(p.barcode) like lower(concat('%', :search, '%')))
                and (:categoryId is null or p.category.id = :categoryId)
                and (:active is null or p.active = :active)
                and (:stockStatus is null
                    or (:stockStatus = com.smartstock.backend.inventory.dto.InventoryStockStatus.LOW_STOCK
                        and coalesce(b.quantityOnHand, 0.0000) <= p.reorderLevel)
                    or (:stockStatus = com.smartstock.backend.inventory.dto.InventoryStockStatus.OUT_OF_STOCK
                        and coalesce(b.quantityOnHand, 0.0000) = 0.0000))
            """,
        countQuery = """
            select count(p.id)
            from Product p left join InventoryBalance b on b.product.id = p.id
            where (:search is null or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.name) like lower(concat('%', :search, '%'))
                or lower(p.barcode) like lower(concat('%', :search, '%')))
                and (:categoryId is null or p.category.id = :categoryId)
                and (:active is null or p.active = :active)
                and (:stockStatus is null
                    or (:stockStatus = com.smartstock.backend.inventory.dto.InventoryStockStatus.LOW_STOCK
                        and coalesce(b.quantityOnHand, 0.0000) <= p.reorderLevel)
                    or (:stockStatus = com.smartstock.backend.inventory.dto.InventoryStockStatus.OUT_OF_STOCK
                        and coalesce(b.quantityOnHand, 0.0000) = 0.0000))
            """
    )
    Page<InventoryBalanceProjection> search(
        @Param("search") String search,
        @Param("categoryId") Long categoryId,
        @Param("active") Boolean active,
        @Param("stockStatus") InventoryStockStatus stockStatus,
        Pageable pageable
    );
}
