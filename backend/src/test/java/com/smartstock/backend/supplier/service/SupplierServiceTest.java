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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierPurchaseHistoryReader purchaseHistoryReader;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier supplier;
    private SupplierResponse supplierResponse;

    @BeforeEach
    void setUp() {
        supplier = new Supplier(
            "Acme Supplies",
            "acme supplies",
            "Jane",
            "jane@acme.com",
            "555-0100",
            "Main Street"
        );
        ReflectionTestUtils.setField(supplier, "id", 1L);

        supplierResponse = new SupplierResponse(
            1L,
            "Acme Supplies",
            "Jane",
            "jane@acme.com",
            "555-0100",
            "Main Street",
            true,
            0L,
            null,
            null
        );
    }

    @Test
    void createNormalizesNameAndEmailAndSavesSupplier() {
        SupplierCreateRequest request = new SupplierCreateRequest(
            "  Acme   Supplies  ",
            "Jane",
            "  Jane@Acme.com  ",
            "555-0100",
            "Main Street"
        );

        when(supplierRepository.existsByNormalizedName("acme supplies"))
            .thenReturn(false);
        when(supplierRepository.existsByEmailIgnoreCase("jane@acme.com"))
            .thenReturn(false);
        when(supplierMapper.toEntity(any(SupplierCreateRequest.class), eq("acme supplies")))
            .thenReturn(supplier);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(supplierResponse);

        SupplierResponse result = supplierService.create(request);

        assertSame(supplierResponse, result);

        ArgumentCaptor<SupplierCreateRequest> captor =
            ArgumentCaptor.forClass(SupplierCreateRequest.class);

        verify(supplierMapper).toEntity(captor.capture(), eq("acme supplies"));
        assertEquals("Acme Supplies", captor.getValue().name());
        assertEquals("jane@acme.com", captor.getValue().email());
        verify(supplierRepository).save(supplier);
    }

    @Test
    void createTreatsBlankEmailAsNull() {
        SupplierCreateRequest request = new SupplierCreateRequest(
            "  Acme   Supplies  ",
            "Jane",
            "   ",
            "555-0100",
            null
        );

        when(supplierRepository.existsByNormalizedName("acme supplies"))
            .thenReturn(false);
        when(supplierMapper.toEntity(any(SupplierCreateRequest.class), eq("acme supplies")))
            .thenReturn(supplier);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(supplierResponse);

        assertSame(supplierResponse, supplierService.create(request));

        ArgumentCaptor<SupplierCreateRequest> captor =
            ArgumentCaptor.forClass(SupplierCreateRequest.class);

        verify(supplierMapper).toEntity(captor.capture(), eq("acme supplies"));
        assertNull(captor.getValue().email());
        verify(supplierRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void createRejectsDuplicateNormalizedName() {
        SupplierCreateRequest request = new SupplierCreateRequest(
            "  Acme Supplies ",
            null,
            null,
            null,
            null
        );

        when(supplierRepository.existsByNormalizedName("acme supplies"))
            .thenReturn(true);

        DuplicateResourceException exception =
            assertThrows(DuplicateResourceException.class,
                () -> supplierService.create(request));

        assertEquals("SUPPLIER_NAME_ALREADY_EXISTS", exception.getCode());
        verify(supplierRepository, never()).existsByEmailIgnoreCase(any());
        verify(supplierRepository, never()).save(any(Supplier.class));
        verifyNoInteractions(supplierMapper);
    }

    @Test
    void createRejectsDuplicateEmail() {
        SupplierCreateRequest request = new SupplierCreateRequest(
            "Acme Supplies",
            null,
            " Jane@Acme.com ",
            null,
            null
        );

        when(supplierRepository.existsByNormalizedName("acme supplies"))
            .thenReturn(false);
        when(supplierRepository.existsByEmailIgnoreCase("jane@acme.com"))
            .thenReturn(true);

        DuplicateResourceException exception =
            assertThrows(DuplicateResourceException.class,
                () -> supplierService.create(request));

        assertEquals("SUPPLIER_EMAIL_ALREADY_EXISTS", exception.getCode());
        verify(supplierRepository, never()).save(any(Supplier.class));
        verifyNoInteractions(supplierMapper);
    }

    @Test
    void updateNormalizesValuesAndUpdatesSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNormalizedNameAndIdNot("new supply", 1L))
            .thenReturn(false);
        when(supplierMapper.toResponse(supplier)).thenReturn(supplierResponse);

        SupplierUpdateRequest request = new SupplierUpdateRequest(
            "  New   Supply ",
            "Bob",
            "  ",
            "555",
            "Address"
        );

        SupplierResponse result = supplierService.update(1L, request);

        assertSame(supplierResponse, result);

        verify(supplierMapper).updateEntity(
            eq(supplier),
            argThat(updateRequest ->
                "New Supply".equals(updateRequest.name())
                    && "Bob".equals(updateRequest.contactPerson())
                    && updateRequest.email() == null
                    && "555".equals(updateRequest.phone())
                    && "Address".equals(updateRequest.address())
            ),
            eq("new supply")
        );
    }

    @Test
    void updateRejectsDuplicateNormalizedNameExcludingCurrentSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNormalizedNameAndIdNot("acme supplies", 1L))
            .thenReturn(true);

        SupplierUpdateRequest request = new SupplierUpdateRequest(
            "Acme Supplies",
            "Jane",
            "jane@acme.com",
            "555",
            "Address"
        );

        DuplicateResourceException exception =
            assertThrows(DuplicateResourceException.class,
                () -> supplierService.update(1L, request));

        assertEquals("SUPPLIER_NAME_ALREADY_EXISTS", exception.getCode());
        verify(supplierRepository, never()).existsByEmailIgnoreCaseAndIdNot(any(), eq(1L));
        verify(supplierMapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void updateRejectsDuplicateEmailExcludingCurrentSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNormalizedNameAndIdNot("new supply", 1L))
            .thenReturn(false);
        when(supplierRepository.existsByEmailIgnoreCaseAndIdNot(
            "jane@new-supplier.com",
            1L
        )).thenReturn(true);

        SupplierUpdateRequest request = new SupplierUpdateRequest(
            "New Supply",
            "Bob",
            " Jane@New-Supplier.com ",
            "555",
            "Address"
        );

        DuplicateResourceException exception =
            assertThrows(DuplicateResourceException.class,
                () -> supplierService.update(1L, request));

        assertEquals("SUPPLIER_EMAIL_ALREADY_EXISTS", exception.getCode());
        verify(supplierMapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void searchReturnsPagedSuppliersAndAppliesCriteria() {
        Page<Supplier> repositoryPage = new PageImpl<>(
            List.of(supplier),
            PageRequest.of(2, 10),
            21
        );

        when(supplierMapper.toResponse(supplier)).thenReturn(supplierResponse);
        when(supplierRepository.findAll(
            ArgumentMatchers.<Specification<Supplier>>any(),
            argThat((Pageable pageable) ->
                pageable.getPageNumber() == 2
                    && pageable.getPageSize() == 10
                    && pageable.getSort().getOrderFor("name") != null
                    && pageable.getSort().getOrderFor("name").isAscending()
            )
        )).thenReturn(repositoryPage);

        PageResponse<SupplierResponse> result = supplierService.search(
            new SupplierSearchCriteria(" Acme ", true, 2, 10, "name")
        );

        assertEquals(1, result.content().size());
        assertEquals(2, result.page());
        assertEquals(10, result.size());
        assertEquals(21, result.totalElements());
        assertEquals(3, result.totalPages());
        assertSame(supplierResponse, result.content().getFirst());
    }

    @Test
    void searchFallsBackToNameSortForInvalidSort() {
        Page<Supplier> repositoryPage = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 20),
            0
        );

        when(supplierRepository.findAll(
            ArgumentMatchers.<Specification<Supplier>>any(),
            argThat((Pageable pageable) ->
                pageable.getSort().getOrderFor("name") != null
                    && pageable.getSort().getOrderFor("name").isAscending()
            )
        )).thenReturn(repositoryPage);

        PageResponse<SupplierResponse> result = supplierService.search(
            new SupplierSearchCriteria(null, null, 0, 20, "unsupportedField")
        );

        assertEquals(0, result.totalElements());
        assertEquals(List.of(), result.content());
    }

    @Test
    void getByIdReturnsMappedSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierMapper.toResponse(supplier)).thenReturn(supplierResponse);

        SupplierResponse result = supplierService.getById(1L);

        assertSame(supplierResponse, result);
    }

    @Test
    void getByIdRejectsMissingSupplier() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class,
                () -> supplierService.getById(99L));

        assertEquals("SUPPLIER_NOT_FOUND", exception.getCode());
        verifyNoInteractions(supplierMapper);
    }

    @Test
    void updateRejectsMissingSupplier() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class,
                () -> supplierService.update(
                    99L,
                    new SupplierUpdateRequest("New Supply", null, null, null, null)
                ));

        assertEquals("SUPPLIER_NOT_FOUND", exception.getCode());
        verifyNoInteractions(supplierMapper);
    }

    @Test
    void deactivatePreservesSupplierEntityAndSetsItInactive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deactivate(1L);

        assertFalse(supplier.isActive());
    }

    @Test
    void deactivateRejectsMissingSupplier() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class,
                () -> supplierService.deactivate(99L));

        assertEquals("SUPPLIER_NOT_FOUND", exception.getCode());
    }

    @Test
    void activateRestoresSupplierStatus() {
        supplier.deactivate();
        assertFalse(supplier.isActive());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.activate(1L);

        assertEquals(true, supplier.isActive());
    }

    @Test
    void activateRejectsMissingSupplier() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
            assertThrows(ResourceNotFoundException.class,
                () -> supplierService.activate(99L));

        assertEquals("SUPPLIER_NOT_FOUND", exception.getCode());
    }

    @Test
    void purchaseHistoryRejectsInvalidDateRange() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        InvalidRequestException exception =
            assertThrows(InvalidRequestException.class,
                () -> supplierService.getPurchaseHistory(
                    1L,
                    LocalDate.of(2026, 12, 1),
                    LocalDate.of(2026, 1, 1),
                    null,
                    0,
                    20,
                    "orderDate,desc"
                ));

        assertEquals("The from date must not be after the to date.", exception.getMessage());
        verifyNoInteractions(purchaseHistoryReader);
    }

    @Test
    void purchaseHistoryDelegatesExactPartialStatusForInactiveSupplier() {
        supplier.deactivate();
        PageResponse<SupplierPurchaseSummaryResponse> expected =
            new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(purchaseHistoryReader.getHistory(
            eq(1L), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
            eq("PARTIALLY_RECEIVED"), eq(0), eq(20), eq("orderDate,desc")
        )).thenReturn(expected);

        PageResponse<SupplierPurchaseSummaryResponse> result = supplierService.getPurchaseHistory(
            1L, null, null, "PARTIALLY_RECEIVED", 0, 20, "orderDate,desc"
        );

        assertSame(expected, result);
        verify(purchaseHistoryReader).getHistory(
            eq(1L), ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
            eq("PARTIALLY_RECEIVED"), eq(0), eq(20), eq("orderDate,desc")
        );
    }
}