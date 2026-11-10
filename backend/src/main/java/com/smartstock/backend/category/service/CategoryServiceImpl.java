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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        CategoryMapper categoryMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        String name = normalizeName(request.name());
        Category parent = resolveParent(request.parentId());
        ensureUnique(name, parent == null ? null : parent.getId(), null);
        return categoryMapper.toResponse(categoryRepository.save(
            categoryMapper.toEntity(new CategoryCreateRequest(name, request.description(), request.parentId()), parent)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = findCategory(id);
        String name = normalizeName(request.name());
        Category parent = resolveParent(request.parentId());
        ensureNotCircular(category, parent);
        ensureUnique(name, parent == null ? null : parent.getId(), id);
        categoryMapper.updateEntity(
            category, new CategoryUpdateRequest(name, request.description(), request.parentId()), parent
        );
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new BusinessRuleException(
                "CATEGORY_HAS_ACTIVE_PRODUCTS",
                "A category with active products cannot be deactivated."
            );
        }
        category.deactivate();
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "CATEGORY_NOT_FOUND", "Category with id '" + id + "' was not found."
            ));
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        Category parent = findCategory(parentId);
        if (!parent.isActive()) {
            throw new BusinessRuleException(
                "CATEGORY_PARENT_INACTIVE", "An inactive category cannot be used as a parent."
            );
        }
        return parent;
    }

    private void ensureUnique(String name, Long parentId, Long id) {
        boolean exists = id == null
            ? categoryRepository.existsByNameIgnoreCaseAndParentId(name, parentId)
            : categoryRepository.existsByNameIgnoreCaseAndParentIdAndIdNot(name, parentId, id);
        if (exists) {
            throw new DuplicateResourceException(
                "CATEGORY_NAME_ALREADY_EXISTS", "A category with this name already exists under the parent."
            );
        }
    }

    private void ensureNotCircular(Category category, Category parent) {
        if (parent == null) {
            return;
        }
        if (category.getId().equals(parent.getId())) {
            throw new BusinessRuleException(
                "CATEGORY_SELF_PARENT", "A category cannot be its own parent."
            );
        }
        Category current = parent;
        while (current != null) {
            if (category.getId().equals(current.getId())) {
                throw new BusinessRuleException(
                    "CATEGORY_CIRCULAR_HIERARCHY", "A category cannot be assigned beneath its own descendant."
                );
            }
            current = current.getParent();
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
