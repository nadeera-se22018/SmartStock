package com.smartstock.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 255) String description,
    Long parentId
) {
}
