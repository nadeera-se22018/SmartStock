package com.smartstock.backend.product.service;

import com.smartstock.backend.category.entity.Category;
import com.smartstock.backend.category.repository.CategoryRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.product.dto.ProductCreateRequest;
import com.smartstock.backend.product.dto.ProductResponse;
import com.smartstock.backend.product.dto.ProductSearchCriteria;
import com.smartstock.backend.product.dto.ProductUpdateRequest;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.mapper.ProductMapper;
import com.smartstock.backend.product.port.ProductStockReader;
import com.smartstock.backend.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductStockReader stockReader;

    public ProductServiceImpl(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        ProductMapper productMapper,
        ProductStockReader stockReader
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
        this.stockReader = stockReader;
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        String sku = normalizeSku(request.sku());
        String barcode = normalizeBarcode(request.barcode());
        ensureUnique(sku, barcode, null);
        Category category = resolveCategory(request.categoryId());
        Product product = productRepository.save(productMapper.toEntity(
            new ProductCreateRequest(sku, barcode, request.name().trim(), request.description(),
                request.costPrice(), request.sellingPrice(), request.reorderLevel(), request.categoryId()),
            category
        ));
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchCriteria criteria) {
        Sort sort = Sort.by(resolveSort(criteria.sort()));
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);
        Specification<Product> specification = (root, query, cb) -> cb.conjunction();
        if (criteria.search() != null && !criteria.search().isBlank()) {
            String search = "%" + criteria.search().trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("sku")), search),
                cb.like(cb.lower(root.get("name")), search),
                cb.like(cb.lower(root.get("description")), search)
            ));
        }
        if (criteria.categoryId() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("category").get("id"), criteria.categoryId()));
        }
        if (criteria.active() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("active"), criteria.active()));
        }
        Page<ProductResponse> page = productRepository.findAll(specification, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product barcode was not supplied.");
        }
        return toResponse(productRepository.findByBarcode(normalized).orElseThrow(
            () -> new ResourceNotFoundException(
                "PRODUCT_NOT_FOUND", "No product with barcode '" + normalized + "' was found."
            )
        ));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findProduct(id);
        String sku = normalizeSku(request.sku());
        String barcode = normalizeBarcode(request.barcode());
        ensureUnique(sku, barcode, id);
        Category category = resolveCategory(request.categoryId());
        productMapper.updateEntity(product, new ProductUpdateRequest(
            sku, barcode, request.name().trim(), request.description(), request.costPrice(),
            request.sellingPrice(), request.reorderLevel(), request.categoryId()
        ), category);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Product product = findProduct(id);
        product.deactivate();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "PRODUCT_NOT_FOUND", "Product with id '" + id + "' was not found."
            ));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "CATEGORY_NOT_FOUND", "Category with id '" + categoryId + "' was not found."
            ));
        if (!category.isActive()) {
            throw new BusinessRuleException(
                "CATEGORY_INACTIVE", "An inactive category cannot be assigned to a product."
            );
        }
        return category;
    }

    private void ensureUnique(String sku, String barcode, Long id) {
        boolean skuExists = id == null
            ? productRepository.existsBySkuIgnoreCase(sku)
            : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id);
        if (skuExists) {
            throw new DuplicateResourceException(
                "PRODUCT_SKU_ALREADY_EXISTS", "A product with SKU '" + sku + "' already exists."
            );
        }
        boolean barcodeExists = barcode != null && (id == null
            ? productRepository.existsByBarcode(barcode)
            : productRepository.existsByBarcodeAndIdNot(barcode, id));
        if (barcodeExists) {
            throw new DuplicateResourceException(
                "PRODUCT_BARCODE_ALREADY_EXISTS", "A product with barcode '" + barcode + "' already exists."
            );
        }
    }

    private ProductResponse toResponse(Product product) {
        BigDecimal quantity = stockReader.getAvailableQuantity(product.getId());
        return productMapper.toResponse(product, quantity);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeBarcode(String barcode) {
        return barcode == null || barcode.isBlank() ? null : barcode.trim();
    }

    private String resolveSort(String sort) {
        return switch (sort == null ? "name" : sort) {
            case "sku", "name", "createdAt", "updatedAt", "sellingPrice" -> sort;
            default -> "name";
        };
    }
}
