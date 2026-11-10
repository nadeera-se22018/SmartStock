package com.smartstock.backend.category.service;

import com.smartstock.backend.category.dto.CategoryCreateRequest;
import com.smartstock.backend.category.dto.CategoryResponse;
import com.smartstock.backend.category.dto.CategoryUpdateRequest;
import com.smartstock.backend.category.entity.Category;
import com.smartstock.backend.category.mapper.CategoryMapper;
import com.smartstock.backend.category.repository.CategoryRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        response = new CategoryResponse(
            1L,
            "Electronics",
            "Electronic products",
            null,
            true,
            0L,
            null,
            null
        );
    }

    @Test
    void createRootCategorySuccessfully() {
        Category category = category(1L, "Electronics", null);

        when(categoryRepository.existsByNameIgnoreCaseAndParentId(
            eq("Electronics"), isNull()
        )).thenReturn(false);
        when(categoryMapper.toEntity(any(CategoryCreateRequest.class), isNull()))
            .thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.create(
            new CategoryCreateRequest("  Electronics  ", "Electronic products", null)
        );

        assertSame(response, result);

        ArgumentCaptor<CategoryCreateRequest> requestCaptor =
            ArgumentCaptor.forClass(CategoryCreateRequest.class);

        verify(categoryMapper).toEntity(requestCaptor.capture(), isNull());
        assertEquals("Electronics", requestCaptor.getValue().name());
        verify(categoryRepository).save(category);
    }

    @Test
    void createChildCategorySuccessfully() {
        Category parent = category(10L, "Electronics", null);
        Category child = category(20L, "Laptops", parent);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsByNameIgnoreCaseAndParentId(
            "Laptops", 10L
        )).thenReturn(false);
        when(categoryMapper.toEntity(any(CategoryCreateRequest.class), eq(parent)))
            .thenReturn(child);
        when(categoryRepository.save(child)).thenReturn(child);
        when(categoryMapper.toResponse(child)).thenReturn(response);

        CategoryResponse result = categoryService.create(
            new CategoryCreateRequest("Laptops", "Laptop products", 10L)
        );

        assertSame(response, result);
        verify(categoryRepository).findById(10L);
        verify(categoryRepository).existsByNameIgnoreCaseAndParentId("Laptops", 10L);
        verify(categoryMapper).toEntity(any(CategoryCreateRequest.class), eq(parent));
        verify(categoryRepository).save(child);
    }

    @Test
    void createRejectsDuplicateSiblingName() {
        when(categoryRepository.existsByNameIgnoreCaseAndParentId(
            "Electronics", null
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> categoryService.create(
                new CategoryCreateRequest(" Electronics ", null, null)
            )
        );

        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", exception.getCode());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void createRejectsMissingParent() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> categoryService.create(
                new CategoryCreateRequest("Laptops", null, 99L)
            )
        );

        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
        verify(categoryRepository, never())
            .existsByNameIgnoreCaseAndParentId(anyString(), any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveParent() {
        Category parent = category(10L, "Electronics", null);
        parent.deactivate();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> categoryService.create(
                new CategoryCreateRequest("Laptops", null, 10L)
            )
        );

        assertEquals("CATEGORY_PARENT_INACTIVE", exception.getCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsMappedCategory() {
        Category category = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        assertSame(response, categoryService.getById(1L));

        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void getByIdThrowsWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> categoryService.getById(404L)
        );

        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
    }

    @Test
    void getAllReturnsMappedCategories() {
        Category first = category(1L, "Electronics", null);
        Category second = category(2L, "Furniture", null);

        CategoryResponse firstResponse = response;
        CategoryResponse secondResponse = new CategoryResponse(
            2L, "Furniture", null, null, true, 0L, null, null
        );

        when(categoryRepository.findAll()).thenReturn(List.of(first, second));
        when(categoryMapper.toResponse(first)).thenReturn(firstResponse);
        when(categoryMapper.toResponse(second)).thenReturn(secondResponse);

        List<CategoryResponse> result = categoryService.getAll();

        assertEquals(List.of(firstResponse, secondResponse), result);
        verify(categoryRepository).findAll();
    }

    @Test
    void updateSuccessfully() {
        Category category = category(1L, "Old Name", null);
        CategoryResponse updatedResponse = new CategoryResponse(
            1L, "New Name", "Updated", null, true, 0L, null, null
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndParentIdAndIdNot(
            "New Name", null, 1L
        )).thenReturn(false);
        when(categoryMapper.toResponse(category)).thenReturn(updatedResponse);

        CategoryResponse result = categoryService.update(
            1L,
            new CategoryUpdateRequest(" New Name ", "Updated", null)
        );

        assertSame(updatedResponse, result);
        verify(categoryMapper).updateEntity(
            eq(category),
            any(CategoryUpdateRequest.class),
            isNull()
        );
    }

    @Test
    void updateRejectsDuplicateSiblingName() {
        Category category = category(1L, "Old Name", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndParentIdAndIdNot(
            "Electronics", null, 1L
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> categoryService.update(
                1L,
                new CategoryUpdateRequest("Electronics", null, null)
            )
        );

        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", exception.getCode());
        verify(categoryMapper, never())
            .updateEntity(any(), any(), any());
    }

    @Test
    void updateRejectsSelfParenting() {
        Category category = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> categoryService.update(
                1L,
                new CategoryUpdateRequest("Electronics", null, 1L)
            )
        );

        assertEquals("CATEGORY_SELF_PARENT", exception.getCode());
        verify(categoryRepository, never())
            .existsByNameIgnoreCaseAndParentIdAndIdNot(anyString(), any(), anyLong());
    }

    @Test
    void updateRejectsCircularHierarchy() {
        Category root = category(1L, "Root", null);
        Category descendant = category(2L, "Child", root);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(descendant));

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> categoryService.update(
                1L,
                new CategoryUpdateRequest("Root", null, 2L)
            )
        );

        assertEquals("CATEGORY_CIRCULAR_HIERARCHY", exception.getCode());
        verify(categoryMapper, never())
            .updateEntity(any(), any(), any());
    }

    @Test
    void deactivateSuccessfullyWhenNoActiveProductsExist() {
        Category category = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndActiveTrue(1L))
            .thenReturn(false);

        categoryService.deactivate(1L);

        assertFalse(category.isActive());
        verify(productRepository).existsByCategoryIdAndActiveTrue(1L);
    }

    @Test
    void deactivateRejectsCategoryWithActiveProducts() {
        Category category = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndActiveTrue(1L))
            .thenReturn(true);

        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> categoryService.deactivate(1L)
        );

        assertEquals("CATEGORY_HAS_ACTIVE_PRODUCTS", exception.getCode());
        assertTrue(category.isActive());
    }

    @Test
    void deactivateThrowsWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> categoryService.deactivate(404L)
        );

        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
        verifyNoInteractions(productRepository);
    }

    private Category category(Long id, String name, Category parent) {
        Category category = new Category(name, null, parent);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}