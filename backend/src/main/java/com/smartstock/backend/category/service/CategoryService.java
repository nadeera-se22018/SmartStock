package com.smartstock.backend.category.service;

import com.smartstock.backend.category.dto.CategoryCreateRequest;
import com.smartstock.backend.category.dto.CategoryResponse;
import com.smartstock.backend.category.dto.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryCreateRequest request);
    CategoryResponse getById(Long id);
    List<CategoryResponse> getAll();
    CategoryResponse update(Long id, CategoryUpdateRequest request);
    void deactivate(Long id);
}
