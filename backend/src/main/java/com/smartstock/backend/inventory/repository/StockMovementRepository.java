package com.smartstock.backend.inventory.repository;

import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.entity.StockMovementType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface StockMovementRepository extends Repository<StockMovement, Long> {
    <S extends StockMovement> S save(S movement);

    Optional<StockMovement> findById(Long id);

    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from StockMovement m where m.idempotencyKey = :idempotencyKey")
    Optional<StockMovement> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Query("""
        select m.id as id, p.id as productId, p.sku as sku, p.name as productName,
            m.movementType as movementType, m.quantity as quantity,
            m.quantityBefore as quantityBefore, m.quantityAfter as quantityAfter,
            m.referenceType as referenceType, m.referenceId as referenceId, m.reason as reason,
            u.id as performedByUserId, u.username as performedByUsername, m.createdAt as createdAt
        from StockMovement m join m.product p join com.smartstock.backend.auth.entity.User u
            on u.id = m.performedBy
        where (:productId is null or p.id = :productId)
            and (:movementType is null or m.movementType = :movementType)
            and (:referenceType is null or m.referenceType = :referenceType)
            and (:referenceId is null or m.referenceId = :referenceId)
            and (:fromTime is null or m.createdAt >= :fromTime)
            and (:toTime is null or m.createdAt <= :toTime)
        """)
    Page<StockMovementProjection> search(
        @Param("productId") Long productId,
        @Param("movementType") StockMovementType movementType,
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId,
        @Param("fromTime") Instant from,
        @Param("toTime") Instant to,
        Pageable pageable
    );

    @Query("""
        select m.id as id, p.id as productId, p.sku as sku, p.name as productName,
            m.movementType as movementType, m.quantity as quantity,
            m.quantityBefore as quantityBefore, m.quantityAfter as quantityAfter,
            m.referenceType as referenceType, m.referenceId as referenceId, m.reason as reason,
            u.id as performedByUserId, u.username as performedByUsername, m.createdAt as createdAt
        from StockMovement m join m.product p join com.smartstock.backend.auth.entity.User u
            on u.id = m.performedBy
        where m.id = :movementId
        """)
    Optional<StockMovementProjection> findProjectionById(@Param("movementId") Long movementId);

    @Query("""
        select m.id as id, p.id as productId, p.sku as sku, p.name as productName,
            m.movementType as movementType, m.quantity as quantity,
            m.quantityBefore as quantityBefore, m.quantityAfter as quantityAfter,
            m.referenceType as referenceType, m.referenceId as referenceId, m.reason as reason,
            u.id as performedByUserId, u.username as performedByUsername, m.createdAt as createdAt
        from StockMovement m join m.product p join com.smartstock.backend.auth.entity.User u
            on u.id = m.performedBy
        where m.idempotencyKey = :idempotencyKey
        """)
    Optional<StockMovementProjection> findProjectionByIdempotencyKey(
        @Param("idempotencyKey") String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        select m.id as id, p.id as productId, p.sku as sku, p.name as productName,
            m.movementType as movementType, m.quantity as quantity,
            m.quantityBefore as quantityBefore, m.quantityAfter as quantityAfter,
            m.referenceType as referenceType, m.referenceId as referenceId, m.reason as reason,
            u.id as performedByUserId, u.username as performedByUsername, m.createdAt as createdAt
        from StockMovement m join m.product p join com.smartstock.backend.auth.entity.User u
            on u.id = m.performedBy
        where m.idempotencyKey = :idempotencyKey
        """)
    Optional<StockMovementProjection> findProjectionByIdempotencyKeyForUpdate(
        @Param("idempotencyKey") String idempotencyKey
    );
}
