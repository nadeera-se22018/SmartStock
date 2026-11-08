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