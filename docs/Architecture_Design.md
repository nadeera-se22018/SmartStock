Read `SKILL.md`
Completed with input: {
  "path": "d:\\program\\SmartStock\\skills\\bmad-agent-architect\\SKILL.md"
}

Search for `EPIC-02|Phase 1|Product foundation`

Search for `EPIC-02|Phase 1|Product foundation`

Read , lines 154 to 260
Completed with input: {
  "path": "d:\\program\\SmartStock\\docs\\BA_Agile_Backlog.md",
  "view_range": [
    154,
    260
  ]
}

Read , lines 628 to 646
Completed with input: {
  "path": "d:\\program\\SmartStock\\docs\\BA_Agile_Backlog.md",
  "view_range": [
    628,
    646
  ]
}

Read , lines 1 to 90
Completed with input: {
  "path": "d:\\program\\SmartStock\\docs\\BA_Agile_Backlog.md",
  "view_range": [
    1,
    90
  ]
}

🧭 Winston here. I’ve reconciled the proposal with EPIC-02’s acceptance rules and build sequence. I’m checking the repository’s current backend conventions now so the architecture is exact rather than generic.

Find files matching `**/*`

Search for `spring-boot|pom.xml|build.gradle|src/main/java|@RestController|Product|Category`

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\pom.xml"
}

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\src\\main\\resources\\schema.sql"
}

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\src\\main\\resources\\application.properties"
}

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\src\\main\\java\\com\\smartstock\\backend\\security\\SecurityConfig.java"
}

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\src\\main\\java\\com\\smartstock\\backend\\auth\\entity\\User.java"
}

Read 
Completed with input: {
  "path": "d:\\program\\SmartStock\\backend\\src\\main\\java\\com\\smartstock\\backend\\auth\\entity\\Role.java"
}

🧭 **Winston — System Architect**

This architecture is based on the `SmartStock System Proposal`, `BA Agile Backlog`, and current backend conventions in `SecurityConfig.java`.

# Phase 1 Architecture — EPIC-02

## 1. Scope

Phase 1 delivers:

1. Product and Category entities
2. Repositories
3. Product and Category CRUD
4. Validation and duplicate handling
5. Product search, filtering, barcode lookup, pagination
6. Product authorization
7. Backend unit and integration tests

The implementation must preserve:

- Historical transaction integrity
- Soft deactivation instead of deletion
- Backend ownership of business rules
- Future compatibility with inventory, purchasing, sales, and reporting modules

---

# 2. Architectural Style

Use a **feature-oriented layered architecture**.

Each feature owns its:

- Controller
- DTOs
- Entity
- Mapper
- Repository
- Service interface
- Service implementation

Shared concerns belong in `common`.

```text
HTTP Request
    ↓
Controller
    ↓
Request DTO validation
    ↓
Service interface
    ↓
Service implementation
    ↓
Repository
    ↓
MySQL
```

Controllers must never:

- Contain business rules
- Access repositories directly
- Return JPA entities
- Calculate financial values
- Trust frontend authorization or identifiers

---

# 3. Required Folder Structure

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── smartstock/
    │   │           └── backend/
    │   │               ├── SmartStockBackendApplication.java
    │   │               │
    │   │               ├── common/
    │   │               │   ├── exception/
    │   │               │   │   ├── GlobalExceptionHandler.java
    │   │               │   │   ├── ErrorCode.java
    │   │               │   │   ├── ResourceNotFoundException.java
    │   │               │   │   ├── DuplicateResourceException.java
    │   │               │   │   ├── BusinessRuleException.java
    │   │               │   │   └── ErrorResponse.java
    │   │               │   │
    │   │               │   ├── web/
    │   │               │   │   ├── ApiResponse.java
    │   │               │   │   └── PageResponse.java
    │   │               │   │
    │   │               │   ├── persistence/
    │   │               │   │   └── AuditableEntity.java
    │   │               │   │
    │   │               │   └── validation/
    │   │               │       └── ValidationConstants.java
    │   │               │
    │   │               ├── auth/
    │   │               │   └── existing authentication packages
    │   │               │
    │   │               ├── security/
    │   │               │   └── existing JWT and security packages
    │   │               │
    │   │               ├── category/
    │   │               │   ├── controller/
    │   │               │   │   └── CategoryController.java
    │   │               │   ├── dto/
    │   │               │   │   ├── CategoryCreateRequest.java
    │   │               │   │   ├── CategoryUpdateRequest.java
    │   │               │   │   └── CategoryResponse.java
    │   │               │   ├── entity/
    │   │               │   │   └── Category.java
    │   │               │   ├── mapper/
    │   │               │   │   └── CategoryMapper.java
    │   │               │   ├── repository/
    │   │               │   │   └── CategoryRepository.java
    │   │               │   └── service/
    │   │               │       ├── CategoryService.java
    │   │               │       └── CategoryServiceImpl.java
    │   │               │
    │   │               └── product/
    │   │                   ├── controller/
    │   │                   │   └── ProductController.java
    │   │                   ├── dto/
    │   │                   │   ├── ProductCreateRequest.java
    │   │                   │   ├── ProductUpdateRequest.java
    │   │                   │   ├── ProductResponse.java
    │   │                   │   └── ProductSearchCriteria.java
    │   │                   ├── entity/
    │   │                   │   └── Product.java
    │   │                   ├── mapper/
    │   │                   │   └── ProductMapper.java
    │   │                   ├── repository/
    │   │                   │   └── ProductRepository.java
    │   │                   └── service/
    │   │                       ├── ProductService.java
    │   │                       └── ProductServiceImpl.java
    │   │
    │   ├── resources/
    │   │   ├── application.properties
    │   │   └── db/
    │   │       └── migration/
    │   │           ├── V1__initial_schema.sql
    │   │           └── V2__product_category_schema.sql
    │   │
    │   └── test/
    │       └── java/
    │           └── com/
    │               └── smartstock/
    │                   └── backend/
    │                       ├── product/
    │                       │   ├── ProductServiceTest.java
    │                       │   ├── ProductControllerTest.java
    │                       │   └── ProductRepositoryTest.java
    │                       └── category/
    │                           ├── CategoryServiceTest.java
    │                           └── CategoryControllerTest.java
```

The existing `auth` and `security` packages should remain compatible with this structure.

---

# 4. Domain Model

## Product

Required fields:

```text
id
sku
barcode
name
description
costPrice
sellingPrice
reorderLevel
active
version
createdAt
updatedAt
category
```

Rules:

- `sku`: required, trimmed, normalized, unique
- `barcode`: optional, unique when supplied
- Prices and reorder level: non-negative
- Product starts active
- Deactivation is soft delete
- Historical products must not be physically deleted
- `BigDecimal` must be used for all monetary values
- `@Version` should protect against lost updates

Recommended database types:

```text
cost_price       DECIMAL(19,4)
selling_price    DECIMAL(19,4)
reorder_level    DECIMAL(19,4)
```

## Category

```text
id
name
description
parent
active
version
createdAt
updatedAt
```

Rules:

- Category name is unique within its parent
- Root category names are unique
- Categories with active products cannot be deleted
- Deactivation preserves product history
- Circular parent relationships are rejected
- A category cannot be its own parent

Use a self-referencing relationship:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private Category parent;
```

Avoid bidirectional child collections unless required. They often create serialization and performance problems.

---

# 5. DTO Policy

Never expose entities from REST endpoints.

Use Java records for immutable request and response DTOs.

Example:

```java
public record ProductCreateRequest(
    @NotBlank @Size(max = 50) String sku,
    @Size(max = 100) String barcode,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("0.00") BigDecimal costPrice,
    @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
    @NotNull @DecimalMin("0.00") BigDecimal reorderLevel,
    Long categoryId
) {}
```

