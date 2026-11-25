package com.smartstock.backend.purchasing.controller;

import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseStatus;
import com.smartstock.backend.purchasing.service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {
    @Mock
    private PurchaseService purchaseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PurchaseController(purchaseService)).build();
    }

    @Test
    void createReturnsCreatedAndPurchaseLocation() throws Exception {
        PurchaseResponse response = new PurchaseResponse(
            44L, "PO-2026-test", 12L, "Acme", LocalDate.of(2026, 11, 25),
            null, PurchaseStatus.DRAFT, null, null, null, List.of(), null, null
        );
        when(purchaseService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId": 12,
                      "orderDate": "2026-11-25",
                      "items": [{"productId": 101, "quantity": 2, "unitCost": 10.50}]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/purchases/44")));
    }

    @Test
    void createRejectsInvalidQuantityBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId": 12,
                      "orderDate": "2026-11-25",
                      "items": [{"productId": 101, "quantity": 0, "unitCost": 10.50}]
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).create(any());
    }

    @Test
    void receivingReturnsCreatedForFirstRequestAndOkForReplay() throws Exception {
        GoodsReceiptResponse first = new GoodsReceiptResponse(
            900L, "GR-2026-test", 44L, "PO-2026-test", Instant.parse("2026-11-25T10:00:00Z"),
            "inventory-user", PurchaseStatus.PARTIALLY_RECEIVED, null, List.of()
        );
        when(purchaseService.receiveGoods(eq(44L), any(), eq("123e4567-e89b-12d3-a456-426614174000")))
            .thenReturn(first, first.asReplay());
        String request = """
            {
              "items": [{"purchaseOrderItemId": 501, "receivedQuantity": 2}]
            }
            """;

        mockMvc.perform(post("/api/purchases/44/receipts")
                .header("Idempotency-Key", "123e4567-e89b-12d3-a456-426614174000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/purchases/44/receipts/900")));

        mockMvc.perform(post("/api/purchases/44/receipts")
                .header("Idempotency-Key", "123e4567-e89b-12d3-a456-426614174000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk());

        verify(purchaseService, org.mockito.Mockito.times(2))
            .receiveGoods(eq(44L), any(), eq("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void searchRejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/purchases")
                .param("from", "2026-12-01")
                .param("to", "2026-11-01"))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).search(any());
    }

    @Test
    void searchRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/purchases").param("status", "PARTIAL"))
            .andExpect(status().isBadRequest());

        verify(purchaseService, never()).search(any());
    }
}
