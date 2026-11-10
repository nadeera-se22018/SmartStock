package com.smartstock.backend.product.service;

import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.product.dto.ProductCreateRequest;
import com.smartstock.backend.product.dto.ProductResponse;
import com.smartstock.backend.product.dto.ProductSearchCriteria;
import com.smartstock.backend.product.dto.ProductUpdateRequest;

public interface ProductService {
    ProductResponse create(ProductCreateRequest request);
    ProductResponse getById(Long id);
    PageResponse<ProductResponse> search(ProductSearchCriteria criteria);
    ProductResponse getByBarcode(String barcode);
    ProductResponse update(Long id, ProductUpdateRequest request);
    void deactivate(Long id);
}
