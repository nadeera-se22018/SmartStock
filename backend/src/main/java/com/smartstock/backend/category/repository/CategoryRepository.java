package com.smartstock.backend.category.repository;

import com.smartstock.backend.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCaseAndParentId(String name, Long parentId);

    boolean existsByNameIgnoreCaseAndParentIdAndIdNot(String name, Long parentId, Long id);

    boolean existsByParentId(Long parentId);

    boolean existsByIdAndActiveTrue(Long id);
}
