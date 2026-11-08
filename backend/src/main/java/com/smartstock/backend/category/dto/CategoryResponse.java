package com.smartstock.backend.category.dto;

import java.time.Instant;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    Long parentId,
    boolean active,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
}
