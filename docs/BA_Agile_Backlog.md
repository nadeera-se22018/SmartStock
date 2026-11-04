Read `SKILL.md`
Completed with input: {
  "path": "D:\\program\\SmartStock\\skills\\bmad-agent-analyst\\SKILL.md"
}

📊 Mary here. I’ll activate the Business Analyst configuration, then translate the proposal into a prioritized, solo-developer-ready backlog with acceptance criteria and implementation tasks.

Read 
Completed with input: {
  "path": "D:\\program\\SmartStock\\skills\\bmad-agent-analyst\\customize.toml"
}

Find files matching `_bmad/**/*`

Find files matching `docs/**/*`

Read 
Completed with input: {
  "path": "D:\\program\\SmartStock\\docs\\plan\\break-work-into-stories-and-track-it.md"
}

Read 
Completed with input: {
  "path": "D:\\program\\SmartStock\\docs\\plan\\define-requirements-and-a-specification.md"
}

Read , lines 118 to 395
Completed with input: {
  "path": "D:\\program\\SmartStock\\docs\\SDLC_Plan.md",
  "view_range": [
    118,
    395
  ]
}

📊 **Mary — Business Analyst**

The repository does not currently contain a `_bmad/` customization tree, so this breakdown uses the base Business Analyst rules from `SKILL.md`, the proposal, and the existing `SDLC plan`.

# 1. Product Scope

## MVP goal

Deliver a single-business inventory and sales system supporting:

- One inventory location
- Product and category management
- Supplier and purchasing workflows
- Inventory tracking
- Sales and invoices
- Basic returns
- Customer records
- Dashboard and reports
- Audit logging
- Role-based authorization

## Deferred features

Implement only after the MVP is stable:

- Multi-branch inventory
- Forecasting
- AI insights
- Email notifications
- Loyalty points
- Online payments
- Batch/FIFO inventory valuation
- Camera barcode scanning

# 2. Epic Breakdown

| Epic | Module | Priority | Dependencies |
|---|---|---:|---|
| EPIC-01 | User and Role Administration | Must | JWT authentication |
| EPIC-02 | Product and Category Management | Must | Database |
| EPIC-03 | Supplier Management | Must | Database |
| EPIC-04 | Purchasing and Goods Receiving | Must | Products, suppliers |
| EPIC-05 | Inventory Management | Must | Products, receiving |
| EPIC-06 | Customer Management | Should | Sales |
| EPIC-07 | Sales and Invoicing | Must | Products, inventory, customers |
| EPIC-08 | Returns and Refunds | Should | Sales |
| EPIC-09 | Dashboard and Reporting | Must | Sales, inventory, purchasing |
| EPIC-10 | Audit Logs and Notifications | Should | All transaction modules |
| EPIC-11 | Quality, Deployment, and Documentation | Must | All MVP modules |

# 3. EPIC-01 — User and Role Administration

Authentication and JWT infrastructure is complete. The remaining work is account administration and authorization enforcement.

## US-01.1 — View users

**As an administrator**, I want to view all users so that I can manage system access.

### Acceptance criteria

- Users can be paginated.
- Results show username, email, name, roles, status, and creation date.
- Password hashes are never returned.
- Administrators can filter by active/inactive status.
- Non-administrators cannot access the endpoint.

### Tasks

1. Create `GET /api/users`.
2. Add pagination and status filtering.
3. Create user response DTO.
4. Add `@PreAuthorize("hasRole('ADMIN')")`.
5. Add repository query.
6. Add controller and service tests.

## US-01.2 — Create user

**As an administrator**, I want to create a user account so that staff can access the system.

### Acceptance criteria

- Username and email must be unique.
- Password is stored using BCrypt.
- At least one role is required.
- New accounts are active by default.
- Duplicate username/email returns a clear validation error.

### Tasks

1. Create `POST /api/users`.
2. Add request DTO validation.
3. Hash the password.
4. Resolve roles through `RoleRepository`.
5. Save the user and role assignments.
6. Record an audit event.

## US-01.3 — Activate or deactivate user

**As an administrator**, I want to disable an account without deleting it so that historical transactions remain valid.

### Acceptance criteria

- Deactivated users cannot authenticate.
- Existing user records remain in the database.
- The action is audit logged.
- An administrator cannot accidentally deactivate the final active administrator.

## US-01.4 — Enforce role permissions

**As the system**, I must enforce authorization on the backend so that frontend restrictions cannot be bypassed.

### Required access rules

- `ADMIN`: users, roles, settings, audit logs
- `MANAGER`: products, suppliers, purchases, inventory, reports
- `SALES_STAFF`: product lookup, customers, sales, permitted returns
- `INVENTORY_STAFF`: products, suppliers, receiving, inventory