The response DTO should contain:

```text
id
sku
barcode
name
description
costPrice
sellingPrice
reorderLevel
active
category
availableQuantity
createdAt
updatedAt
```

`availableQuantity` requires a stock-reading boundary. Do not invent inventory values in the product service. Define a future-facing interface:

```java
public interface ProductStockReader {
    BigDecimal getAvailableQuantity(Long productId);
}
```

The inventory module will provide the implementation. Until then, this requirement must either be explicitly marked unavailable or a minimal zero-balance foundation must be created. It must not be silently hardcoded in multiple places.

---

# 6. Mapper Pattern

Use dedicated mapper classes:

```java
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        // Entity-to-response mapping only
    }

    public Product toEntity(ProductCreateRequest request, Category category) {
        // Request-to-entity mapping only
    }

    public void updateEntity(
        Product product,
        ProductUpdateRequest request,
        Category category
    ) {
        // Controlled update mapping
    }
}
```

Mappers must not:

- Perform database lookups
- Apply authorization
- Contain business decisions
- Calculate inventory or totals

The service resolves `categoryId` and passes the resolved category to the mapper.

---

# 7. Service Layer

Define interfaces first.

```java
public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse getById(Long id);

    PageResponse<ProductResponse> search(ProductSearchCriteria criteria);

    ProductResponse getByBarcode(String barcode);

    ProductResponse update(Long id, ProductUpdateRequest request);

    void deactivate(Long id);
}
```

Implementation rules:

- `@Transactional` on write methods
- `@Transactional(readOnly = true)` on read methods
- Validate business rules in the service
- Perform duplicate checks before persistence
- Still rely on database constraints for race-condition protection
- Throw domain-specific exceptions
- Never return `Optional` from controller methods

Example responsibilities:

```text
ProductServiceImpl
├── normalize SKU and barcode
├── resolve category
├── check duplicate identifiers
├── create/update/deactivate product
├── enforce historical deletion rules
├── call ProductStockReader for availability
└── map entity to response
```

Category service must additionally enforce:

```text
- duplicate name under same parent
- no self-parenting
- no circular hierarchy
- no deletion while active products exist
```

---

# 8. Repository Design

```java
public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    Optional<Product> findByBarcode(String barcode);
}
```

Search must use database-side filtering, not Java stream filtering.

Supported criteria:

```text
search
categoryId
active
page
size
sort
```

The search implementation may use:

- Spring Data `Specification`
- QueryDSL, if already available
- Explicit JPQL queries for this small scope

Do not introduce QueryDSL solely for Phase 1 unless query complexity demands it.

Category repository:

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCaseAndParentId(String name, Long parentId);

    boolean existsByNameIgnoreCaseAndParentIdAndIdNot(
        String name,
        Long parentId,
        Long id
    );

    boolean existsByParentId(Long parentId);

    boolean existsByIdAndActiveTrue(Long id);
}
```

---

# 9. REST API Contract

## Products

```text
POST   /api/products
GET    /api/products/{id}
GET    /api/products
GET    /api/products/barcode/{barcode}
PUT    /api/products/{id}
PATCH  /api/products/{id}/deactivate
```

Example search:

```text
GET /api/products?search=milk&categoryId=2&active=true&page=0&size=20
```

## Categories

```text
POST   /api/categories
GET    /api/categories/{id}
GET    /api/categories
PUT    /api/categories/{id}
PATCH  /api/categories/{id}/deactivate
```

Use consistent HTTP semantics:

| Operation | Response |
|---|---:|
| Create | `201 Created` |
| Read | `200 OK` |
| Update | `200 OK` |
| Deactivate | `204 No Content` |
| Invalid request | `400 Bad Request` |
| Unauthenticated | `401 Unauthorized` |
| Unauthorized | `403 Forbidden` |
| Not found | `404 Not Found` |
| Duplicate | `409 Conflict` |

---

# 10. Global Exception Handling

Implement one `@RestControllerAdvice`.

Use RFC 7807-style responses:

```json
{
  "type": "https://smartstock.local/errors/duplicate-resource",
  "title": "Duplicate resource",
  "status": 409,
  "code": "PRODUCT_SKU_ALREADY_EXISTS",
  "detail": "A product with SKU 'ABC-001' already exists.",
  "path": "/api/products",
  "timestamp": "2026-11-08T10:00:00Z",
  "fieldErrors": []
}
```

Handle at minimum:

```text
MethodArgumentNotValidException
ConstraintViolationException
ResourceNotFoundException
DuplicateResourceException
BusinessRuleException
DataIntegrityViolationException
AccessDeniedException
AuthenticationException
HttpMessageNotReadableException
Exception
```

The generic handler must log the exception and return a non-sensitive error. It must not hide validation or database failures behind a successful response.

---

# 11. Authorization Matrix

The existing security setup already enables method security in `SecurityConfig.java`.

Recommended method-level rules:

| Operation | Roles |
|---|---|
| Product/category lookup | `ADMIN`, `MANAGER`, `SALES_STAFF`, `INVENTORY_STAFF` |
| Product/category creation | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |
| Product/category update | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |
| Product/category deactivation | `ADMIN`, `MANAGER` |
| Category deletion | Not exposed; use deactivation |

Use `@PreAuthorize` on controller or service methods. Keep authorization server-side even when frontend routes are protected.

---

# 12. Database Rules

The current project uses `ddl-auto=validate` in `application.properties`. Therefore schema changes must be versioned.

Use Flyway or an equivalent migration mechanism before adding EPIC-02 tables.

Required constraints:

```text
products.sku       NOT NULL UNIQUE
products.barcode   UNIQUE, nullable
products.name      NOT NULL
products.active    NOT NULL
categories.name    NOT NULL
products.category_id FOREIGN KEY categories.id
categories.parent_id FOREIGN KEY categories.id
```

Important MySQL detail: a normal unique constraint involving nullable `parent_id` may not reliably enforce unique root category names. Root-category uniqueness must be enforced deliberately through:

- A normalized parent scope column, or
- A generated database column, or
- A transactional service check combined with a database-safe unique strategy

Do not rely only on `existsBy...` checks because concurrent requests can bypass them.

---

# 13. Testing Requirements

## Unit tests

`ProductServiceTest` must cover:

- Successful creation
- Duplicate SKU
- Duplicate barcode
- Missing category
- Negative price
- Update to duplicate SKU
- Deactivation
- Missing product
- Historical product cannot be deleted

`CategoryServiceTest` must cover:

- Root category creation
- Child category creation
- Duplicate sibling name
- Self-parenting
- Circular hierarchy
- Deactivation
- Category with active products cannot be deleted

## Web tests

`@WebMvcTest` should verify:

- Request validation
- Response status codes
- JSON response shape
- Authorization rules
- Error response format

## Repository/integration tests

Use MySQL-compatible integration tests where possible. Verify:

- Unique constraints
- Pagination
- Case-insensitive search
- Barcode lookup
- Category hierarchy
- Soft deactivation

Do not consider the epic complete until both service and endpoint tests pass.

---

# 14. Required Engineering Practices

The developer must follow these rules:

1. Use constructor injection only.
2. Keep entities free from REST concerns.
3. Do not use Lombok-generated unrestricted setters on entities.
4. Normalize SKU and barcode consistently.
5. Use `BigDecimal`, never `double`, for money.
6. Use pagination defaults and maximum page-size limits.
7. Use `LAZY` relationships by default.
8. Avoid exposing entity relationships through JSON.
9. Use database constraints in addition to service validation.
10. Use soft deactivation for products and categories.
11. Keep transaction boundaries in services.
12. Do not silently catch exceptions.
13. Do not trust frontend totals, category IDs, or authorization.
14. Document every endpoint with OpenAPI annotations.
15. Keep API response formats consistent.
16. Add indexes for SKU, barcode, name, active status, and category.
17. Do not introduce inventory mutation logic into the product service.
18. Ensure schema migrations work from a clean database.

---

# 15. Definition of Ready Before Coding

The developer should not begin implementation until these decisions are confirmed:

- [ ] Product and category table names are approved.
- [ ] Product monetary precision is approved.
- [ ] SKU and barcode normalization rules are approved.
- [ ] Category root-name uniqueness strategy is selected.
- [ ] Product `availableQuantity` integration is resolved.
- [ ] Authorization matrix is accepted.
- [ ] API response/error format is accepted.
- [ ] Migration strategy is selected.
- [ ] Pagination defaults and limits are defined.
- [ ] Deactivation versus deletion behavior is confirmed.
- [ ] Required tests are listed in the pull request checklist.

**Architectural recommendation:** implement EPIC-02 as an independently testable product/category module, but establish the shared exception, DTO, mapper, pagination, migration, and authorization conventions now. Every later epic—purchasing, inventory, sales, reporting, and audit—should reuse these conventions rather than create parallel implementations.

---

# 16. Phase 1 Architecture — EPIC-03 Supplier Management

## 16.1 Scope

EPIC-03 delivers:

1. A `Supplier` entity and versioned database schema.
2. Supplier create, update, read, search/list, and soft-deactivation operations.
3. Duplicate supplier detection where practical.
4. Supplier purchase-history retrieval when EPIC-04 purchase records exist.
5. Server-side authorization and consistent RFC 7807 error responses.

EPIC-03 must not implement purchase orders, goods receipts, inventory mutations, or reporting calculations. Supplier purchase history is a read-only projection owned by the purchasing domain and exposed through the supplier API.

## 16.2 Required Folder Structure

```text
backend/src/main/java/com/smartstock/backend/supplier/
├── controller/
│   └── SupplierController.java
├── dto/
│   ├── SupplierCreateRequest.java
│   ├── SupplierUpdateRequest.java
│   ├── SupplierResponse.java
│   ├── SupplierSearchCriteria.java
│   ├── SupplierPurchaseHistoryResponse.java
│   └── SupplierPurchaseSummaryResponse.java
├── entity/
│   └── Supplier.java
├── mapper/
│   └── SupplierMapper.java
├── repository/
│   └── SupplierRepository.java
└── service/
    ├── SupplierService.java
    └── SupplierServiceImpl.java
