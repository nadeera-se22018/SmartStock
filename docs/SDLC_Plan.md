# SDLC Breakdown for SmartStock

The proposal describes an iterative Agile project using **React.js, Spring Boot, and MySQL**. For a solo developer, the scope should be prioritized around a reliable core system first, with advanced features treated as optional extensions.

## 1. Planning and Feasibility

**Goal:** Confirm what can realistically be delivered by one developer.

### Main activities
- Define the minimum viable product (MVP).
- Identify users:
  - Administrator
  - Business owner/manager
  - Sales staff
  - Inventory staff
- Prioritize core workflows:
  1. Login and role-based access
  2. Product and category management
  3. Supplier management
  4. Purchasing and goods receiving
  5. Inventory tracking
  6. Sales and invoicing
  7. Dashboard and reports
- Defer complex features such as:
  - Multi-branch support
  - Sales forecasting
  - AI insights
  - Loyalty points
  - Email notifications
- Select development tools, Git workflow, and deployment approach.

### Deliverables
- Project scope and MVP definition
- User roles and use cases
- Initial product backlog
- Risk assessment
- Eight-week development plan

---

## 2. Requirements Analysis

**Goal:** Convert the proposal into clear, testable requirements.

### Functional requirements
Document requirements for:

- Authentication and authorization
- Products and categories
- Suppliers
- Purchase orders
- Goods receiving
- Stock movements
- Sales and returns
- Customers
- Invoices and receipts
- Reports
- Dashboard
- Notifications
- Audit logs
- Business settings

### Non-functional requirements
Define requirements for:

- Security
- Performance
- Usability
- Reliability
- Data integrity
- Maintainability
- Mobile responsiveness
- Backup and recovery

### Solo-developer recommendation
Write user stories such as:

> As a sales staff member, I want to search for a product by barcode so that I can add it quickly to a sale.

Each story should include acceptance criteria and priority.

### Deliverables
- Software Requirements Specification
- Use-case diagrams
- User stories
- Acceptance criteria
- Prioritized backlog

---

## 3. System and Database Design

**Goal:** Design the architecture, database, APIs, and user interface before implementation.

### Application architecture

```text
React Frontend
      ↓
Spring Boot REST API
      ↓
Service Layer
      ↓
JPA Repository Layer
      ↓
MySQL Database
```

The backend must remain responsible for:

- Stock calculations
- Sales totals
- Discounts and taxes
- Authorization
- Inventory transactions
- Overselling prevention

### Database design

Core tables should include:

- users
- roles
- permissions
- products
- categories
- suppliers
- customers
- purchase_orders
- purchase_order_items
- goods_receipts
- sales
- sale_items
- payments
- inventory_balances
- stock_movements
- audit_logs
- business_settings

Start with a single business and one inventory location. Avoid implementing branches or warehouses in the MVP unless required.

### API design

Define endpoints for:

- `/api/auth`
- `/api/products`
- `/api/categories`
- `/api/suppliers`
- `/api/purchases`
- `/api/inventory`
- `/api/sales`
- `/api/customers`
- `/api/reports`
- `/api/dashboard`
- `/api/audit-logs`

Use OpenAPI/Swagger documentation while developing the endpoints.

### Frontend design

Prepare wireframes for:

- Login
- Dashboard
- Product management
- Supplier management
- Purchase order creation
- Inventory view
- Point-of-sale screen
- Reports
- User and settings screens

### Deliverables
- Architecture diagram
- Entity relationship diagram
- Database schema
- API specification
- Wireframes
- UI component plan

---

## 4. Implementation

**Goal:** Build the system incrementally in vertical slices.

### Backend implementation

Use:

- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- Bean Validation
- MySQL
- Maven

Implement in this order:

1. Project configuration and database connection
2. Common response and error-handling structure
3. User authentication and JWT security
4. Roles and permissions
5. Products and categories
6. Suppliers
7. Purchasing and goods receiving
8. Inventory ledger and stock movements
9. Sales and invoice generation
10. Customers and returns
11. Reports and dashboard APIs
12. Audit logs and notifications

### Frontend implementation

Use:

- React with TypeScript if possible
- React Router
- Axios
- Bootstrap or Material UI
- Recharts
- React Testing Library

Implement:

1. Application layout and navigation
2. Login and protected routes
3. Reusable forms, tables, modals, and validation
4. Product and category screens
5. Supplier and purchasing screens
6. Inventory screens
7. Point-of-sale screen
8. Dashboard and reports
9. Responsive mobile layouts
10. Loading, empty, and error states

### Important implementation rule

For sales and purchasing, use database transactions so that:

- A sale cannot complete if stock is insufficient.
- Stock and sale records update together.
- Failed transactions roll back completely.
- Duplicate submissions are prevented where possible.

---

## 5. Testing

**Goal:** Verify that the application is correct, secure, and usable.

### Backend testing

Use JUnit and Mockito to test:

- Authentication
- Authorization rules
- Product validation
- Purchase totals
- Goods receiving
- Stock updates
- Sales calculations
- Discounts and taxes
- Returns and refunds
- Overselling prevention
- Report calculations

### Frontend testing

Use React Testing Library to test:

- Login behavior
- Protected routes
- Form validation
- Product search
- Cart calculations
- Sales submission
- Error messages
- Dashboard rendering

### Integration testing

Verify complete workflows:

1. Create a product.
2. Create a purchase order.
3. Receive goods.
4. Confirm stock increased.
5. Process a sale.
6. Confirm stock decreased.
7. Generate an invoice.
8. Process a return.
9. Confirm stock was restored.

### Other testing

- API testing with Postman
- Security testing for unauthorized endpoints
- Responsive browser testing
- Performance testing for search and reports
- Backup and restoration testing

---

## 6. Deployment

**Goal:** Make the application runnable outside the development environment.

### Activities

- Create Docker configuration for:
  - React frontend
  - Spring Boot backend
  - MySQL database
- Configure environment variables for:
  - Database credentials
  - JWT secret
  - API URL
  - Application settings
- Create database initialization or migration scripts.
- Configure production logging.
- Deploy to a suitable cloud platform or local server.
- Configure HTTPS if publicly hosted.
- Document database backup and restoration.

### Deliverables

- Deployed application
- Docker Compose configuration
- Production configuration guide
- Database setup script
- Backup and restore instructions
- Deployment documentation

---

## 7. Maintenance and Enhancement

**Goal:** Support the system after the initial release.

### Activities

- Fix production defects.
- Monitor logs and database performance.
- Apply security updates.
- Improve slow queries and indexes.
- Refine usability based on feedback.
- Add future features incrementally.

### Possible post-MVP enhancements

- Multi-branch inventory
- Barcode camera scanning
- Batch and expiry tracking
- Reorder recommendations
- Sales forecasting
- Email notifications
- AI-powered insights
- Online payment integration

---

# Suggested Eight-Week Solo-Developer Schedule

| Week | SDLC Focus | Main Deliverables |
|---|---|---|
| 1 | Planning, requirements, design | Scope, user stories, wireframes, architecture, database draft |
| 2 | Project foundation | Spring Boot setup, React setup, MySQL, Git, base API structure |
| 3 | Authentication and products | JWT, roles, protected routes, product/category CRUD |
| 4 | Suppliers, purchasing, inventory | Suppliers, purchase orders, goods receiving, stock ledger |
| 5 | Sales and invoicing | POS screen, cart, sales transactions, stock deduction, invoices |
| 6 | Dashboard and reports | KPIs, charts, sales reports, inventory reports, audit logs |
| 7 | Integration and testing | End-to-end workflows, security testing, bug fixing, optimization |
| 8 | Deployment and documentation | Docker deployment, backup procedure, user guide, final demonstration |

## Recommended MVP Boundary

For a solo developer, the release should guarantee:

- Secure login and role-based access
- Product and supplier management
- Purchasing and goods receiving
- Accurate inventory updates
- Sales and invoice generation
- Basic returns
- Dashboard KPIs
- Core reports
- Audit logging
- Automated tests
- Deployment documentation

Advanced analytics, forecasting, AI, multi-branch support, and email notifications should only be implemented after the core workflows are stable.