# 4. EPIC-02 — Product and Category Management

## US-02.1 — Create product

**As a manager**, I want to create a product so that it can be purchased and sold.

### Acceptance criteria

- SKU is mandatory and unique.
- Barcode is optional but unique when supplied.
- Cost price, selling price, and reorder level cannot be negative.
- Category is optional or must reference an existing category.
- Product starts as active.
- Duplicate SKU/barcode returns a business validation error.

### Tasks

1. Create `Product` and `Category` entities.
2. Create repositories.
3. Create request and response DTOs.
4. Implement `POST /api/products`.
5. Add Bean Validation.
6. Add duplicate identifier handling.
7. Add unit and integration tests.

## US-02.2 — Search and list products

**As sales or inventory staff**, I want to search products by name, SKU, barcode, or category.

### Acceptance criteria

- Search is case-insensitive.
- Results are paginated.
- Inactive products are excluded by default.
- Barcode lookup returns the matching product.
- Search results include current available quantity.

### Endpoint

```text
GET /api/products?search=&categoryId=&active=true&page=0&size=20
GET /api/products/barcode/{barcode}
```

## US-02.3 — Update and deactivate product

**As a manager**, I want to update product information or deactivate obsolete products.

### Business rules

- Product identifiers cannot be changed to duplicates.
- Deactivation does not delete purchase or sales history.
- A product with historical transactions must not be hard deleted.
- Price changes affect future transactions only.

## US-02.4 — Manage categories

**As a manager**, I want to organize products into categories and subcategories.

### Acceptance criteria

- Category names are unique within the same parent category.
- A category with active products cannot be deleted.
- Categories may be deactivated instead.
- Circular parent-child relationships are rejected.

# 5. EPIC-03 — Supplier Management

## US-03.1 — Create and update supplier

**As a manager**, I want to maintain supplier details so that purchasing records are accurate.

### Data

- Supplier name
- Contact person
- Email
- Phone
- Address
- Active status

### Acceptance criteria

- Supplier name is required.
- Email format is validated when supplied.
- Deactivation preserves historical purchase records.
- Duplicate supplier records are prevented where practical.

## US-03.2 — View supplier purchase history

**As a manager**, I want to view a supplier’s purchase history.

### Acceptance criteria

- Results can be filtered by date and status.
- Purchase order totals are displayed.
- Outstanding orders are identifiable.
- Historical records remain visible for inactive suppliers.

# 6. EPIC-04 — Purchasing and Goods Receiving

## US-04.1 — Create purchase order

**As a manager**, I want to create a purchase order for products required from a supplier.

### Acceptance criteria

- Purchase order has a unique order number.
- Products and quantities must be valid.
- Unit costs cannot be negative.
- Totals are calculated by the backend.
- New orders start in `DRAFT`.
- Draft orders do not change inventory.

### Endpoint

```text
POST /api/purchases
GET /api/purchases
GET /api/purchases/{id}
PUT /api/purchases/{id}
```

## US-04.2 — Submit or cancel purchase order

**As a manager**, I want to submit or cancel an order.

### State rules

```text
DRAFT → SUBMITTED
SUBMITTED → PARTIALLY_RECEIVED
SUBMITTED → RECEIVED
PARTIALLY_RECEIVED → RECEIVED
DRAFT/SUBMITTED → CANCELLED
```

Invalid state transitions must be rejected.

## US-04.3 — Receive goods

**As inventory staff**, I want to record received goods so that stock increases only when goods physically arrive.

### Acceptance criteria

- Partial receipts are supported.
- Received quantity cannot exceed ordered quantity.
- Inventory update and receipt creation occur in one database transaction.
- A stock movement is created for each received product.
- Purchase order status is updated automatically.
- Duplicate receipt processing is prevented.

## US-04.4 — View purchasing history

**As a manager**, I want to view purchase orders by supplier, status, and date.

# 7. EPIC-05 — Inventory Management

## US-05.1 — View inventory balances

**As inventory staff**, I want to see current stock levels.

### Acceptance criteria

- Product quantity on hand is displayed.
- Reserved quantity is displayed if applicable.
- Available quantity is calculated consistently.
- Products can be filtered by low-stock and out-of-stock status.
- Results are paginated.

## US-05.2 — Record stock adjustment

**As authorized inventory staff**, I want to adjust stock when physical counts differ from system records.

### Acceptance criteria

- Adjustment requires a reason.
- Positive and negative adjustments are supported.
- Stock cannot become negative.
- Adjustment creates a stock movement.
- Adjustment is audit logged.
- Unauthorized users cannot perform adjustments.