```

The purchase-history read boundary belongs to a purchasing-domain port when implemented:

```text
supplier/port/SupplierPurchaseHistoryReader.java
```

EPIC-03 must not make supplier services depend on purchase entity implementation classes.

## 16.3 Entity Structure

### Supplier

```java
@Entity
@Table(
    name = "suppliers",
    indexes = {
        @Index(name = "idx_suppliers_name", columnList = "name"),
        @Index(name = "idx_suppliers_email", columnList = "email"),
        @Index(name = "idx_suppliers_phone", columnList = "phone"),
        @Index(name = "idx_suppliers_active", columnList = "is_active")
    }
)
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "contact_person", length = 180)
    private String contactPerson;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
```

Required fields and rules:

| Field | Database | Validation and behavior |
|---|---|---|
| `id` | `BIGINT`, identity | Read-only. |
| `name` | `VARCHAR(180)`, not null | Required, trim before validation/storage. Duplicate detection is practical by normalized name. |
| `contactPerson` | `VARCHAR(180)`, nullable | Optional. Trim where supplied. |
| `email` | `VARCHAR(255)`, nullable | Optional; valid RFC-style email syntax. Store trim/lowercase for matching. |
| `phone` | `VARCHAR(30)`, nullable | Optional; preserve display formatting. |
| `address` | `TEXT`, nullable | Optional. |
| `active` | `BOOLEAN`, not null | Starts `true`. Deactivation is soft; never hard delete. |
| `version` | `BIGINT`, not null | Optimistic-lock update protection. |
| `createdAt` | `TIMESTAMP`, not null | Server/database-managed. |
| `updatedAt` | `TIMESTAMP`, not null | Server/database-managed. |

Supplier entities must expose controlled methods such as `updateDetails(...)`, `deactivate()`, and `activate()` rather than unrestricted setters. Activation is not required by the backlog but is useful as the inverse deactivation operation if the repository UI needs the supplier to be restored. If not exposed, do not implement it initially.

### Database migration

Add a versioned migration (for example `V3__supplier_management.sql`) and do not use entity auto-DDL to create supplier tables:

```sql
CREATE TABLE suppliers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    contact_person VARCHAR(180),
    email VARCHAR(255),
    phone VARCHAR(30),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_suppliers_name ON suppliers (name);
CREATE INDEX idx_suppliers_email ON suppliers (email);
CREATE INDEX idx_suppliers_phone ON suppliers (phone);
CREATE INDEX idx_suppliers_active ON suppliers (is_active);
```

Required constraints and operational rules:

- `name` is mandatory.
- Database uniqueness is not guaranteed for names when differing only in whitespace/case. Store a separate normalized key for race-safe duplicate prevention.
- Recommended: add `normalized_name VARCHAR(180) NOT NULL` and a unique constraint on `normalized_name`.
- Email uniqueness is not required by the backlog. Do not impose a database unique constraint on email; duplicates by name and email may still be surfaced as a warning/business conflict.
- Add foreign-key supplier references to purchase orders in EPIC-04 using `supplier_id BIGINT NOT NULL REFERENCES suppliers(id)`. Supplier deletion must remain prohibited.
- Use `@Version` and the migration-defined optimistic lock strategy consistently with EPIC-02.

Example normalized-name approach:

```text
Supplier name: "  Acme  Supplies "
normalizedName: "acme supplies"
```

Normalize by trimming, collapsing internal whitespace, and applying lowercase matching. Keep the original display name and the normalized key in separate fields.

## 16.4 Repositories

### SupplierRepository

```java
public interface SupplierRepository
        extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findByNormalizedName(String normalizedName);

    boolean existsByNormalizedName(String normalizedName);

    boolean existsByNormalizedNameAndIdNot(String normalizedName, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Supplier> findByIdAndActiveTrue(Long id);
}
```

Search must use Spring Data specifications or explicit JPQL. Do not fetch suppliers and filter in Java.

Recommended derived/explicit queries:

```text
findByNormalizedName
existsByNormalizedName
existsByNormalizedNameAndIdNot
existsByEmailIgnoreCase
existsByEmailIgnoreCaseAndIdNot
findByIdAndActiveTrue
```

Search criteria:

```text
search       String: name, contactPerson, email, or phone
active       Boolean: default true
page         int: default 0
size         int: default 20, maximum 100
sort         String: default name
```

Duplicate strategy:

1. Normalize `name` and reject an exact duplicate normalized name as `409 SUPPLIER_NAME_ALREADY_EXISTS`.
2. Optionally normalize email and reject a duplicate email only as a business warning or conflict; the backlog says duplicate prevention is practical, but email can be shared by separate supplier accounts.
3. Enforce `normalizedName` unique at the database layer for concurrency safety.
4. Map `DataIntegrityViolationException` to a consistent duplicate conflict response.

No supplier repository may bypass soft deactivation to return inactive suppliers by default; include inactive suppliers only when `active=false` or `active` is omitted if the API contract defines all-status search explicitly.

## 16.5 DTO Contracts

Use immutable Java records. No request or response DTO may expose JPA entities or lazy relationship state.

### SupplierCreateRequest

```java
public record SupplierCreateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 180) String contactPerson,
    @Email @Size(max = 255) String email,
    @Size(max = 30) String phone,
    @Size(max = 10000) String address
) {
}
```

### SupplierUpdateRequest

```java
public record SupplierUpdateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 180) String contactPerson,
    @Email @Size(max = 255) String email,
    @Size(max = 30) String phone,
    @Size(max = 10000) String address
) {
}
```

Do not include `id`, `active`, `version`, `createdAt`, or `updatedAt` in update requests. Optimistic concurrency can use an `If-Match` header later, but EPIC-03 does not require it. Keep `@Version` for persistence protection.

### SupplierResponse

```java
public record SupplierResponse(
    Long id,
    String name,
    String contactPerson,
    String email,
    String phone,
    String address,
    boolean active,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
}
```

### SupplierSearchCriteria

```java
public record SupplierSearchCriteria(
    String search,
    Boolean active,
    int page,
    int size,
    String sort
) {
}
```

### SupplierPurchaseSummaryResponse

The purchase feature will supply one summary row through a projection port:

```java
public record SupplierPurchaseSummaryResponse(
    Long purchaseId,
    String purchaseNumber,
    Instant orderDate,
    String status,
    BigDecimal totalAmount,
    BigDecimal outstandingAmount,
    Instant dueDate
) {
}
```

`outstandingAmount` is 0 when EPIC-04 defines no unpaid balance, or must be calculated by EPIC-04 rather than by the supplier service. EPIC-03 must not calculate financial totals in Java from line items.

### SupplierPurchaseHistoryResponse

```java
public record SupplierPurchaseHistoryResponse(
    Long supplierId,
    String supplierName,
    boolean supplierActive,
    long totalOrders,
    BigDecimal totalPurchased,
    BigDecimal totalOutstanding,
    List<SupplierPurchaseSummaryResponse> orders
) {
}
```

An exact paginated representation is required for history rather than an unbounded list. Prefer:

```text
PageResponse<SupplierPurchaseSummaryResponse>
```

The supplier detail response should not embed purchase history. The history belongs to the dedicated endpoint below.

## 16.6 Mapper

Create `SupplierMapper` as a `@Component`. It may perform entity-to-response mapping and request-to-entity mapping, but it must not read repositories or calculate purchase totals.

## 16.7 Service Contract

```java
public interface SupplierService {

