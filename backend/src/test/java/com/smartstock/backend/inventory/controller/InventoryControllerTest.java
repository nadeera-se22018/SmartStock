package com.smartstock.backend.inventory.controller;

import com.smartstock.backend.inventory.dto.StockAdjustmentDirection;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.entity.StockMovementType;
import com.smartstock.backend.inventory.service.InventoryAdjustmentResult;
import com.smartstock.backend.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {
    @Mock
    private InventoryService inventoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new InventoryController(inventoryService)).build();
    }

    @Test
    void adjustmentReturnsCreatedAndMovementLocation() throws Exception {
        String key = "123e4567-e89b-12d3-a456-426614174000";
        when(inventoryService.adjust(any(), eq(key))).thenReturn(new InventoryAdjustmentResult(
            movement(17L), false
        ));

        mockMvc.perform(post("/api/inventory/adjustments")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 7,
                      "direction": "IN",
                      "quantity": 2.5000,
                      "reason": "Physical count correction"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(
                "/api/inventory/movements/17"
            )));

        verify(inventoryService).adjust(
            new StockAdjustmentRequest(7L, StockAdjustmentDirection.IN,
                new BigDecimal("2.5000"), "Physical count correction"),
            key
        );
    }

    @Test
    void adjustmentReplayReturnsOkWithoutCreatedLocation() throws Exception {
        String key = "123e4567-e89b-12d3-a456-426614174000";
        when(inventoryService.adjust(any(), eq(key))).thenReturn(new InventoryAdjustmentResult(
            movement(17L), true
        ));

        mockMvc.perform(post("/api/inventory/adjustments")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 7,
                      "direction": "IN",
                      "quantity": 2.5000,
                      "reason": "Physical count correction"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void invalidAdjustmentDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/inventory/adjustments")
                .header("Idempotency-Key", "123e4567-e89b-12d3-a456-426614174000")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 7,
                      "direction": "OUT",
                      "quantity": 0,
                      "reason": "Physical count correction"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(inventoryService, never()).adjust(any(), any());
    }

    @Test
    void authorizationAllowsInventoryStaffToAdjustAndKeepsManagersReadOnly() throws Exception {
        String adjustmentRoles = InventoryController.class
            .getMethod("adjust", StockAdjustmentRequest.class, String.class)
            .getAnnotation(PreAuthorize.class)
            .value();
        String balanceReadRoles = InventoryController.class
            .getMethod("getBalance", Long.class)
            .getAnnotation(PreAuthorize.class)
            .value();

        assertEquals("hasAnyRole('ADMIN', 'INVENTORY_STAFF')", adjustmentRoles);
        assertEquals("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_STAFF')", balanceReadRoles);
    }

    private StockMovementResponse movement(Long id) {
        return new StockMovementResponse(
            id, 7L, "STOCK-7", "Stock item", StockMovementType.ADJUSTMENT_IN,
            new BigDecimal("2.5000"), new BigDecimal("4.0000"), new BigDecimal("6.5000"),
            "INVENTORY_ADJUSTMENT", null, "Physical count correction", 41L, "inventory-user",
            Instant.parse("2026-11-28T00:00:00Z")
        );
    }
}
