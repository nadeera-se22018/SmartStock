package com.smartstock.backend.supplier.service;

import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.InvalidRequestException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.supplier.dto.SupplierCreateRequest;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import com.smartstock.backend.supplier.dto.SupplierResponse;
import com.smartstock.backend.supplier.dto.SupplierSearchCriteria;
import com.smartstock.backend.supplier.dto.SupplierUpdateRequest;
import com.smartstock.backend.supplier.entity.Supplier;
import com.smartstock.backend.supplier.mapper.SupplierMapper;
import com.smartstock.backend.supplier.port.SupplierPurchaseHistoryReader;
import com.smartstock.backend.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class SupplierServiceImpl implements SupplierService {
    private static final Set<String> PURCHASE_STATUSES = Set.of(
        "DRAFT", "SUBMITTED", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED"
    );

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierPurchaseHistoryReader purchaseHistoryReader;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper supplierMapper,
                               SupplierPurchaseHistoryReader purchaseHistoryReader) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
        this.purchaseHistoryReader = purchaseHistoryReader;
    }

    @Override @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        String normalizedName = normalizeName(request.name());
        String email = normalizeEmail(request.email());
        ensureUnique(normalizedName, email, null);
        Supplier supplier = supplierMapper.toEntity(
            new SupplierCreateRequest(normalizedNameDisplay(request.name()), request.contactPerson(), email,
                request.phone(), request.address()), normalizedName);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Override @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return supplierMapper.toResponse(findSupplier(id));
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(SupplierSearchCriteria criteria) {
        Sort sort = Sort.by(resolveSort(criteria.sort()));
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), sort);
        Specification<Supplier> specification = (root, query, cb) -> cb.conjunction();
        if (criteria.search() != null && !criteria.search().isBlank()) {
            String search = "%" + criteria.search().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), search),
                cb.like(cb.lower(root.get("contactPerson")), search),
                cb.like(cb.lower(root.get("email")), search),
                cb.like(cb.lower(root.get("phone")), search)
            ));
        }
        if (criteria.active() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
        }
        Page<SupplierResponse> page = supplierRepository.findAll(specification, pageable).map(supplierMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override @Transactional
    public SupplierResponse update(Long id, SupplierUpdateRequest request) {
        Supplier supplier = findSupplier(id);
        String normalizedName = normalizeName(request.name());
        String email = normalizeEmail(request.email());
        ensureUnique(normalizedName, email, id);
        supplierMapper.updateEntity(supplier,
            new SupplierUpdateRequest(normalizedNameDisplay(request.name()), request.contactPerson(), email,
                request.phone(), request.address()), normalizedName);
        return supplierMapper.toResponse(supplier);
    }

    @Override @Transactional
    public void deactivate(Long id) {
        findSupplier(id).deactivate();
    }

    @Override @Transactional
    public void activate(Long id) {
        findSupplier(id).activate();
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<SupplierPurchaseSummaryResponse> getPurchaseHistory(
        Long supplierId, LocalDate from, LocalDate to, String status,
        int page, int size, String sort
    ) {
        findSupplier(supplierId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("The from date must not be after the to date.");
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (!PURCHASE_STATUSES.contains(normalizedStatus)) {
                throw new InvalidRequestException("The purchase status filter is invalid.");
            }
            status = normalizedStatus;
        } else {
            status = null;
        }
        return purchaseHistoryReader.getHistory(supplierId, from, to, status, page, size, sort);
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("SUPPLIER_NOT_FOUND", "Supplier with id '" + id + "' was not found."));
    }

    private void ensureUnique(String normalizedName, String email, Long id) {
        boolean nameExists = id == null
            ? supplierRepository.existsByNormalizedName(normalizedName)
            : supplierRepository.existsByNormalizedNameAndIdNot(normalizedName, id);
        if (nameExists) {
            throw new DuplicateResourceException("SUPPLIER_NAME_ALREADY_EXISTS", "A supplier with this name already exists.");
        }
        if (email != null) {
            boolean emailExists = id == null
                ? supplierRepository.existsByEmailIgnoreCase(email)
                : supplierRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
            if (emailExists) {
                throw new DuplicateResourceException("SUPPLIER_EMAIL_ALREADY_EXISTS", "A supplier with this email already exists.");
            }
        }
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizedNameDisplay(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSort(String sort) {
        if (sort == null) return "name";
        return switch (sort) {
            case "name", "email", "phone", "createdAt", "updatedAt" -> sort;
            default -> "name";
        };
    }
}
