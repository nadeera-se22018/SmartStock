package com.smartstock.backend.product.service;

import com.smartstock.backend.category.entity.Category;
import com.smartstock.backend.category.repository.CategoryRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.product.dto.ProductCreateRequest;
import com.smartstock.backend.product.dto.ProductResponse;
import com.smartstock.backend.product.dto.ProductSearchCriteria;
import com.smartstock.backend.product.dto.ProductUpdateRequest;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.mapper.ProductMapper;
import com.smartstock.backend.product.port.ProductStockReader;
import com.smartstock.backend.product.repository.ProductRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductStockReader stockReader;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductResponse response;

    @BeforeEach
    void setUp() {
        category = new Category("Electronics", null, null);
        ReflectionTestUtils.setField(category, "id", 10L);

        product = new Product(
            "SKU-001",
            "9551234567890",
            "Laptop",
            "Business laptop",
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            new BigDecimal("5.00"),
            category
        );
        ReflectionTestUtils.setField(product, "id", 1L);

        response = new ProductResponse(
            1L,
            "SKU-001",
            "9551234567890",
            "Laptop",
            "Business laptop",
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            new BigDecimal("5.00"),
            true,
            0L,
            null,
            new BigDecimal("12.00"),
            null,
            null
        );
    }

    @Test
    void createSuccessfullyNormalizesSkuBarcodeAndName() {
        ProductCreateRequest request = new ProductCreateRequest(
            " sku-001 ",
            " 9551234567890 ",
            "  Laptop ",
            "Business laptop",
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            new BigDecimal("5.00"),
            10L
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("9551234567890")).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(any(ProductCreateRequest.class), eq(category)))
            .thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(stockReader.getAvailableQuantity(1L))
            .thenReturn(new BigDecimal("12.00"));
        when(productMapper.toResponse(product, new BigDecimal("12.00")))
            .thenReturn(response);

        ProductResponse result = productService.create(request);

        assertSame(response, result);

        ArgumentCaptor<ProductCreateRequest> requestCaptor =
            ArgumentCaptor.forClass(ProductCreateRequest.class);

        verify(productMapper).toEntity(requestCaptor.capture(), eq(category));
        ProductCreateRequest normalized = requestCaptor.getValue();

        assertEquals("SKU-001", normalized.sku());
        assertEquals("9551234567890", normalized.barcode());
        assertEquals("Laptop", normalized.name());
        verify(productRepository).save(product);
    }

    @Test
    void createAllowsNullBarcode() {
        ProductCreateRequest request = new ProductCreateRequest(
            "SKU-001",
            null,
            "Laptop",
            null,
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            BigDecimal.ZERO,
            null
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productMapper.toEntity(any(ProductCreateRequest.class), isNull()))
            .thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(stockReader.getAvailableQuantity(1L)).thenReturn(BigDecimal.ZERO);
        when(productMapper.toResponse(product, BigDecimal.ZERO)).thenReturn(response);

        productService.create(request);

        verify(productRepository, never()).existsByBarcode(anyString());
        verify(categoryRepository, never()).findById(anyLong());
    }

    @Test
    void createRejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> productService.create(validCreateRequest())
        );

        assertEquals("PRODUCT_SKU_ALREADY_EXISTS", exception.getCode());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(categoryRepository, productMapper, stockReader);
    }

    @Test
    void createRejectsDuplicateBarcode() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("9551234567890")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> productService.create(validCreateRequest())
        );

        assertEquals("PRODUCT_BARCODE_ALREADY_EXISTS", exception.getCode());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(categoryRepository, productMapper, stockReader);
    }

    @Test
    void createRejectsMissingCategory() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("9551234567890")).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.create(validCreateRequest())
        );

        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveCategory() {
        Category inactiveCategory = new Category("Electronics", null, null);
        ReflectionTestUtils.setField(inactiveCategory, "id", 10L);
        inactiveCategory.deactivate();

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("9551234567890")).thenReturn(false);
        when(categoryRepository.findById(10L))
            .thenReturn(Optional.of(inactiveCategory));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.create(validCreateRequest())
        );

        assertEquals("CATEGORY_INACTIVE", exception.getCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsProductResponseWithAvailableQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(stockReader.getAvailableQuantity(1L))
            .thenReturn(new BigDecimal("12.00"));
        when(productMapper.toResponse(product, new BigDecimal("12.00")))
            .thenReturn(response);

        ProductResponse result = productService.getById(1L);

        assertSame(response, result);
        verify(stockReader).getAvailableQuantity(1L);
    }

    @Test
    void getByIdThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.getById(404L)
        );

        assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
        verifyNoInteractions(stockReader, productMapper);
    }

    @Test
    void getByBarcodeNormalizesBarcodeAndReturnsProduct() {
        when(productRepository.findByBarcode("9551234567890"))
            .thenReturn(Optional.of(product));
        when(stockReader.getAvailableQuantity(1L)).thenReturn(BigDecimal.TEN);
        when(productMapper.toResponse(product, BigDecimal.TEN)).thenReturn(response);

        ProductResponse result =
            productService.getByBarcode(" 9551234567890 ");

        assertSame(response, result);
        verify(productRepository).findByBarcode("9551234567890");
    }

    @Test
    void getByBarcodeRejectsBlankBarcode() {
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.getByBarcode("   ")
        );

        assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
        verify(productRepository, never()).findByBarcode(anyString());
    }

    @Test
    void getByBarcodeThrowsWhenBarcodeDoesNotExist() {
        when(productRepository.findByBarcode("9551234567890"))
            .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.getByBarcode("9551234567890")
        );

        assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
    }

    @Test
    void searchBuildsPageRequestAndMapsResults() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
            " laptop ", 10L, true, 0, 20, "sellingPrice"
        );

        Pageable expectedPageable = org.springframework.data.domain.PageRequest.of(
            0, 20, Sort.by("sellingPrice")
        );

        when(productRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(
            List.of(product),
            expectedPageable,
            1
        ));

        when(stockReader.getAvailableQuantity(1L)).thenReturn(BigDecimal.TEN);
        when(productMapper.toResponse(product, BigDecimal.TEN)).thenReturn(response);

        var result = productService.search(criteria);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals("sellingPrice", pageable.getSort().getOrderFor("sellingPrice").getProperty());
    }
    
    @Test
    void searchFallsBackToNameForUnknownSort() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
            null,
            null,
            true,
            0,
            20,
            "invalid"
        );

        when(productRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var result = productService.search(criteria);

        assertTrue(result.content().isEmpty());

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            pageableCaptor.capture()
        );

        assertNotNull(pageableCaptor.getValue().getSort().getOrderFor("name"));
    }

    @Test
    void updateSuccessfullyNormalizesValues() {
        ProductUpdateRequest request = new ProductUpdateRequest(
            " new-sku ",
            " 123456 ",
            " Updated laptop ",
            "Updated description",
            new BigDecimal("600.00"),
            new BigDecimal("900.00"),
            new BigDecimal("8.00"),
            10L
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("NEW-SKU", 1L))
            .thenReturn(false);
        when(productRepository.existsByBarcodeAndIdNot("123456", 1L))
            .thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(productMapper.toResponse(eq(product), any())).thenReturn(response);
        when(stockReader.getAvailableQuantity(1L)).thenReturn(BigDecimal.TEN);

        ProductResponse result = productService.update(1L, request);

        assertSame(response, result);
        verify(productMapper).updateEntity(
            eq(product),
            argThat(updated ->
                updated.sku().equals("NEW-SKU")
                    && updated.barcode().equals("123456")
                    && updated.name().equals("Updated laptop")
            ),
            eq(category)
        );
    }

    @Test
    void updateRejectsDuplicateSku() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-001", 1L))
            .thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> productService.update(
                1L,
                validUpdateRequest()
            )
        );

        assertEquals("PRODUCT_SKU_ALREADY_EXISTS", exception.getCode());
        verify(productMapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void updateRejectsDuplicateBarcode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-001", 1L))
            .thenReturn(false);
        when(productRepository.existsByBarcodeAndIdNot("9551234567890", 1L))
            .thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> productService.update(
                1L,
                validUpdateRequest()
            )
        );

        assertEquals("PRODUCT_BARCODE_ALREADY_EXISTS", exception.getCode());
        verify(productMapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void deactivateSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deactivate(1L);

        assertFalse(product.isActive());
    }

    @Test
    void deactivateThrowsWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.deactivate(404L)
        );

        assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
    }

    @Test
    void negativePricesAreRejectedByRequestValidation() {
        Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

        ProductCreateRequest request = new ProductCreateRequest(
            "SKU-001",
            null,
            "Laptop",
            null,
            new BigDecimal("-1.00"),
            new BigDecimal("-1.00"),
            new BigDecimal("-1.00"),
            null
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    private ProductCreateRequest validCreateRequest() {
        return new ProductCreateRequest(
            "SKU-001",
            "9551234567890",
            "Laptop",
            "Business laptop",
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            new BigDecimal("5.00"),
            10L
        );
    }

    private ProductUpdateRequest validUpdateRequest() {
        return new ProductUpdateRequest(
            "SKU-001",
            "9551234567890",
            "Laptop",
            "Business laptop",
            new BigDecimal("500.00"),
            new BigDecimal("750.00"),
            new BigDecimal("5.00"),
            10L
        );
    }
}