    SupplierResponse create(SupplierCreateRequest request);

    SupplierResponse getById(Long id);

    PageResponse<SupplierResponse> search(SupplierSearchCriteria criteria);

    SupplierResponse update(Long id, SupplierUpdateRequest request);

    void deactivate(Long id);

    PageResponse<SupplierPurchaseSummaryResponse> getPurchaseHistory(
        Long supplierId,
        LocalDate from,
        LocalDate to,
        String status,
        int page,
        int size,
        String sort
    );
}
```

Implementation rules:

- `@Transactional` on create, update, and deactivate.
- `@Transactional(readOnly = true)` on reads and search.
- Normalize name and email in the service before duplicate checks.
- Reject duplicate normalized name with `DuplicateResourceException`.
- Reject missing suppliers with `ResourceNotFoundException`.
- Deactivation preserves all supplier purchase records and must not delete the supplier.
- Reject deactivation if a future transaction policy requires it, but do not implement an arbitrary hard-delete restriction.
- Historical purchase records must remain visible for inactive suppliers.
- Purchase-history methods verify that the supplier exists first, including inactive suppliers, then delegate to `SupplierPurchaseHistoryReader`.
- `SupplierPurchaseHistoryReader` returns purchasing-domain projections; supplier code must not join supplier and purchase tables directly.

## 16.8 REST API Contract

### Supplier operations

```text
POST   /api/suppliers
GET    /api/suppliers/{id}
GET    /api/suppliers
PUT    /api/suppliers/{id}
PATCH  /api/suppliers/{id}/deactivate
```

Optional status restoration endpoint:

```text
PATCH /api/suppliers/{id}/activate
```

The activate endpoint is not required by the backlog and should only be added if the UI requires restoration. The mandatory implementation is deactivation.

### Purchase history

```text
GET /api/suppliers/{id}/purchase-history
```

Supported query parameters:

```text
from=2026-01-01
  Optional ISO-8601 LocalDate lower inclusive bound.

  to=2026-12-31
  Optional ISO-860-1 LocalDate upper inclusive bound.

  status=DRAFT|SUBMITTED|PARTIAL|RECEIVED|CANCELLED
  Optional purchase status. Accepted values must match EPIC-04 exactly.

  page=0
  size=20
  sort=orderDate,desc
```

The backlog defines `outstanding orders`, but EPIC-04 status names are the authoritative values. EPIC-03 must not invent enum values independently. Implement the status filter as a value-validated enum shared with the purchasing module or a purchasing projection contract.

### Search

```text
GET /api/suppliers?search=acme&active=true&page=0&size=20&sort=name
```

- `active=true` is the default.
- `active=false` returns inactive suppliers.
- To list both active and inactive suppliers, expose `active` as optional/omittable rather than silently using `false` as “all”; define a clear contract.
- Recommended default behavior: omitted `active=true`; explicit `active=false` means inactive only; support `active` with no value? No. Add `GET /api/suppliers?all=true` only if product requirements need mixed-status output. Otherwise keep omitted and explicit statuses unambiguous.

### HTTP semantics

| Operation | Success response |
|---|---:|
| Create | `201 Created` + `SupplierResponse` + `Location: /api/suppliers/{id}` |
| Read | `200 OK` + `SupplierResponse` |
| Search/list | `200 OK` + `PageResponse<SupplierResponse>` |
| Update | `200 OK` + `SupplierResponse` |
| Deactivate | `204 No Content` |
| Purchase history | `200 OK` + `PageResponse<SupplierPurchaseSummaryResponse>` |

Standard failures:

| Failure | Status/code |
|---|---|
| Invalid request or malformed date/status | `400 VALIDATION_FAILED` |
| Missing supplier | `404 RESOURCE_NOT_FOUND` |
| Duplicate normalized name/email conflict | `409 SUPPLIER_NAME_ALREADY_EXISTS` or `409 SUPPLIER_EMAIL_ALREADY_EXISTS` |
| Authentication missing | `401 AUTHENTICATION_REQUIRED` |
| Insufficient role | `403 ACCESS_DENIED` |
| Database constraint conflict | `409 DATABASE_CONSTRAINT_VIOLATION` |

Supplier create/update should use `SupplierCreateRequest`/`SupplierUpdateRequest` Bean Validation. Email should be nullable: `@Email` must allow null/blank when the field is optional, and the service should convert blank values to `null`.

## 16.9 Authorization Matrix

Use `@PreAuthorize` on controller or service methods:

| Operation | Roles |
|---|---|
| Supplier lookup/list/history | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |
| Supplier create/update | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |
| Supplier deactivate/activate | `ADMIN`, `MANAGER` |

Sales staff do not maintain or purchase from suppliers under the EPIC-03 and role matrix. Keep authorization server-side and do not return suppliers to sales staff unless a separate read requirement is approved.

## 16.10 Purchase History Dependency and Ordering

EPIC-03 can be implemented in two slices:

1. **Supplier master data slice**: entity, migration, repository, DTOs, mapper, service, controller, validation, authorization, and tests.
2. **Purchase history slice**: requires EPIC-04 purchase order entity and repository/projection to exist.

Do not create a `PurchaseOrder` placeholder solely for EPIC-03. Until EPIC-04 exists, implement the history endpoint behind a clearly named port with a zero-order adapter or return `501 Not Implemented`. `501` is preferable to fabricated purchase data and must not be treated as success. Do not permanently leave the endpoint enabled with empty data.

EPIC-04 must establish:

- Purchase order/supplier relationship: `ManyToOne Supplier` or a non-ORM supplier ID at the persistence boundary.
- Unique purchase order number.
- Stable purchase status enum.
- Persisted order date and total amount.
- A purchase projection/query that calculates or exposes `totalAmount`, status, and outstanding balance.
- Referential integrity to suppliers.
- Historical visibility for inactive suppliers.

The preferred read flow is:

```text
SupplierController
  -> SupplierService
  -> SupplierPurchaseHistoryReader (supplier port)
  -> purchasing implementation/repository projection
  -> PageResponse<SupplierPurchaseSummaryResponse>
