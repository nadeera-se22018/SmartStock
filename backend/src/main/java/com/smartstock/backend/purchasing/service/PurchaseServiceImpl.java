package com.smartstock.backend.purchasing.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.auth.repository.UserRepository;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.InvalidRequestException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.purchasing.dto.GoodsReceiptCreateRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptItemRequest;
import com.smartstock.backend.purchasing.dto.GoodsReceiptResponse;
import com.smartstock.backend.purchasing.dto.PurchaseCreateRequest;
import com.smartstock.backend.purchasing.dto.PurchaseItemRequest;
import com.smartstock.backend.purchasing.dto.PurchaseResponse;
import com.smartstock.backend.purchasing.dto.PurchaseSearchCriteria;
import com.smartstock.backend.purchasing.dto.PurchaseStatus;
import com.smartstock.backend.purchasing.dto.PurchaseUpdateRequest;
import com.smartstock.backend.purchasing.entity.GoodsReceipt;
import com.smartstock.backend.purchasing.entity.GoodsReceiptItem;
import com.smartstock.backend.purchasing.entity.PurchaseOrder;
import com.smartstock.backend.purchasing.entity.PurchaseOrderItem;
import com.smartstock.backend.purchasing.mapper.PurchaseMapper;
import com.smartstock.backend.purchasing.port.InventoryReceivingPort;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import com.smartstock.backend.purchasing.repository.GoodsReceiptRepository;
import com.smartstock.backend.purchasing.repository.PurchaseOrderRepository;
import com.smartstock.backend.supplier.dto.SupplierPurchaseSummaryResponse;
import com.smartstock.backend.supplier.entity.Supplier;
import com.smartstock.backend.supplier.port.SupplierPurchaseHistoryReader;
import com.smartstock.backend.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class PurchaseServiceImpl implements PurchaseService, SupplierPurchaseHistoryReader {
    private static final int DECIMAL_SCALE = 4;
    private static final Set<String> SORT_FIELDS = Set.of(
        "orderDate", "purchaseNumber", "status", "totalAmount", "updatedAt"
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PurchaseMapper purchaseMapper;
    private final InventoryReceivingPort inventoryReceivingPort;

    public PurchaseServiceImpl(
        PurchaseOrderRepository purchaseOrderRepository,
        GoodsReceiptRepository goodsReceiptRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        PurchaseMapper purchaseMapper,
        InventoryReceivingPort inventoryReceivingPort
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.purchaseMapper = purchaseMapper;
        this.inventoryReceivingPort = inventoryReceivingPort;
    }

    @Override
    @Transactional
    public PurchaseResponse create(PurchaseCreateRequest request) {
        Supplier supplier = findActiveSupplier(request.supplierId());
        User createdBy = findAuthenticatedUser();
        PurchaseOrder order = new PurchaseOrder(
            generatePurchaseNumber(request.orderDate()),
            supplier,
            createdBy.getId(),
            request.orderDate(),
            request.expectedDeliveryDate(),
            request.notes()
        );
        List<PurchaseOrderItem> items = createItems(request.items(), order);
        items.forEach(order::addItem);
        BigDecimal subtotal = calculateSubtotal(items);
        order.setSubtotal(subtotal);
        return purchaseMapper.toPurchaseResponse(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getById(Long id) {
        return purchaseMapper.toPurchaseResponse(findOrder(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponse> search(PurchaseSearchCriteria criteria) {
        if (criteria.from() != null && criteria.to() != null && criteria.from().isAfter(criteria.to())) {
            throw new InvalidRequestException("The from date must not be after the to date.");
        }
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), resolveSort(criteria.sort()));
        Specification<PurchaseOrder> specification = (root, query, cb) -> cb.conjunction();
        if (criteria.supplierId() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), criteria.supplierId()));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.from() != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("orderDate"), criteria.from()));
        }
        if (criteria.to() != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("orderDate"), criteria.to()));
        }
        Page<PurchaseResponse> page = purchaseOrderRepository.findAll(specification, pageable)
            .map(purchaseMapper::toPurchaseResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public PurchaseResponse update(Long id, PurchaseUpdateRequest request) {
        PurchaseOrder order = findLockedOrder(id);
        if (order.getStatus() != PurchaseStatus.DRAFT) {
            throw transitionNotAllowed(order);
        }
        Supplier supplier = findActiveSupplier(request.supplierId());
        List<PurchaseOrderItem> replacementItems = createItems(request.items(), order);
        BigDecimal subtotal = calculateSubtotal(replacementItems);
        order.updateDraft(
            supplier,
            request.orderDate(),
            request.expectedDeliveryDate(),
            request.notes(),
            replacementItems,
            subtotal
        );
        return purchaseMapper.toPurchaseResponse(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public PurchaseResponse submit(Long id) {
        PurchaseOrder order = findLockedOrder(id);
        findActiveSupplier(order.getSupplier().getId());
        for (PurchaseOrderItem item : order.getItems()) {
            if (!item.getProduct().isActive()) {
                throw new BusinessRuleException(
                    "PURCHASE_PRODUCT_INACTIVE", "A purchase order with inactive products cannot be submitted."
                );
            }
        }
        order.submit();
        return purchaseMapper.toPurchaseResponse(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public PurchaseResponse cancel(Long id) {
        PurchaseOrder order = findLockedOrder(id);
        order.cancel();
        return purchaseMapper.toPurchaseResponse(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public GoodsReceiptResponse receiveGoods(
        Long purchaseOrderId,
        GoodsReceiptCreateRequest request,
        String idempotencyKey
    ) {
        validateIdempotencyKey(idempotencyKey);
        Map<Long, BigDecimal> requestedQuantities = requestedQuantities(request.items());
        String requestHash = hashRequest(request, requestedQuantities);
        PurchaseOrder order = findLockedOrder(purchaseOrderId);

        if (idempotencyKey != null) {
            GoodsReceipt previousReceipt = goodsReceiptRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (previousReceipt != null) {
                if (!previousReceipt.getPurchaseOrder().getId().equals(purchaseOrderId)
                    || !requestHash.equals(previousReceipt.getRequestHash())) {
                    throw new DuplicateResourceException(
                        "PURCHASE_IDEMPOTENCY_KEY_REUSED",
                        "The idempotency key was already used with a different receipt request."
                    );
                }
                return purchaseMapper.toGoodsReceiptResponse(previousReceipt).asReplay();
            }
        }

        if (order.getStatus() != PurchaseStatus.SUBMITTED
            && order.getStatus() != PurchaseStatus.PARTIALLY_RECEIVED) {
            throw transitionNotAllowed(order);
        }
        Map<Long, PurchaseOrderItem> orderItems = order.getItems().stream()
            .collect(java.util.stream.Collectors.toMap(PurchaseOrderItem::getId, item -> item));
        for (Long itemId : requestedQuantities.keySet()) {
            PurchaseOrderItem item = orderItems.get(itemId);
            if (item == null) {
                throw new ResourceNotFoundException(
                    "PURCHASE_ORDER_ITEM_NOT_FOUND",
                    "Purchase order item with id '" + itemId + "' was not found on this purchase order."
                );
            }
            if (!item.getProduct().isActive()) {
                throw new BusinessRuleException(
                    "PURCHASE_PRODUCT_INACTIVE", "Goods cannot be received for an inactive product."
                );
            }
            if (item.getReceivedQuantity().add(requestedQuantities.get(itemId))
                .compareTo(item.getQuantity()) > 0) {
                throw new BusinessRuleException(
                    "PURCHASE_RECEIPT_QUANTITY_EXCEEDED",
                    "The received quantity cannot exceed the remaining quantity on the purchase order."
                );
            }
        }

        findActiveSupplier(order.getSupplier().getId());
        PurchaseStatus statusAfterReceipt = order.statusAfterReceipt(requestedQuantities);
        User receivedBy = findAuthenticatedUser();
        Instant receivedAt = request.receivedAt() == null ? Instant.now() : request.receivedAt();
        GoodsReceipt receipt = new GoodsReceipt(
            generateReceiptNumber(receivedAt),
            order,
            receivedBy.getId(),
            receivedBy.getUsername(),
            receivedAt,
            request.notes(),
            statusAfterReceipt,
            idempotencyKey,
            idempotencyKey == null ? null : requestHash
        );
        requestedQuantities.forEach((itemId, quantity) -> {
            PurchaseOrderItem orderItem = orderItems.get(itemId);
            receipt.addItem(new GoodsReceiptItem(
                receipt,
                orderItem,
                orderItem.getProduct(),
                quantity,
                orderItem.getUnitCost()
            ));
        });
        GoodsReceipt persistedReceipt = goodsReceiptRepository.saveAndFlush(receipt);

        Map<Long, BigDecimal> quantitiesByProduct = new TreeMap<>();
        requestedQuantities.forEach((itemId, quantity) -> {
            Long productId = orderItems.get(itemId).getProduct().getId();
            quantitiesByProduct.merge(productId, quantity, BigDecimal::add);
        });
        List<PurchaseReceiptLine> inventoryLines = quantitiesByProduct.entrySet().stream()
            .map(entry -> new PurchaseReceiptLine(
                entry.getKey(), entry.getValue(), persistedReceipt.getReceiptNumber()
            ))
            .toList();
        inventoryReceivingPort.applyPurchaseReceipt(
            order.getId(), persistedReceipt.getId(), receivedBy.getId(), inventoryLines
        );

        order.recordReceipt(requestedQuantities);
        purchaseOrderRepository.save(order);
        return purchaseMapper.toGoodsReceiptResponse(persistedReceipt, statusAfterReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptResponse> getReceipts(Long purchaseOrderId, int page, int size) {
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw purchaseNotFound(purchaseOrderId);
        }
        Page<GoodsReceiptResponse> receiptPage = goodsReceiptRepository.findByPurchaseOrderId(
            purchaseOrderId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"))
        ).map(purchaseMapper::toGoodsReceiptResponse);
        return PageResponse.from(receiptPage);
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptResponse getReceiptById(Long purchaseOrderId, Long receiptId) {
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw purchaseNotFound(purchaseOrderId);
        }
        GoodsReceipt receipt = goodsReceiptRepository.findByIdAndPurchaseOrderId(receiptId, purchaseOrderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "GOODS_RECEIPT_NOT_FOUND", "Goods receipt with id '" + receiptId + "' was not found."
            ));
        return purchaseMapper.toGoodsReceiptResponse(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierPurchaseSummaryResponse> getHistory(
        Long supplierId,
        LocalDate from,
        LocalDate to,
        String status,
        int page,
        int size,
        String sort
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("The from date must not be after the to date.");
        }
        PurchaseStatus purchaseStatus = parseStatus(status);
        PurchaseSearchCriteria criteria = new PurchaseSearchCriteria(
            supplierId, purchaseStatus, from, to, page, size, sort
        );
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), resolveSort(criteria.sort()));
        Specification<PurchaseOrder> specification = (root, query, cb) ->
            cb.equal(root.get("supplier").get("id"), supplierId);
        if (purchaseStatus != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("status"), purchaseStatus));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("orderDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("orderDate"), to));
        }
        Page<SupplierPurchaseSummaryResponse> result = purchaseOrderRepository.findAll(specification, pageable)
            .map(purchaseMapper::toSupplierPurchaseSummary);
        return PageResponse.from(result);
    }

    private PurchaseOrder findOrder(Long id) {
        return purchaseOrderRepository.findWithDetailsById(id).orElseThrow(() -> purchaseNotFound(id));
    }

    private PurchaseOrder findLockedOrder(Long id) {
        purchaseOrderRepository.findLockedById(id).orElseThrow(() -> purchaseNotFound(id));
        return findOrder(id);
    }

    private Supplier findActiveSupplier(Long id) {
        Supplier supplier = supplierRepository.findByIdForUpdate(id).orElseThrow(() ->
            new ResourceNotFoundException("SUPPLIER_NOT_FOUND", "Supplier with id '" + id + "' was not found."));
        if (!supplier.isActive()) {
            throw new BusinessRuleException(
                "PURCHASE_SUPPLIER_INACTIVE", "An inactive supplier cannot be used for a purchase order."
            );
        }
        return supplier;
    }

    private List<PurchaseOrderItem> createItems(List<PurchaseItemRequest> requests, PurchaseOrder order) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException(
                "PURCHASE_ITEMS_REQUIRED", "A purchase order must contain at least one item."
            );
        }
        Set<Long> productIds = new java.util.HashSet<>();
        List<PurchaseOrderItem> items = new java.util.ArrayList<>();
        for (PurchaseItemRequest request : requests) {
            if (!productIds.add(request.productId())) {
                throw new BusinessRuleException(
                    "PURCHASE_DUPLICATE_PRODUCT", "A product may only appear once in a purchase order."
                );
            }
            Product product = productRepository.findById(request.productId()).orElseThrow(() ->
                new ResourceNotFoundException(
                    "PRODUCT_NOT_FOUND", "Product with id '" + request.productId() + "' was not found."
                ));
            if (!product.isActive()) {
                throw new BusinessRuleException(
                    "PURCHASE_PRODUCT_INACTIVE", "An inactive product cannot be added to a purchase order."
                );
            }
            if (request.quantity() == null || request.quantity().signum() <= 0
                || request.unitCost() == null || request.unitCost().signum() < 0) {
                throw new BusinessRuleException(
                    "PURCHASE_ITEM_INVALID", "Purchase quantities must be positive and unit costs cannot be negative."
                );
            }
            BigDecimal quantity = scaleSafeDecimal(request.quantity(), "quantity");
            BigDecimal unitCost = scaleSafeDecimal(request.unitCost(), "unit cost");
            PurchaseOrderItem item = new PurchaseOrderItem(order, product, quantity, unitCost);
            requireDatabaseDecimal(item.getLineTotal(), "line total");
            items.add(item);
        }
        return items;
    }

    private BigDecimal calculateSubtotal(List<PurchaseOrderItem> items) {
        BigDecimal subtotal = items.stream()
            .map(PurchaseOrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
        requireDatabaseDecimal(subtotal, "purchase total");
        return subtotal;
    }

    private User findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessRuleException(
                "PURCHASE_AUTHENTICATED_USER_REQUIRED", "An authenticated user is required for this operation."
            );
        }
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(() ->
            new ResourceNotFoundException(
                "USER_NOT_FOUND", "Authenticated user '" + authentication.getName() + "' was not found."
            ));
        if (!user.isActive()) {
            throw new BusinessRuleException(
                "PURCHASE_USER_INACTIVE", "An inactive user cannot perform this operation."
            );
        }
        return user;
    }

    private Map<Long, BigDecimal> requestedQuantities(List<GoodsReceiptItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("PURCHASE_RECEIPT_ITEMS_REQUIRED", "A receipt must contain at least one item.");
        }
        Map<Long, BigDecimal> quantities = new TreeMap<>();
        for (GoodsReceiptItemRequest request : requests) {
            if (request.purchaseOrderItemId() == null || request.receivedQuantity() == null
                || request.receivedQuantity().signum() <= 0) {
                throw new BusinessRuleException(
                    "PURCHASE_RECEIPT_ITEM_INVALID", "Received quantities must be greater than zero."
                );
            }
            BigDecimal quantity = scaleSafeDecimal(request.receivedQuantity(), "received quantity");
            if (quantities.putIfAbsent(request.purchaseOrderItemId(), quantity) != null) {
                throw new BusinessRuleException(
                    "PURCHASE_RECEIPT_DUPLICATE_ITEM", "A purchase order item may only appear once per receipt."
                );
            }
        }
        return quantities;
    }

    private String hashRequest(GoodsReceiptCreateRequest request, Map<Long, BigDecimal> quantities) {
        StringBuilder canonical = new StringBuilder();
        appendHashToken(canonical, request.receivedAt() == null ? null : request.receivedAt().toString());
        appendHashToken(canonical, request.notes());
        quantities.forEach((itemId, quantity) -> {
            appendHashToken(canonical, itemId.toString());
            appendHashToken(canonical, quantity.stripTrailingZeros().toPlainString());
        });
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }

    private void appendHashToken(StringBuilder canonical, String value) {
        if (value == null) {
            canonical.append("-1:");
        } else {
            canonical.append(value.length()).append(':').append(value);
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key != null && (key.length() < 36 || key.length() > 128 || key.isBlank())) {
            throw new InvalidRequestException(
                "The Idempotency-Key must be a UUID or an opaque value between 36 and 128 characters."
            );
        }
    }

    private BigDecimal scaleSafeDecimal(BigDecimal value, String field) {
        try {
            BigDecimal normalized = value.setScale(DECIMAL_SCALE, RoundingMode.UNNECESSARY);
            requireDatabaseDecimal(normalized, field);
            return normalized;
        } catch (ArithmeticException exception) {
            throw new InvalidRequestException(
                "The " + field + " must not have more than four decimal places."
            );
        }
    }

    private void requireDatabaseDecimal(BigDecimal value, String field) {
        if (value.precision() > 19 || value.precision() - value.scale() > 15) {
            throw new InvalidRequestException("The " + field + " exceeds the supported numeric range.");
        }
    }

    private Sort resolveSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.split(",", -1);
        String field = parts.length == 0 ? "" : parts[0];
        if (!SORT_FIELDS.contains(field) || parts.length > 2) {
            throw new InvalidRequestException("The purchase sort field is invalid.");
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("The purchase sort direction is invalid.");
            }
        }
        return Sort.by(direction, field);
    }

    private PurchaseStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return PurchaseStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("The purchase status filter is invalid.");
        }
    }

    private String generatePurchaseNumber(LocalDate orderDate) {
        return uniqueNumber("PO", orderDate.getYear(), purchaseOrderRepository::existsByPurchaseNumberIgnoreCase);
    }

    private String generateReceiptNumber(Instant receivedAt) {
        return uniqueNumber(
            "GR", receivedAt.atZone(ZoneOffset.UTC).getYear(), goodsReceiptRepository::existsByReceiptNumberIgnoreCase
        );
    }

    private String uniqueNumber(String prefix, int year, java.util.function.Predicate<String> exists) {
        for (int attempt = 0; attempt < 8; attempt++) {
            String number = prefix + "-" + year + "-" + UUID.randomUUID();
            if (!exists.test(number)) {
                return number;
            }
        }
        throw new DuplicateResourceException(
            "PURCHASE_NUMBER_GENERATION_FAILED", "A unique purchase document number could not be generated."
        );
    }

    private BusinessRuleException transitionNotAllowed(PurchaseOrder order) {
        return new BusinessRuleException(
            "PURCHASE_STATUS_TRANSITION_NOT_ALLOWED",
            "The purchase order cannot transition from " + order.getStatus() + " for this operation."
        );
    }

    private ResourceNotFoundException purchaseNotFound(Long id) {
        return new ResourceNotFoundException(
            "PURCHASE_NOT_FOUND", "Purchase order with id '" + id + "' was not found."
        );
    }
}
