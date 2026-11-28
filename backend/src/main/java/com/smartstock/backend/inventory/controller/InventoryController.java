package com.smartstock.backend.inventory.controller;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.InventorySearchCriteria;
import com.smartstock.backend.inventory.dto.InventoryStockStatus;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.dto.StockMovementSearchCriteria;
import com.smartstock.backend.inventory.entity.StockMovementType;
import com.smartstock.backend.inventory.service.InventoryAdjustmentResult;
import com.smartstock.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@Validated
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public PageResponse<InventoryBalanceResponse> searchBalances(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "true") Boolean active,
        @RequestParam(required = false) InventoryStockStatus stockStatus,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "sku,asc") String sort
    ) {
        return inventoryService.searchBalances(
            new InventorySearchCriteria(search, categoryId, active, stockStatus, page, size, sort)
        );
    }

    @GetMapping("/balances/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public InventoryBalanceResponse getBalance(@PathVariable Long productId) {
        return inventoryService.getBalance(productId);
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_STAFF')")
    public ResponseEntity<StockMovementResponse> adjust(
        @Valid @RequestBody StockAdjustmentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        InventoryAdjustmentResult result = inventoryService.adjust(request, idempotencyKey);
        StockMovementResponse response = result.movement();
        if (result.replayed()) {
            return ResponseEntity.ok(response);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/inventory/movements/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public PageResponse<StockMovementResponse> searchMovements(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) StockMovementType movementType,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) Long referenceId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return inventoryService.searchMovements(new StockMovementSearchCriteria(
            productId, movementType, referenceType, referenceId, from, to, page, size, sort
        ));
    }

    @GetMapping("/movements/{movementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public StockMovementResponse getMovement(@PathVariable Long movementId) {
        return inventoryService.getMovement(movementId);
    }
}