```

## 16.11 Testing Requirements

### SupplierServiceTest

Cover:

- Successful supplier creation.
- Supplier name required and whitespace rejected.
- Valid optional email accepted.
- Blank email normalized to `null`.
- Invalid email rejected.
- Duplicate normalized name rejected.
- Duplicate email behavior according to the approved policy.
- Update successful.
- Update to duplicate name rejected.
- Missing supplier on read/update/deactivate rejected.
- Deactivation is preserved and not deleted.
- Active supplier search only.
- Inactive supplier remains visible by explicit status filter.
- Purchase history for inactive supplier remains accessible.

### SupplierControllerTest

Use `@WebMvcTest` to verify:

- `201`, `200`, `204`, `400`, `404`, `409`, `401`, and `403` behavior.
- Location header for creation.
- Request validation and nullable email handling.
- JSON fields; no JPA entities or passwords.
- Pagination and date/status query validation.
- Method-level authorization.

### SupplierRepositoryTest

Use MySQL-compatible integration tests to verify:

- Unique normalized name constraint.
- Case-insensitive/normalized duplicate detection.
- Pagination and database-side search.
- Active/inactive filtering.
- Optimistic lock protection.
- Soft deactivation.
- Supplier remains referenced by historical purchase records when implemented.

## 16.12 Definition of Ready for EPIC-03

Before coding EPIC-03:

- [ ] Supplier normalized-name and display-name strategy is approved.
- [ ] Duplicate email behavior is approved (warn or reject).
- [ ] Default active-status search behavior is approved.
- [ ] Activation endpoint is included or explicitly excluded.
- [ ] EPIC-04 purchase status names and outstanding balance definition are agreed.
- [ ] Purchase history endpoint pagination and sort fields are approved.
- [ ] Migration mechanism and supplier table constraints is approved.
- [ ] Authorization matrix is approved.
- [ ] Soft deactivation, not hard deletion, is confirmed.
- [ ] Contract tests are listed for supplier history and inactive suppliers.

---

# 17. Phase 2 Architecture — EPIC-04 Purchasing and Goods Receiving

## 17.1 Scope

EPIC-04 delivers:

1. Purchase order and purchase order item entities.
2. Goods receipt and goods receipt item entities.
3. Draft creation and editing, submission, cancellation, and goods receipt workflows.
4. Backend-calculated purchase totals.
5. Partial receipt quantity tracking and automatic order status calculation.
6. Atomic inventory updates and stock movement creation in the receiving transaction.
7. Paginated purchasing history filtered by supplier, status, and order date.
8. Cross-module supplier purchase-history integration.
9. Authorization and backend unit, web, repository, and transaction tests.

EPIC-04 must remain bounded by existing conventions:

- Feature-oriented layered architecture.
- Controllers never access repositories or contain business rules.
- DTOs are the only request/response models crossing the HTTP boundary.
- JPA entities are not returned directly.
- Write operations use `@Transactional` or an explicitly scoped transaction boundary.
- Read operations use `@Transactional(readOnly = true)`.
- The purchasing module owns purchase and receipt documents.
- The inventory module owns balances and stock movements.
- MySQL constraints and optimistic locking protect concurrent writes.

## 17.2 Required Package Structure

```text
backend/src/main/java/com/smartstock/backend/
├── purchasing/
│   ├── controller/
│   │   └── PurchaseController.java
│   ├── dto/
│   │   ├── PurchaseCreateRequest.java
│   │   ├── PurchaseUpdateRequest.java
│   │   ├── PurchaseSubmitRequest.java
│   │   ├── PurchaseCancelRequest.java
│   │   ├── PurchaseResponse.java
│   │   ├── PurchaseItemRequest.java
│   │   ├── PurchaseItemResponse.java
│   │   ├── GoodsReceiptCreateRequest.java
│   │   ├── GoodsReceiptItemRequest.java
│   │   ├── GoodsReceiptResponse.java
│   │   ├── GoodsReceiptItemResponse.java
│   │   ├── PurchaseSearchCriteria.java
│   │   └── PurchaseStatus.java
│   ├── entity/
│   │   ├── PurchaseOrder.java
│   │   ├── PurchaseOrderItem.java
│   │   ├── GoodsReceipt.java
│   │   └── GoodsReceiptItem.java
│   ├── mapper/
│   │   └── PurchaseMapper.java
│   ├── repository/
│   │   ├── PurchaseOrderRepository.java
│   │   ├── PurchaseOrderItemRepository.java
│   │   ├── GoodsReceiptRepository.java
│   │   └── GoodsReceiptItemRepository.java
│   ├── port/
│   │   ├── InventoryReceivingPort.java
│   │   └── InventoryReceivingResult.java
│   └── service/
│       ├── PurchaseService.java
│       └── PurchaseServiceImpl.java
└── inventory/
    └── port/adapter implementation for InventoryReceivingPort
