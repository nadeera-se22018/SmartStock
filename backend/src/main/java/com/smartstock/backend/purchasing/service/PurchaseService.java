package com.smartstock.backend.purchasing.service;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.purchasing.dto.GoodsReceiptCreateRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseCreateRequest;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseSearchCriteria;
import com.smartstock.backend.purchasing.dto.PurchaseUpdateRequest;

public interface PurchaseService {
    PurchaseResponse create(PurchaseCreateRequest request);
    PurchaseResponse getById(Long id);
    PageResponse<PurchaseResponse> search(PurchaseSearchCriteria criteria);
    PurchaseResponse update(Long id, PurchaseUpdateRequest request);
    PurchaseResponse submit(Long id);
    PurchaseResponse cancel(Long id);
    GoodsReceiptResponse receiveGoods(Long purchaseOrderId, GoodsReceiptCreateRequest request, String idempotencyKey);
    PageResponse<GoodsReceiptResponse> getReceipts(Long purchaseOrderId, int page, int size);
    GoodsReceiptResponse getReceiptById(Long purchaseOrderId, Long receiptId);
}