## US-05.3 — Maintain inventory ledger

**As a manager**, I want to view all stock movements for traceability.

### Movement types

- `PURCHASE_RECEIPT`
- `SALE`
- `SALE_RETURN`
- `ADJUSTMENT_IN`
- `ADJUSTMENT_OUT`

### Acceptance criteria

- Each movement contains before and after quantities.
- Each movement references the responsible user.
- Transaction reference information is stored.
- Movement history cannot be silently deleted.

## US-05.4 — Generate low-stock alerts

**As a manager**, I want to identify products at or below their reorder levels.

### Rule

```text
low stock when quantity_on_hand <= reorder_level
out of stock when quantity_on_hand = 0
```

# 8. EPIC-06 — Customer Management

## US-06.1 — Create customer

**As sales staff**, I want to create a customer record so that sales can be associated with a customer.

### Acceptance criteria

- Customer name is required.
- Email and phone are optional but validated.
- Duplicate customers are identified using available contact information.
- Customers can be deactivated without deleting sales history.

## US-06.2 — Search customer history

**As sales staff**, I want to search customers and view their previous purchases.

### Acceptance criteria

- Search by name or phone.
- Customer history is paginated.
- Sales totals and invoice numbers are displayed.
- Customer data is protected from unauthorized access.

# 9. EPIC-07 — Sales and Invoicing

## US-07.1 — Create sale

**As sales staff**, I want to create a sale using a point-of-sale interface.

### Acceptance criteria

- Products can be searched by name, SKU, or barcode.
- Products can be added to a cart.
- Quantity must be greater than zero.
- Sale cannot exceed available stock.
- Subtotal, discount, tax, and total are calculated server-side.
- Sale, sale items, payment, stock deduction, and stock movement commit atomically.
- Invoice number is unique.

### Endpoint

```text
POST /api/sales
GET /api/sales
GET /api/sales/{id}
```

## US-07.2 — Record payment

**As sales staff**, I want to record how the customer paid.

### Supported methods

- Cash
- Card
- Bank transfer
- Other

### Acceptance criteria

- Payment amount must be positive.
- Payment cannot exceed the amount due unless overpayment is explicitly supported.
- Payment is associated with the sale.
- Payment method is recorded.

## US-07.3 — Generate invoice

**As sales staff**, I want to print or download an invoice.

### Acceptance criteria

- Invoice includes business information.
- Invoice includes invoice number and date.
- Invoice includes products, quantities, prices, discounts, tax, and total.
- Invoice is generated from persisted sale data.
- Invoice cannot be edited after completion.

## US-07.4 — Prevent duplicate sales

**As the system**, I must prevent duplicate submissions.

### Implementation tasks

1. Accept an idempotency key from the client.
2. Store or validate the request key.
3. Reject repeated completed requests.
4. Add concurrent-sale integration tests.
5. Use row locking or appropriate transaction isolation when reducing stock.

# 10. EPIC-08 — Returns and Refunds

## US-08.1 — Process full or partial return

**As authorized sales staff**, I want to return products from a completed sale.

### Acceptance criteria

- Original invoice must exist.
- Returned quantity cannot exceed sold quantity minus previous returns.
- Return requires a reason.
- Refund amount is calculated from sale data.
- Returned stock is restored when appropriate.
- Sale status changes to `PARTIALLY_RETURNED` or `RETURNED`.
- Return and stock restoration are atomic.

## US-08.2 — Record refund

**As sales staff**, I want to record the refund associated with a return.

### Business rules

- Refund cannot exceed the refundable amount.
- Refund method must be recorded.
- Return actions must be audit logged.

# 11. EPIC-09 — Dashboard and Reporting

## US-09.1 — View dashboard KPIs

**As a business owner**, I want to view key business statistics.

### KPIs

- Total sales
- Number of completed sales
- Gross profit estimate
- Inventory valuation
- Low-stock count
- Out-of-stock count
- Outstanding purchase orders
- Recent transactions

### Endpoint

```text
GET /api/dashboard?from=&to=
```

## US-09.2 — View sales trends

**As a manager**, I want to see sales trends over time.

### Acceptance criteria

- Date range is configurable.
- Results can be grouped by day, week, or month.
- Empty periods return zero values rather than missing data.
- Time-zone handling is consistent.

## US-09.3 — Generate reports

**As a manager**, I want to generate operational reports.

### MVP reports

- Daily/weekly/monthly sales
- Product-wise sales
- Inventory stock
- Low-stock products
- Purchase history
- Supplier transactions
- Sales returns
- Gross profit summary
- Stock movement

