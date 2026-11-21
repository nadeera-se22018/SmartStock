package com.smartstock.backend.supplier.dto;
public record SupplierSearchCriteria(String search, Boolean active, int page, int size, String sort) {}
