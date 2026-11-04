-- SmartStock core schema
-- Target database: MySQL 8.0+
-- Spring Boot can execute this file during startup when:
-- spring.sql.init.mode=always

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_categories_parent_name (parent_id, name),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NULL,
    sku VARCHAR(80) NOT NULL UNIQUE,
    barcode VARCHAR(100) UNIQUE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    cost_price DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    selling_price DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    reorder_level DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    unit VARCHAR(30) NOT NULL DEFAULT 'unit',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_cost_price CHECK (cost_price >= 0),
    CONSTRAINT chk_products_selling_price CHECK (selling_price >= 0),
    CONSTRAINT chk_products_reorder_level CHECK (reorder_level >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    contact_person VARCHAR(150),
    email VARCHAR(180),
    phone VARCHAR(50),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_suppliers_name (name),
    INDEX idx_suppliers_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(50),
    address VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customers_name (name),
    INDEX idx_customers_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('DRAFT', 'SUBMITTED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')
        NOT NULL DEFAULT 'DRAFT',
    order_date DATE NOT NULL,
    expected_date DATE NULL,
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_orders_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchase_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_purchase_orders_amounts CHECK (
        subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0
    ),
    INDEX idx_purchase_orders_status (status),
    INDEX idx_purchase_orders_order_date (order_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity DECIMAL(19, 4) NOT NULL,
    received_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    unit_cost DECIMAL(19, 4) NOT NULL,
    line_total DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_purchase_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_purchase_order_items_quantities CHECK (
        ordered_quantity > 0 AND received_quantity >= 0
        AND received_quantity <= ordered_quantity
    ),
    CONSTRAINT chk_purchase_order_items_costs CHECK (
        unit_cost >= 0 AND line_total >= 0
    ),
    UNIQUE KEY uk_purchase_order_product (purchase_order_id, product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    received_by BIGINT NOT NULL,
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_goods_receipts_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_goods_receipts_received_by
        FOREIGN KEY (received_by) REFERENCES users (id),
    INDEX idx_goods_receipts_received_at (received_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goods_receipt_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_receipt_id BIGINT NOT NULL,
    purchase_order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    unit_cost DECIMAL(19, 4) NOT NULL,
    batch_number VARCHAR(100),
    expiry_date DATE NULL,
    CONSTRAINT fk_goods_receipt_items_receipt
        FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipts (id),
    CONSTRAINT fk_goods_receipt_items_order_item
        FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id),
    CONSTRAINT fk_goods_receipt_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_goods_receipt_items_values CHECK (
        quantity > 0 AND unit_cost >= 0
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory_balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL UNIQUE,
    quantity_on_hand DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    quantity_reserved DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_balances_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_inventory_balances_quantities CHECK (
        quantity_on_hand >= 0 AND quantity_reserved >= 0
        AND quantity_reserved <= quantity_on_hand
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NULL,
    cashier_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('COMPLETED', 'PARTIALLY_RETURNED', 'RETURNED', 'CANCELLED')
        NOT NULL DEFAULT 'COMPLETED',
    sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    discount_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    CONSTRAINT fk_sales_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_cashier
        FOREIGN KEY (cashier_id) REFERENCES users (id),
    CONSTRAINT chk_sales_amounts CHECK (
        subtotal >= 0 AND discount_amount >= 0
        AND tax_amount >= 0 AND total_amount >= 0
    ),
    INDEX idx_sales_sale_date (sale_date),
    INDEX idx_sales_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    cost_price DECIMAL(19, 4) NOT NULL,
    discount_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    line_total DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_sale_items_values CHECK (
        quantity > 0 AND unit_price >= 0 AND cost_price >= 0
        AND discount_amount >= 0 AND tax_amount >= 0 AND line_total >= 0
    ),
    INDEX idx_sale_items_product (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id BIGINT NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'BANK_TRANSFER', 'OTHER')
        NOT NULL DEFAULT 'CASH',
    amount DECIMAL(19, 4) NOT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_number VARCHAR(100),
    CONSTRAINT fk_payments_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    INDEX idx_payments_sale (sale_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales_returns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sale_id BIGINT NOT NULL,
    processed_by BIGINT NOT NULL,
    return_number VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    reason VARCHAR(255),
    refund_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    returned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_returns_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sales_returns_processed_by
        FOREIGN KEY (processed_by) REFERENCES users (id),
    CONSTRAINT chk_sales_returns_refund CHECK (refund_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales_return_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sales_return_id BIGINT NOT NULL,
    sale_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    refund_amount DECIMAL(19, 4) NOT NULL,
    CONSTRAINT fk_sales_return_items_return
        FOREIGN KEY (sales_return_id) REFERENCES sales_returns (id),
    CONSTRAINT fk_sales_return_items_sale_item
        FOREIGN KEY (sale_item_id) REFERENCES sale_items (id),
    CONSTRAINT fk_sales_return_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_sales_return_items_values CHECK (
        quantity > 0 AND refund_amount >= 0
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    performed_by BIGINT NOT NULL,
    movement_type ENUM(
        'PURCHASE_RECEIPT', 'SALE', 'SALE_RETURN',
        'ADJUSTMENT_IN', 'ADJUSTMENT_OUT'
    ) NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    quantity_before DECIMAL(19, 4) NOT NULL,
    quantity_after DECIMAL(19, 4) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_movements_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_performed_by
        FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_stock_movements_values CHECK (
        quantity > 0 AND quantity_before >= 0 AND quantity_after >= 0
    ),
    INDEX idx_stock_movements_product_date (product_id, created_at),
    INDEX idx_stock_movements_reference (reference_type, reference_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NULL,
    details JSON NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    INDEX idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    notification_type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_notifications_user_read (user_id, is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS business_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_name VARCHAR(180) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(180),
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    tax_rate DECIMAL(7, 4) NOT NULL DEFAULT 0.0000,
    invoice_prefix VARCHAR(20) NOT NULL DEFAULT 'INV',
    next_invoice_number BIGINT NOT NULL DEFAULT 1,
    reorder_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_business_settings_tax_rate CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT chk_business_settings_invoice_number CHECK (next_invoice_number > 0)
) ENGINE=InnoDB;

INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Full system administration access'),
    ('MANAGER', 'Business management and reporting access'),
    ('SALES_STAFF', 'Sales and customer transaction access'),
    ('INVENTORY_STAFF', 'Inventory and goods receiving access')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO business_settings (business_name)
SELECT 'SmartStock Business'
WHERE NOT EXISTS (SELECT 1 FROM business_settings);
