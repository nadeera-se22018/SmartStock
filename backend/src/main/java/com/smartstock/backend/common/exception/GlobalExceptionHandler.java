package com.smartstock.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException exception, HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.name(),
            "Validation failed.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception, HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> errors = exception.getConstraintViolations().stream()
            .map(error -> new ErrorResponse.FieldError(error.getPropertyPath().toString(), error.getMessage()))
            .toList();
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.name(),
            "Validation failed.", request, errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
        DuplicateResourceException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
        BusinessRuleException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
        DataIntegrityViolationException exception, HttpServletRequest request
    ) {
        log.warn("Database constraint violation at {}", request.getRequestURI(), exception);
        return response(HttpStatus.CONFLICT, ErrorCode.DATABASE_CONSTRAINT_VIOLATION.name(),
            "The request conflicts with existing data.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED.name(),
            "You do not have permission to perform this operation.", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
        AuthenticationException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_REQUIRED.name(),
            "Authentication is required.", request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
        HttpMessageNotReadableException exception, HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST.name(),
            "The request body is malformed.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
        Exception exception, HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}", request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name(),
            "An unexpected error occurred.", request, List.of());
    }

    private ResponseEntity<ErrorResponse> response(
        HttpStatus status, String code, String detail, HttpServletRequest request,
        List<ErrorResponse.FieldError> fieldErrors
    ) {
        String title = switch (status.value()) {
            case 400 -> "Bad request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 409 -> "Resource conflict";
            default -> "Internal server error";
        };
        ErrorResponse body = new ErrorResponse(
            "https://smartstock.local/errors/" + code.toLowerCase().replace('_', '-'),
            title, status.value(), code, detail, request.getRequestURI(), Instant.now(), fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
