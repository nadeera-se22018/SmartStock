package com.smartstock.backend.purchasing.controller;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.purchasing.dto.GoodsReceiptCreateRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseCreateRequest;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseSearchCriteria;
import com.smartstock.backend.purchasing.dto.PurchaseUpdateRequest;
import com.smartstock.backend.purchasing.service.PurchaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Validated
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseCreateRequest request) {
        PurchaseResponse response = purchaseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public PageResponse<PurchaseResponse> search(@Valid @ModelAttribute PurchaseSearchCriteria criteria) {
        return purchaseService.search(criteria);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public PurchaseResponse getById(@PathVariable Long id) {
        return purchaseService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PurchaseResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseUpdateRequest request) {
        return purchaseService.update(id, request);
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PurchaseResponse submit(@PathVariable Long id) {
        return purchaseService.submit(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PurchaseResponse cancel(@PathVariable Long id) {
        return purchaseService.cancel(id);
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_STAFF')")
    public ResponseEntity<GoodsReceiptResponse> receiveGoods(
        @PathVariable Long id,
        @Valid @RequestBody GoodsReceiptCreateRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false)
        @Size(min = 36, max = 128) String idempotencyKey
    ) {
        GoodsReceiptResponse response = purchaseService.receiveGoods(id, request, idempotencyKey);
        if (response.replayed()) {
            return ResponseEntity.ok(response);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{receiptId}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public PageResponse<GoodsReceiptResponse> getReceipts(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return purchaseService.getReceipts(id, page, size);
    }

    @GetMapping("/{id}/receipts/{receiptId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')")
    public GoodsReceiptResponse getReceiptById(@PathVariable Long id, @PathVariable Long receiptId) {
        return purchaseService.getReceiptById(id, receiptId);
    }
}