## US-09.4 — Export reports

**As an authorized user**, I want to export reports.

### Acceptance criteria

- CSV export is implemented first.
- PDF export follows for the highest-priority reports.
- Export respects filters and date ranges.
- Large reports are paginated or streamed safely.

# 12. EPIC-10 — Audit Logs and Notifications

## US-10.1 — Record audit events

**As an administrator**, I want to inspect important system actions.

### Actions to record

- Login success/failure
- User creation and deactivation
- Role changes
- Product changes
- Stock adjustments
- Purchase status changes
- Goods receiving
- Sales
- Returns
- Settings changes

### Audit fields

- User
- Action
- Entity type
- Entity ID
- Timestamp
- IP address
- Structured details

## US-10.2 — View audit logs

**As an administrator**, I want to filter audit logs by user, action, entity, and date.

## US-10.3 — In-app low-stock notifications

**As a manager**, I want to receive low-stock notifications.

### Acceptance criteria

- Notifications are created when a product crosses its reorder threshold.
- Duplicate unread notifications are avoided.
- Users can mark notifications as read.
- Email notifications remain out of MVP scope.

# 13. EPIC-11 — Quality, Deployment, and Documentation

## US-11.1 — API documentation

**As a developer**, I want documented REST endpoints.

### Tasks

- Add OpenAPI dependency.
- Document authentication.
- Document request and response DTOs.
- Document validation errors.
- Document role requirements.
- Verify Swagger UI manually.

## US-11.2 — Automated testing

### Required backend tests

- Repository tests
- Service unit tests
- Controller tests
- Authorization tests
- Transaction workflow tests
- Overselling tests
- Return quantity tests
- Report calculation tests

## US-11.3 — Deployment readiness

### Tasks

- Externalize database credentials.
- Externalize JWT secret.
- Add production profile.
- Add Docker configuration.
- Add database backup instructions.
- Add startup and setup documentation.
- Verify clean installation in a separate environment.

# 14. Recommended Build Sequence

## Phase 1 — Product foundation

1. Product entity and repository
2. Category entity and repository
3. Product CRUD service
4. Category CRUD service
5. Validation and duplicate handling
6. Product search and pagination
7. Product authorization
8. Backend tests

## Phase 2 — Suppliers and purchasing

1. Supplier entity and repository
2. Supplier CRUD
3. Purchase order DTOs
4. Purchase order creation
5. Purchase order status transitions
6. Goods receipt workflow
7. Purchase totals
8. Purchasing tests

## Phase 3 — Inventory

1. Inventory balance repository
2. Stock movement service
3. Receiving stock updates
4. Stock adjustment workflow
5. Low-stock queries
6. Inventory transaction locking
7. Inventory audit logging
8. Inventory tests

## Phase 4 — Sales

1. Customer management
2. Product lookup for POS
3. Cart request model
4. Backend price and total calculation
5. Stock availability validation
6. Sale transaction
7. Payment recording
8. Invoice generation
9. Overselling and concurrency tests

## Phase 5 — Returns and reporting

1. Return request validation
2. Full and partial returns
3. Refund records
4. Stock restoration
5. Sales reports
6. Inventory reports
7. Dashboard KPI queries
8. CSV export
9. PDF export

## Phase 6 — Hardening and delivery

1. Audit log completion
2. Notification workflow
3. Swagger documentation
4. Security testing
5. End-to-end workflow testing
6. Performance improvements
7. Docker deployment
8. Backup and restore verification
9. User documentation
10. Final demonstration preparation

# 15. End-to-End Definition of Done

A story is complete only when:

- Backend API is implemented.
- Authorization is enforced server-side.
- Request validation is present.
- Database transaction behavior is correct.
- Error responses are consistent.
- Unit or integration tests pass.
- Swagger documentation is updated.
- Relevant audit logging exists.
- No historical transaction is hard deleted.
- The workflow is verified through Postman or an automated integration test.

# 16. Critical End-to-End Acceptance Scenario

The MVP must pass this complete workflow:

1. Administrator creates a manager.
2. Manager creates a category.
3. Manager creates a product.
4. Manager creates a supplier.
5. Manager creates a purchase order.
6. Inventory staff receives goods.
7. System increases inventory.
8. Sales staff creates a customer.
9. Sales staff sells the product.
10. System validates stock and deducts inventory.
11. System generates a unique invoice.
12. Manager views updated dashboard totals.
13. Sales staff processes a partial return.
14. System restores returned stock.
15. Administrator views the complete audit history.

This sequence should be the primary integration test for SmartStock.