```

The inventory implementation should not become a dependency on purchasing entities. It receives product IDs and quantities and returns only persisted inventory results or throws a transactional business exception.

## 17.3 Domain Model

### PurchaseOrder

Required fields:

```text
id
purchaseNumber
supplier            // ManyToOne Supplier, FK protected; inactive suppliers remain readable
orderDate
status              // DRAFT, SUBMITTED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
expectedDeliveryDate // optional
notes               // optional
items               // one-to-many PurchaseOrderItem
receipts            // one-to-many GoodsReceipt
subTotal            // BigDecimal, computed and persisted
totalAmount         // BigDecimal, computed and persisted
version
createdAt
updatedAt
```

Rules:

- `purchaseNumber` is required, normalized by the service, and unique.
- The order number can be generated by the backend or supplied if a future UI requires it; uniqueness is enforced by a database constraint.
- `orderDate` is the persisted business date. Use `Instant` or `LocalDate` consistently; the supplier projection currently exposes `Instant` and should not be changed without a contract decision.
- `subTotal` is the sum of `quantity × unitCost` for every ordered item.
- `totalAmount` initially equals `subTotal`. EPIC-04 does not invent taxes, discounts, freight, or payment terms. If those fields are later approved, persist and calculate them explicitly rather than deriving undocumented values.
- Totals are calculated by purchasing services and set in the entity; mapper classes calculate no totals.
- DRAFT orders may be updated or cancelled.
- SUBMITTED orders may be received or cancelled.
- CANCELLED and RECEIVED orders are terminal.
- PARTIALLY_RECEIVED may receive additional goods but cannot return to a non-receiving state or be cancelled.

### PurchaseOrderItem

Required fields:

```text
id
purchaseOrder
product
quantity
unitCost
lineTotal
receivedQuantity
version
createdAt
updatedAt
```

Rules:

- `quantity` is greater than zero.
- `unitCost` is zero or positive.
- `lineTotal = quantity × unitCost` using a project-wide decimal scale and `HALF_UP` rounding; recommended scale is `2`, unambiguous in the database and request/response contracts.
- `receivedQuantity` starts at zero and is never greater than `quantity`.
- Product references must resolve to an active product. Inactive products should not be added to a new order or updated into an existing draft.
- Unique `(purchaseOrderId, productId)` prevents duplicate lines for the same order.
- Quantity and unit cost must be immutable after SUBMITTED. Editing after submission is rejected.

### GoodsReceipt

Required fields:

```text
id
receiptNumber
purchaseOrder
receivedAt
receivedBy // User or stable user ID, entity decision follows existing auth conventions
notes
items
createdAt
updatedAt
```

Rules:

- `receiptNumber` is required and unique.
- A receipt belongs to exactly one submitted or partially received purchase order.
- A receipt is an append-only document. Do not expose update or delete endpoints.
- Receipt creation requires at least one receipt item.
- `receivedAt` is supplied or defaults to the server UTC time; it must not be blindly trusted for audit boundaries.
- `receivedBy` is derived from the authenticated principal where possible, not a caller-supplied identity.

### GoodsReceiptItem

Required fields:

```text
id
goodsReceipt
purchaseOrderItem
product
receivedQuantity
createdAt
```

Rules:

- `receivedQuantity` is greater than zero.
- The item must belong to an item in the receiving purchase order.
- It must not exceed `ordered quantity - already received quantity`.
- Unique receipt-processing keys prevent duplicate processing.

## 17.4 Idempotency and Duplicate Receipt Processing

A goods receipt request must accept an optional server-validated idempotency key through `Idempotency-Key`.

For cross-service-safe duplicate prevention:

```text
idempotencyKey // stored, unique
```

Rules:

- The key is unique per receipt request and must be a UUID or bounded opaque token (recommended 36–128 characters).
- Reusing the same key for an identical receipt returns the original `GoodsReceiptResponse`.
- Reusing a key for a different payload is rejected with `409 DUPLICATE_RESOURCE` or a purchasing-specific business conflict.
- If no key is supplied, a receipt is still protectable inside one database transaction, but clients should provide it for retries.
- Repeat processing must not increment stock or insert a second receipt.
- The database unique constraint on `idempotencyKey` is authoritative; optimistic/concurrency checks complement it.

If retry safety is configured without HTTP headers, `GoodsReceiptCreateRequest` may include `idempotencyKey`, but the preferred contract is the `Idempotency-Key` header. Do not accept conflicting key locations.

## 17.5 State Machine

The authoritative enum is:

```text
DRAFT
SUBMITTED
PARTIALLY_RECEIVED
RECEIVED
CANCELLED
```

Authoritative transitions:

```text
DRAFT → SUBMITTED
SUBMITTED → PARTIALLY_RECEIVED
SUBMITTED → RECEIVED
PARTIALLY_RECEIVED → RECEIVED
DRAFT → CANCELLED
SUBMITTED → CANCELLED
```

Transition rules:

- DRAFT to SUBMITTED requires at least one item and a valid active supplier.
- Submit does not reserve stock and does not change inventory balances.
- SUBMITTED with no receipts remains `SUBMITTED`.
- The first receipt that is less than the total outstanding ordered quantity changes `SUBMITTED → PARTIALLY_RECEIVED`.
- Further partial receipts remain `PARTIALLY_RECEIVED` until every line reaches `receivedQuantity == quantity`.
- The final receipt changes `PARTIALLY_RECEIVED` or `SUBMITTED` to `RECEIVED`.
- DRAFT/SUBMITTED to CANCELLED is allowed; PARTIALLY_RECEIVED and RECEIVED are terminal for cancellation.
- Direct `CANCELLED → ...` and all omitted transitions are rejected with `409 PURCHASE_STATUS_TRANSITION_NOT_ALLOWED`.
- Invalid transitions must not partially update receipts, received quantities, balances, or movements.

For supplier purchase-history projection, status values must be the exact EPIC-04 names. The existing supplier design text containing `PARTIAL` is corrected to `PARTIALLY_RECEIVED`.

## 17.6 Transactional Receiving Boundary with EPIC-05

Receiving is one database transaction. The authoritative implementation pattern is:

```text
PurchaseServiceImpl.receiveGoods(orderId, receiptRequest)
  @Transactional
  1. load purchase order and items with pessimistic or optimistic locking
  2. validate state and supplier/product/quantity rules
  3. insert GoodsReceipt + GoodsReceiptItem records
  4. call InventoryReceivingPort.applyPurchaseReceipt(...)
  5. update received quantities
  6. calculate and apply the new purchase status
  7. save the purchase aggregate
```

`InventoryReceivingPort` contract:

```java
public interface InventoryReceivingPort {

    InventoryReceivingResult applyPurchaseReceipt(
        Long purchaseOrderId,
        Long goodsReceiptId,
        List<PurchaseReceiptLine> lines
    );
}

public record PurchaseReceiptLine(
    Long productId,
    BigDecimal quantity,
    String reference
) {}

public record InventoryReceivingResult(
    Map<Long, BigDecimal> resultingOnHand
) {}
```

Inventory port requirements:

- Inventory owns product balance entities or balance rows and stock movements for EPIC-05.
- Purchasing creates no `StockMovement` entity directly.
- The receiving adapter creates one stock movement for each received product, outside a separate committed transaction.
- Movement reason/reference must identify purchase receipt and purchase order.
- The movement quantity is positive for goods received.
- The adapter uses the same Spring transaction because the purchase service owns `@Transactional` and calls the adapter synchronously.
- The adapter must lock inventory balances to prevent lost updates.
- Any inventory exception, receipt validation failure, or constraint violation rolls back the receipt, received quantities, status, balance, and movements together.
- On successful receipt, commit is atomic at the database transaction boundary.
- Draft and submitted orders never call the inventory port.
- Cancelling an order never calls the inventory port.

EPIC-05 may implement this via a service/component adapter. EPIC-05 must expose a read or adjustment API but must not bypass the purchasing-owned receiving transaction for stock mutation.

## 17.7 Repositories

```java
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>,
                JpaSpecificationExecutor<PurchaseOrder> {

    Optional<PurchaseOrder> findByPurchaseNumberIgnoreCase(String purchaseNumber);

    boolean existsByPurchaseNumberIgnoreCase(String purchaseNumber);

    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    Optional<PurchaseOrder> findWithDetailsById(Long id);
}

public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItem, Long> {
}

public interface GoodsReceiptRepository
        extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByReceiptNumberIgnoreCase(String receiptNumber);

    Optional<GoodsReceipt> findByIdempotencyKey(String idempotencyKey);
}

