package com.smartstock.backend.product.mapper;

import com.smartstock.backend.category.dto.CategorySummary;
import com.smartstock.backend.category.entity.Category;
import com.smartstock.backend.product.dto.ProductCreateRequest;
import com.smartstock.backend.product.dto.ProductResponse;
import com.smartstock.backend.product.dto.ProductUpdateRequest;
import com.smartstock.backend.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return toResponse(product, null);
    }

    public ProductResponse toResponse(Product product, BigDecimal availableQuantity) {
        Category category = product.getCategory();
        return new ProductResponse(
            product.getId(),
            product.getSku(),
            product.getBarcode(),
            product.getName(),
            product.getDescription(),
            product.getCostPrice(),
            product.getSellingPrice(),
            product.getReorderLevel(),
            product.isActive(),
            product.getVersion(),
            category == null ? null : new CategorySummary(category.getId(), category.getName()),
            availableQuantity,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public Product toEntity(ProductCreateRequest request, Category category) {
        return new Product(
            request.sku(),
            request.barcode(),
            request.name(),
            request.description(),
            request.costPrice(),
            request.sellingPrice(),
            request.reorderLevel(),
            category
        );
    }

    public void updateEntity(Product product, ProductUpdateRequest request, Category category) {
        product.updateDetails(
            request.sku(),
            request.barcode(),
            request.name(),
            request.description(),
            request.costPrice(),
            request.sellingPrice(),
            request.reorderLevel(),
            category
        );
    }
}
