package com.smartstock.backend.supplier.service;

import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.supplier.dto.SupplierCreateRequest;
import com.smartstock.backend.supplier.dto.SupplierResponse;
import com.smartstock.backend.supplier.dto.SupplierUpdateRequest;
import com.smartstock.backend.supplier.entity.Supplier;
import com.smartstock.backend.supplier.mapper.SupplierMapper;
import com.smartstock.backend.supplier.port.SupplierPurchaseHistoryReader;
import com.smartstock.backend.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {
    @Mock SupplierRepository supplierRepository;
    @Mock SupplierMapper supplierMapper;
    @Mock SupplierPurchaseHistoryReader purchaseHistoryReader;
    @InjectMocks SupplierServiceImpl supplierService;

    private Supplier supplier;
    private SupplierResponse response;

    @BeforeEach
    void setUp() {
        supplier = new Supplier("Acme Supplies", "acme supplies", "Jane", "jane@acme.com", "555-0100", "Main Street");
        ReflectionTestUtils.setField(supplier, "id", 1L);
        response = new SupplierResponse(1L, "Acme Supplies", "Jane", "jane@acme.com", "555-0100", "Main Street", true, 0L, null, null);
    }

    @Test
    void createNormalizesNameAndBlankEmail() {
        SupplierCreateRequest request = new SupplierCreateRequest("  Acme   Supplies  ", "Jane", "  ", "555-0100", null);
        when(supplierRepository.existsByNormalizedName("acme supplies")).thenReturn(false);
        when(supplierMapper.toEntity(any(), eq("acme supplies"))).thenReturn(supplier);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);
        assertSame(response, supplierService.create(request));
        ArgumentCaptor<SupplierCreateRequest> captor = ArgumentCaptor.forClass(SupplierCreateRequest.class);
        verify(supplierMapper).toEntity(captor.capture(), eq("acme supplies"));
        assertEquals("Acme Supplies", captor.getValue().name());
        assertNull(captor.getValue().email());
    }

    @Test
    void createRejectsDuplicateNormalizedName() {
        when(supplierRepository.existsByNormalizedName("acme supplies")).thenReturn(true);
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
            () -> supplierService.create(new SupplierCreateRequest(" Acme Supplies ", null, null, null, null)));
        assertEquals("SUPPLIER_NAME_ALREADY_EXISTS", exception.getCode());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(supplierRepository.existsByNormalizedName("acme supplies")).thenReturn(false);
        when(supplierRepository.existsByEmailIgnoreCase("jane@acme.com")).thenReturn(true);
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
            () -> supplierService.create(new SupplierCreateRequest("Acme Supplies", null, " Jane@Acme.com ", null, null)));
        assertEquals("SUPPLIER_EMAIL_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void updateNormalizesAndUpdatesSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNormalizedNameAndIdNot("new supply", 1L)).thenReturn(false);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);
        supplierService.update(1L, new SupplierUpdateRequest(" New  Supply ", "Bob", null, "555", "Address"));
        verify(supplierMapper).updateEntity(eq(supplier), argThat(request ->
            "New Supply".equals(request.name()) && request.email() == null), eq("new supply"));
    }

    @Test
    void deactivatePreservesSupplierAndSetsInactive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        supplierService.deactivate(1L);
        assertFalse(supplier.isActive());
    }

    @Test
    void missingSupplierIsRejected() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> supplierService.getById(99L));
        assertEquals("SUPPLIER_NOT_FOUND", exception.getCode());
    }

    @Test
    void purchaseHistoryRejectsInvalidDateRange() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        assertThrows(BusinessRuleException.class,
            () -> supplierService.getPurchaseHistory(1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1), null, 0, 20, "orderDate,desc"));
        verifyNoInteractions(purchaseHistoryReader);
    }
}