public interface GoodsReceiptItemRepository
        extends JpaRepository<GoodsReceiptItem, Long> {
}
```

Repository rules:

- Purchase history filtering belongs in `PurchaseOrderRepository` specifications or one explicit JPQL query.
- Supplier projection data must not expose JPA entities or reading supplier purchase history as a supplier-to-purchase join embedded in the supplier implementation.
- EPIC-04 implements `SupplierPurchaseHistoryReader` and replaces `UnimplementedSupplierPurchaseHistoryReader`.
- The implementation returns `PageResponse<SupplierPurchaseSummaryResponse>` and must verify supplier existence in `SupplierService` first.
- `outstandingAmount` is the purchasing balance remaining to be paid only if finance/payment fields exist. With EPIC-04 alone, do not fabricate an accounts-payable balance. The preferred initial definition is `totalAmount - receivedValueAmount`; however, the supplier summary DTO currently exposes `outstandingAmount`, so product must approve whether it means financial payable or outstanding received goods. EPIC-04 should not mislabel one as the other.

## 17.8 DTO Contracts

### PurchaseCreateRequest

```json
{
  "supplierId": 12,
  "orderDate": "2026-11-21",
  "expectedDeliveryDate": "2026-12-05",
  "notes": "Monthly restock",
  "items": [
    { "productId": 101, "quantity": 20, "unitCost": 12.50 }
  ]
}
```

Validation:

- `supplierId` required and active.
- `orderDate` required and not in the future? No explicit backlog rule prohibits future order dates. Do not invent this restriction.
- `items` required, non-empty.
- Product IDs required and active.
- Quantity greater than zero.
- Unit cost greater than or equal to zero.
- Decimal values must be finite and scale-safe under Bean Validation/custom validation.
- Notes maximum 2000 characters.

### PurchaseUpdateRequest

Same editable fields as create. Update is allowed only for DRAFT. No state is client-supplied. Supplier and line changes are treated atomically.

### PurchaseResponse

```json
{
  "id": 44,
  "purchaseNumber": "PO-2026-000044",
  "supplierId": 12,
  "supplierName": "Acme Trading",
  "orderDate": "2026-11-21",
  "expectedDeliveryDate": "2026-12-05",
  "status": "PARTIALLY_RECEIVED",
  "notes": "Monthly restock",
  "subTotal": 250.00,
  "totalAmount": 250.00,
  "items": [
    {
      "id": 501,
      "productId": 101,
      "productName": "Hammer",
      "quantity": 20,
      "receivedQuantity": 8,
      "unitCost": 12.50,
      "lineTotal": 250.00
    }
  ],
  "createdAt": "2026-11-21T18:00:00Z",
  "updatedAt": "2026-11-21T18:00:00Z"
}
```

### GoodsReceiptCreateRequest

```json
{
  "receivedAt": "2026-11-21T18:30:00Z",
  "notes": "Dock 2",
  "items": [
    { "purchaseOrderItemId": 501, "receivedQuantity": 8 }
  ]
}
```

Header `Idempotency-Key` is preferred. For the DTO fallback, if present it must match the header and is otherwise rejected.

### GoodsReceiptResponse

```json
{
  "id": 900,
  "receiptNumber": "GR-2026-000900",
  "purchaseOrderId": 44,
  "purchaseOrderNumber": "PO-2026-000044",
  "receivedAt": "2026-11-21T18:30:00Z",
  "receivedBy": "inventory-user",
  "statusAfterReceipt": "PARTIALLY_RECEIVED",
  "notes": "Dock 2",
  "items": [
    {
      "id": 1501,
      "purchaseOrderItemId": 501,
      "productId": 101,
      "productName": "Hammer",
      "receivedQuantity": 8
    }
  ]
}
```

DTO design rules:

- Java `record` types or existing project conventions may be used; do not expose entities.
- Use `BigDecimal` for prices and quantities where supported.
- Quantity precision and scale must be defined in migration and DTO validation.
- Purchase status in a response is an exact enum string.
- Use `PageResponse<T>` for all purchase history/search operations.

## 17.9 REST API Contract

```text
POST   /api/purchases
GET    /api/purchases
GET    /api/purchases/{id}
PUT    /api/purchases/{id}
PATCH  /api/purchases/{id}/submit
PATCH  /api/purchases/{id}/cancel
POST   /api/purchases/{id}/receipts
GET    /api/purchases/{id}/receipts
GET    /api/purchases/{id}/receipts/{receiptId}
```

No PUT/DELETE endpoint for goods receipts.

Query parameters:

```text
GET /api/purchases?supplierId=12&status=PARTIALLY_RECEIVED&from=2026-11-01&to=2026-11-30&page=0&size=20&sort=orderDate,desc
```

Rules:

- `supplierId` optional.
- `status` optional and must be one of the exact five values.
- `from` and `to` are inclusive ISO-8601 dates.
- Reject `from > to` with `400 VALIDATION_FAILED`.
- `page` and `size` use existing pagination defaults and upper bounds.
- Sort fields are allowlisted: `orderDate`, `purchaseNumber`, `status`, `totalAmount`, `updatedAt`.
- Unknown sort fields are rejected rather than interpreted by JPA.

HTTP semantics:

| Operation | Success |
|---|---|
| Create purchase | `201 Created` + `PurchaseResponse` + `Location: /api/purchases/{id}` |
| Read purchase | `200 OK` + `PurchaseResponse` |
| Search/list | `200 OK` + `PageResponse<PurchaseResponse>` |
| Update draft | `200 OK` + `PurchaseResponse` |
| Submit/cancel | `200 OK` + `PurchaseResponse` |
| Create receipt | `201 Created` + `GoodsReceiptResponse` + `Location: /api/purchases/{id}/receipts/{receiptId}` |
| Repeated identical receipt request | `200 OK` + original `GoodsReceiptResponse` |
| Read receipt/list | `200 OK` + `GoodsReceiptResponse` or page/list response |

Standard failures:

| Failure | Status/code |
|---|---|
| Missing supplier/product/order/item/receipt | `404 RESOURCE_NOT_FOUND` |
| Invalid quantity/cost/status/filter/date | `400 VALIDATION_FAILED` |
| Duplicate purchase number/receipt number/idempotency key | `409 DUPLICATE_RESOURCE` |
| Invalid state transition or cancellation/receipt rules | `409 BUSINESS_RULE_VIOLATION` with purchasing-specific message/code |
| Receipt exceeds remaining quantity | `409 PURCHASE_RECEIPT_QUANTITY_EXCEEDED` |
| Receiving inactive supplier/product or submitted-item mutation | `409 BUSINESS_RULE_VIOLATION` |
| Inventory mutation conflict or negative inventory | `409 INVENTORY_CONFLICT` |
| Authentication missing | `401 AUTHENTICATION_REQUIRED` |
| Insufficient role | `403 ACCESS_DENIED` |
| Database constraint conflict | `409 DATABASE_CONSTRAINT_VIOLATION` |

Add purchasing-specific error codes only through `ErrorCode`/`GlobalExceptionHandler` if they carry stable API semantics. Do not create controller-specific ad hoc JSON bodies.

## 17.10 Authorization Matrix

Use server-side `@PreAuthorize` on controller or service methods.

| Operation | Roles |
|---|---|
| Create/update/submit/cancel purchase | `ADMIN`, `MANAGER` |
| Read/list/search purchase and receipt history | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |
| Create goods receipt | `ADMIN`, `INVENTORY_STAFF` |
| Receive goods as inventory staff | `INVENTORY_STAFF` |
| Read supplier purchase history | `ADMIN`, `MANAGER`, `INVENTORY_STAFF` |

Notes:

- SALES_STAFF cannot create, submit, cancel, or receive purchases.
- The authenticated principal, not request input, determines `receivedBy`.
- Authorization failure is never returned as `404` to conceal resources; missing versus forbidden remains consistent with existing endpoint behavior.

## 17.11 Service Contract

```java
public interface PurchaseService {

    PurchaseResponse create(PurchaseCreateRequest request);

    PurchaseResponse getById(Long id);

    PageResponse<PurchaseResponse> search(PurchaseSearchCriteria criteria);

    PurchaseResponse update(Long id, PurchaseUpdateRequest request);

    PurchaseResponse submit(Long id);

    PurchaseResponse cancel(Long id);

    GoodsReceiptResponse receiveGoods(
        Long purchaseOrderId,
        GoodsReceiptCreateRequest request,
        String idempotencyKey
    );

    PageResponse<GoodsReceiptResponse> getReceipts(Long purchaseOrderId, int page, int size);

    GoodsReceiptResponse getReceiptById(Long purchaseOrderId, Long receiptId);
}

