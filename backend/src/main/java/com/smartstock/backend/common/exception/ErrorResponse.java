package com.smartstock.backend.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    String type,
    String title,
    int status,
    String code,
    String detail,
    String path,
    Instant timestamp,
    List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
