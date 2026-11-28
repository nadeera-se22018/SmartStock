package com.smartstock.backend.inventory.service;

import com.smartstock.backend.auth.entity.User;
import com.smartstock.backend.common.exception.BusinessRuleException;
import com.smartstock.backend.common.exception.DuplicateResourceException;
import com.smartstock.backend.common.exception.InvalidRequestException;
import com.smartstock.backend.common.exception.ResourceNotFoundException;
import com.smartstock.backend.common.web.PageResponse;
import com.smartstock.backend.inventory.dto.InventoryBalanceResponse;
import com.smartstock.backend.inventory.dto.InventorySearchCriteria;
import com.smartstock.backend.inventory.dto.StockAdjustmentDirection;
import com.smartstock.backend.inventory.dto.StockAdjustmentRequest;
import com.smartstock.backend.inventory.dto.StockMovementResponse;
import com.smartstock.backend.inventory.dto.StockMovementSearchCriteria;
import com.smartstock.backend.inventory.entity.InventoryBalance;
import com.smartstock.backend.inventory.entity.StockMovement;
import com.smartstock.backend.inventory.entity.StockMovementType;
import com.smartstock.backend.inventory.mapper.InventoryMapper;
import com.smartstock.backend.inventory.repository.InventoryBalanceRepository;
import com.smartstock.backend.inventory.repository.StockMovementProjection;
import com.smartstock.backend.inventory.repository.StockMovementRepository;
import com.smartstock.backend.product.entity.Product;
import com.smartstock.backend.product.repository.ProductRepository;
import com.smartstock.backend.purchasing.port.InventoryReceivingResult;
import com.smartstock.backend.purchasing.port.PurchaseReceiptLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class InventoryServiceImpl implements InventoryService {
    private static final int MAX_QUANTITY_SCALE = 4;
    private static final int MAX_QUANTITY_INTEGER_DIGITS = 15;

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(
        InventoryBalanceRepository inventoryBalanceRepository,
        StockMovementRepository stockMovementRepository,
        ProductRepository productRepository,
        InventoryMapper inventoryMapper
    ) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryBalanceResponse> searchBalances(InventorySearchCriteria criteria) {
        requirePage(criteria == null ? null : criteria.page(), criteria == null ? null : criteria.size());
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), balanceSort(criteria.sort()));
        String search = criteria.search() == null || criteria.search().isBlank()
            ? null : criteria.search().trim();
        Page<InventoryBalanceResponse> page = inventoryBalanceRepository.search(
            search, criteria.categoryId(), criteria.active(), criteria.stockStatus(), pageable
        ).map(inventoryMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(Long productId) {
        if (productId == null || productId <= 0) {
            throw new InvalidRequestException("Product id must be positive.");
        }
        Product product = productRepository.findById(productId).orElseThrow(() ->
            new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product with id '" + productId + "' was not found.")
        );
        return inventoryMapper.toResponse(
            product,
            inventoryBalanceRepository.findByProductId(productId).orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> searchMovements(StockMovementSearchCriteria criteria) {
        requirePage(criteria == null ? null : criteria.page(), criteria == null ? null : criteria.size());
        if (criteria.from() != null && criteria.to() != null && criteria.from().isAfter(criteria.to())) {
            throw new InvalidRequestException("The from timestamp must not be after the to timestamp.");
        }
        if (criteria.productId() != null && criteria.productId() <= 0
            || criteria.referenceId() != null && criteria.referenceId() <= 0) {
            throw new InvalidRequestException("Product and reference ids must be positive.");
        }
        String referenceType = criteria.referenceType() == null || criteria.referenceType().isBlank()
            ? null : criteria.referenceType().trim();
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), movementSort(criteria.sort()));
        Page<StockMovementResponse> page = stockMovementRepository.search(
            criteria.productId(),
            criteria.movementType(),
            referenceType,
            criteria.referenceId(),
            criteria.from(),
            criteria.to(),
            pageable
        ).map(inventoryMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementResponse getMovement(Long movementId) {
        if (movementId == null || movementId <= 0) {
            throw new InvalidRequestException("Movement id must be positive.");
        }
        return stockMovementRepository.findProjectionById(movementId)
            .map(inventoryMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                "STOCK_MOVEMENT_NOT_FOUND", "Stock movement with id '" + movementId + "' was not found."
            ));
    }

    @Override
    @Transactional
    public InventoryAdjustmentResult adjust(StockAdjustmentRequest request, String idempotencyKey) {
        validateAdjustment(request);
        String key = normalizeIdempotencyKey(idempotencyKey);
        String reason = request.reason().trim();
        String requestHash = hashRequest(request.productId(), request.direction(), request.quantity(), reason);

        InventoryAdjustmentResult previous = replayIfPresent(key, requestHash, false);
        if (previous != null) {
            return previous;
        }

        User actor = authenticatedUser();
        Product product = productRepository.findByIdForUpdate(request.productId()).orElseThrow(() ->
            new ResourceNotFoundException(
                "PRODUCT_NOT_FOUND", "Product with id '" + request.productId() + "' was not found."
            )
        );
        previous = replayIfPresent(key, requestHash, true);
        if (previous != null) {
            return previous;
        }
        requireActiveProduct(
            product,
            "PURCHASE_PRODUCT_INACTIVE",
            "Goods cannot be received for an inactive product."
        );
        InventoryBalance balance = inventoryBalanceRepository.findByProductIdForUpdate(product.getId())
            .orElseGet(() -> inventoryBalanceRepository.save(new InventoryBalance(product)));
        BigDecimal before = balance.getQuantityOnHand();
        StockMovementType movementType = request.direction() == StockAdjustmentDirection.IN
            ? StockMovementType.ADJUSTMENT_IN : StockMovementType.ADJUSTMENT_OUT;
        BigDecimal after = calculateAfter(balance, request.quantity(), movementType);
        if (movementType == StockMovementType.ADJUSTMENT_IN) {
            balance.increaseOnHand(request.quantity());
        } else {
            balance.decreaseOnHand(request.quantity());
        }
        inventoryBalanceRepository.save(balance);

        StockMovement movement = stockMovementRepository.save(new StockMovement(
            product,
            actor.getId(),
            movementType,
            request.quantity(),
            before,
            after,
            "INVENTORY_ADJUSTMENT",
            null,
            reason,
            key,
            requestHash
        ));
        StockMovementResponse response = stockMovementRepository.findProjectionById(movement.getId())
            .map(inventoryMapper::toResponse)
            .orElseGet(() -> inventoryMapper.toResponse(movement, actor.getUsername()));
        return new InventoryAdjustmentResult(response, false);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        Long receivedByUserId,
        List<PurchaseReceiptLine> lines
    ) {
        if (purchaseOrderId == null || purchaseOrderId <= 0
            || goodsReceiptId == null || goodsReceiptId <= 0
            || receivedByUserId == null || receivedByUserId <= 0
            || lines == null || lines.isEmpty()) {
            throw new InvalidRequestException("A valid purchase order, receipt, actor, and receipt lines are required.");
        }
        Map<Long, BigDecimal> quantities = new TreeMap<>();
        for (PurchaseReceiptLine line : lines) {
            if (line.productId() == null || line.productId() <= 0) {
                throw new InvalidRequestException("Receipt product ids must be positive.");
            }
            validateQuantity(line.quantity());
            quantities.merge(line.productId(), line.quantity(), BigDecimal::add);
        }

        Map<Long, Product> products = new TreeMap<>();
        for (Long productId : quantities.keySet()) {
            Product product = productRepository.findByIdForUpdate(productId).orElseThrow(() ->
                new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product with id '" + productId + "' was not found.")
            );
            requireActiveProduct(
                product,
                "INVENTORY_PRODUCT_INACTIVE",
                "Stock cannot be adjusted for an inactive product."
            );
            products.put(productId, product);
        }

        Map<Long, BigDecimal> resultingOnHand = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            InventoryBalance balance = inventoryBalanceRepository.findByProductIdForUpdate(entry.getKey())
                .orElseGet(() -> inventoryBalanceRepository.save(new InventoryBalance(product)));
            BigDecimal before = balance.getQuantityOnHand();
            BigDecimal after = before.add(entry.getValue());
            validateResultingQuantity(after);
            balance.increaseOnHand(entry.getValue());
            inventoryBalanceRepository.save(balance);
            stockMovementRepository.save(new StockMovement(
                product,
                receivedByUserId,
                StockMovementType.PURCHASE_RECEIPT,
                entry.getValue(),
                before,
                after,
                "GOODS_RECEIPT",
                goodsReceiptId,
                "Goods receipt for purchase order " + purchaseOrderId,
                null,
                null
            ));
            resultingOnHand.put(entry.getKey(), after);
        }
        return new InventoryReceivingResult(resultingOnHand);
    }

    private void validateAdjustment(StockAdjustmentRequest request) {
        if (request == null || request.productId() == null || request.productId() <= 0
            || request.direction() == null || request.reason() == null) {
            throw new InvalidRequestException("Product, direction, quantity, and reason are required.");
        }
        if (request.reason().trim().isEmpty() || request.reason().trim().length() > 255) {
            throw new InvalidRequestException("Adjustment reason must contain 1 to 255 characters.");
        }
        validateQuantity(request.quantity());
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.scale() > MAX_QUANTITY_SCALE) {
            throw new InvalidRequestException("Quantity must be positive and have no more than four decimal places.");
        }
        if (quantity.precision() - quantity.scale() > MAX_QUANTITY_INTEGER_DIGITS) {
            throw new InvalidRequestException("Quantity exceeds the supported DECIMAL(19,4) range.");
        }
    }

    private BigDecimal calculateAfter(
        InventoryBalance balance,
        BigDecimal quantity,
        StockMovementType movementType
    ) {
        BigDecimal after = movementType == StockMovementType.ADJUSTMENT_IN
            ? balance.getQuantityOnHand().add(quantity)
            : balance.getQuantityOnHand().subtract(quantity);
        if (movementType == StockMovementType.ADJUSTMENT_OUT
            && after.compareTo(balance.getQuantityReserved()) < 0) {
            throw new BusinessRuleException(
                "INVENTORY_STOCK_INSUFFICIENT",
                "The adjustment would reduce available stock below the reserved quantity."
            );
        }
        validateResultingQuantity(after);
        return after;
    }

    private void validateResultingQuantity(BigDecimal quantity) {
        if (quantity.signum() < 0) {
            throw new BusinessRuleException(
                "INVENTORY_STOCK_INSUFFICIENT", "The resulting inventory quantity cannot be negative."
            );
        }
        if (quantity.scale() > MAX_QUANTITY_SCALE
            || quantity.precision() - quantity.scale() > MAX_QUANTITY_INTEGER_DIGITS) {
            throw new InvalidRequestException("The resulting inventory quantity exceeds DECIMAL(19,4) limits.");
        }
    }

    private InventoryAdjustmentResult replayIfPresent(String key, String requestHash, boolean currentRead) {
        StockMovement previous = (currentRead
            ? stockMovementRepository.findByIdempotencyKeyForUpdate(key)
            : stockMovementRepository.findByIdempotencyKey(key)).orElse(null);
        if (previous == null) {
            return null;
        }
        if (!requestHash.equals(previous.getRequestHash())) {
            throw new DuplicateResourceException(
                "DUPLICATE_RESOURCE", "The idempotency key was already used with a different adjustment."
            );
        }
        StockMovementResponse response = (currentRead
            ? stockMovementRepository.findProjectionByIdempotencyKeyForUpdate(key)
            : stockMovementRepository.findProjectionByIdempotencyKey(key))
            .map(inventoryMapper::toResponse)
            .orElseThrow(() -> new IllegalStateException(
                "The stock movement exists but its response projection could not be loaded."
            ));
        return new InventoryAdjustmentResult(response, true);
    }

    private void requireActiveProduct(Product product, String errorCode, String message) {
        if (!product.isActive()) {
            throw new BusinessRuleException(errorCode, message);
        }
    }

    private User authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof User user) || user.getId() == null) {
            throw new BusinessRuleException(
                "INVENTORY_AUTHENTICATED_USER_REQUIRED", "An authenticated inventory user is required."
            );
        }
        if (!user.isActive()) {
            throw new BusinessRuleException(
                "INVENTORY_USER_INACTIVE", "An inactive user cannot perform inventory adjustments."
            );
        }
        return user;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException("The Idempotency-Key header is required.");
        }
        String key = idempotencyKey.trim();
        if (key.length() < 36 || key.length() > 128) {
            throw new InvalidRequestException("The Idempotency-Key header must contain 36 to 128 characters.");
        }
        return key;
    }

    private String hashRequest(
        Long productId,
        StockAdjustmentDirection direction,
        BigDecimal quantity,
        String reason
    ) {
        String canonical = productId + "\n" + direction.name() + "\n"
            + quantity.stripTrailingZeros().toPlainString() + "\n" + reason;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private void requirePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    private Sort balanceSort(String sort) {
        return resolveSort(sort, Map.of(
            "sku", "sku",
            "name", "productName",
            "quantityOnHand", "quantityOnHand",
            "availableQuantity", "availableQuantity",
            "reorderLevel", "reorderLevel",
            "updatedAt", "updatedAt"
        ), "sku", Sort.Direction.ASC, false);
    }

    private Sort movementSort(String sort) {
        return resolveSort(sort, Map.of(
            "createdAt", "createdAt",
            "movementType", "movementType",
            "productId", "product.id",
            "quantity", "quantity",
            "id", "id"
        ), "createdAt", Sort.Direction.DESC, true);
    }

    private Sort resolveSort(
        String sort,
        Map<String, String> allowedFields,
        String defaultField,
        Sort.Direction defaultDirection,
        boolean appendId
    ) {
        String value = sort == null || sort.isBlank() ? defaultField : sort.trim();
        String[] parts = value.split(",", -1);
        String field = parts[0].trim();
        if (!allowedFields.containsKey(field) || parts.length > 2) {
            throw new InvalidRequestException("The requested sort field is not supported.");
        }
        Sort.Direction direction = defaultDirection;
        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("Sort direction must be asc or desc.");
            }
        }
        Sort result = Sort.by(direction, allowedFields.get(field));
        if (appendId && !field.equals("id")) {
            result = result.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return result;
    }
}