public interface SupplierPurchaseHistoryReader {
    PageResponse<SupplierPurchaseSummaryResponse> getHistory(
        Long supplierId,
        LocalDate from,
        LocalDate to,
        String status,
        int page,
        int size,
        String sort
    );
}
```

Implementation rules:

- `@Transactional` on create, update, submit, cancel, and receive.
- `@Transactional(readOnly = true)` on reads/search/history.
- Resolve suppliers and products in the service, never in controllers.
- Reject inactive suppliers/products unless a future approved rule explicitly allows historical documents to reference them.
- Recalculate line totals and order totals from persisted item quantities and costs; never trust DTO totals.
- Remove and re-create DRAFT lines during full update only; version/unique constraints protect concurrency.
- Generate purchase and receipt numbers in the service through a collision-safe mechanism.
- Lock purchase order and inventory balance records while receiving.
- Call inventory port once per receipt transaction, with one aggregate list of product quantities.
- Add the receiving inventory call in EPIC-04 only behind the EPIC-05 implementation; purchasing must compile against the port even if EPIC-05 is introduced afterward.

## 17.12 Purchase History and Outstanding Balance

EPIC-04 must replace `UnimplementedSupplierPurchaseHistoryReader` with a purchasing implementation.

The supplier history summary should expose:

- purchase ID
- purchase number
- order date
- status
- total amount
- outstanding amount
- due date

Define amounts before implementation:

- `totalAmount`: purchase order total.
- `outstandingAmount`: approved product meaning. EPIC-04 has no payment or accounts payable requirement, so the safest approved definition is not a financial balance unless explicit invoice/payment data exists.
- Recommended implementation if goods receipt is the only outstanding concept: calculate value outstanding by `Σ(ordered line lineTotal × remaining quantity / ordered quantity)` only when a unit-cost-based weighted quantity rule is adopted. This is line-level and may require rounding.
- Do not expose false payment status. If no purchase payment model exists, set `outstandingAmount = totalAmount` for statuses `SUBMITTED`, `PARTIALLY_RECEIVED`, and `RECEIVED`? This would falsely imply unpaid. Therefore, store or expose no financial payable meaning. The approved backlog wording is the source of truth: product must decide if `outstandingAmount` means payable, goods not received, or another domain value.

Implementation requirement: do not invent this field without an approved policy. Return a database-backed nullable/explicit contract only after EPIC-04/EPIC-05 and supplier requirements align.

## 17.13 Database Migration

Add a dedicated migration, for example `V4__purchasing_receiving_schema.sql` depending on the repository’s migration numbering.

Minimum constraints:

```text
purchase_orders.purchase_number UNIQUE
purchase_order_items UNIQUE(purchase_order_id, product_id)
goods_receipts.receipt_number UNIQUE
goods_receipts.idempotency_key UNIQUE
purchase_order_items.quantity > 0
purchase_order_items.unit_cost >= 0
purchase_order_items.received_quantity >= 0
purchase_order_items.received_quantity <= quantity
goods_receipt_items.received_quantity > 0
```

Indexes:

```text
purchase_orders(supplier_id, order_date)
purchase_orders(status, order_date)
purchase_orders(purchase_number)
purchase_order_items(purchase_order_id)
goods_receipts(purchase_order_id, received_at)
goods_receipts(idempotency_key)
goods_receipt_items(purchase_order_item_id)
```

Foreign-key behavior:

- Purchase order supplier FK is required for referential integrity.
- Product/order-item FKs are required.
- Deleting suppliers or products is not allowed; existing soft-deactivation patterns protect historical documents.
- Goods receipts and items are append-only; physical deletion is not exposed and migrations should not have application-level cascade delete.

## 17.14 Testing Requirements

### PurchaseServiceTest

Cover:

- Successful DRAFT creation with valid supplier/items.
- Unique purchase number generation and collision handling.
- Missing/duplicate/inactive supplier handling.
- Missing/duplicate/inactive product handling.
- Negative unit cost and zero/non-positive quantity rejection.
- Backend-calculated totals and line totals.
- No inventory mutation for DRAFT creation.
- Successful DRAFT update and `SUBMITTED` update rejection.
- Submit from DRAFT to SUBMITTED.
- Submit without items rejection.
- Invalid state transition rejection.
- DRAFT/SUBMITTED cancellation to CANCELLED.
- Cancel rejection after PARTIALLY_RECEIVED or RECEIVED.
- Partial receipt changes SUBMITTED to PARTIALLY_RECEIVED.
- Final receipt changes PARTIALLY_RECEIVED or SUBMITTED to RECEIVED.
- Receipt quantity cannot exceed remaining quantity.
- Mixed product receipt list updates only matching order items.
- Goods receipt and inventory mutation execute in one transaction.
- Receiving exception rolls back receipt, status, received quantities, balances, and movements.
- Duplicate idempotency key returns the original receipt without mutation.
- Conflicting reuse of an idempotency key is rejected.
- Supplier purchase history maps exact statuses and pagination.

### PurchaseControllerTest

Use `@WebMvcTest` with method-level authorization:

- `201`, `200`, `204`, `400`, `404`, `409`, `401`, and `403` behavior.
- Create/update/submit/cancel/receive Location headers and status codes.
- Request Bean Validation for quantities, costs, IDs, and date ranges.
- JSON fields contain no JPA entities or password/user internals.
- Status and sort query validation.
- `Idempotency-Key` validation and duplicate-key conflict behavior.
- Manager cannot receive goods if policy requires inventory staff only.
- Inventory staff can receive goods but cannot create/submitt/cancel purchase orders.
- ADMIN can perform purchasing operations.

### PurchaseRepositoryTest

Use MySQL-compatible integration tests:

- Unique purchase number and receipt/idempotency constraints.
- Unique purchase-order line constraints.
- Filtering by supplier, status, and date range.
- Pagination and allowlisted sorting.
- Loading purchase details without JPA entity leakage.
- Optimistic lock failure under concurrent update/receipt attempts.
- Pessimistic/concurrency locking for concurrent receipts.
- Receipt over-order quantity protection at database/application boundaries.
- Inactive supplier remains visible in purchase history.

### GoodsReceivingTransactionTest

Add a targeted integration test that covers the EPIC-04/EPIC-05 boundary:

- Successful receipt commits purchase, receipt, item, stock balance, and stock movement together.
- Inventory port failure rolls back all purchasing and inventory changes.
- Duplicate idempotency key does not create movement or change balances twice.
- Concurrent receipt requests cannot over-receive or produce two stock mutations.
- The receipt is immutable after persistence.

### Supplier History Contract Test

Verify:

- `SupplierService.getPurchaseHistory` delegates to the purchasing implementation after supplier existence is validated.
- Inactive suppliers remain available in history.
- Status filter accepts only EPIC-04 statuses.
- Pagination and sort behavior match `PageResponse` conventions.
- No direct supplier-layer JPA join or entity return occurs.

## 17.15 Definition of Ready for EPIC-04

Before coding EPIC-04:

- [ ] The purchase number format and collision strategy are approved.
- [ ] The goods receipt number format and collision strategy are approved.
- [ ] Decimal quantity/price scale and rounding policy are approved.
- [ ] `PARTIALLY_RECEIVED` is the only partial status used across all modules.
- [ ] `outstandingAmount` semantics are approved or the supplier projection field is explicitly revised/deferred.
- [ ] Partial receipt is append-only and goods receipts are never deleted/edited.
- [ ] Retry-key placement (`Idempotency-Key` header) is approved.
- [ ] The EPIC-05 inventory port contract and transaction propagation are approved.
- [ ] Authorization distinguishes purchase maintenance from goods receiving roles.
- [ ] A MySQL migration mechanism and migration numbering are approved.
- [ ] Concurrent receipt behavior uses optimistic/pessimistic locking without over-receipt.
- [ ] Contract tests include supplier purchase history and inactive suppliers.