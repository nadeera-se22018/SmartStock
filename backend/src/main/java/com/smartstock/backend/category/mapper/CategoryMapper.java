package com.smartstock.backend.category.mapper;

import com.smartstock.backend.category.dto.CategoryCreateRequest;
import com.smartstock.backend.category.dto.CategoryResponse;
import com.smartstock.backend.category.dto.CategoryUpdateRequest;
import com.smartstock.backend.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getParent() == null ? null : category.getParent().getId(),
            category.isActive(),
            category.getVersion(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    public Category toEntity(CategoryCreateRequest request, Category parent) {
        return new Category(request.name(), request.description(), parent);
    }

    public void updateEntity(Category category, CategoryUpdateRequest request, Category parent) {
        category.updateDetails(request.name(), request.description(), parent);
    }
}
