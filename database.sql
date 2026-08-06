-- PIMS (HealthFirst) - schema + seed data
-- Load with: mysql -u root -p < database.sql

CREATE DATABASE IF NOT EXISTS pims_db;
USE pims_db;

CREATE TABLE users (
    user_id     INT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('Admin', 'Cashier') NOT NULL,
    full_name   VARCHAR(100)
);

CREATE TABLE suppliers (
    supplier_id     INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         TEXT
);

CREATE TABLE medicines (
    medicine_id         INT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(150) NOT NULL,
    company             VARCHAR(100),
    medicine_type       VARCHAR(50),
    price               DECIMAL(10,2) NOT NULL,
    quantity_in_stock   INT DEFAULT 0,
    reorder_level       INT DEFAULT 0,
    expiry_date         DATE,
    supplier_id         INT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

CREATE TABLE sales (
    sale_id       INT PRIMARY KEY AUTO_INCREMENT,
    sale_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount  DECIMAL(10,2),
    user_id       INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE sale_items (
    sale_item_id    INT PRIMARY KEY AUTO_INCREMENT,
    sale_id         INT,
    medicine_id     INT,
    quantity_sold   INT,
    price_at_sale   DECIMAL(10,2),
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

-- Seed data

INSERT INTO users (username, password, role, full_name) VALUES
    ('admin', 'admin123', 'Admin', 'HealthFirst Administrator'),
    ('cashier', 'cash123', 'Cashier', 'Front Counter Cashier');

INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
    ('MedSupply Co.', 'Jane Nkosi', '011-555-0101', 'orders@medsupply.co.za', '12 Distribution Rd, Johannesburg'),
    ('PharmaWholesale Ltd.', 'Sipho Dlamini', '021-555-0202', 'sales@pharmawholesale.co.za', '45 Industrial Ave, Cape Town'),
    ('Health Direct Distributors', 'Amara Botha', '031-555-0303', 'info@healthdirect.co.za', '7 Logistics Park, Durban');

INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
    ('Paracetamol 500mg', 'Adcock Ingram', 'Tablet', 25.99, 150, 30, '2027-06-30', 1),
    ('Amoxicillin 250mg', 'Aspen Pharmacare', 'Capsule', 89.50, 60, 20, '2026-11-15', 1),
    ('Cough Syrup 100ml', 'Cipla', 'Syrup', 45.00, 40, 15, '2026-09-01', 2),
    ('Insulin Injection', 'Novo Nordisk', 'Injection', 320.00, 15, 10, '2026-08-20', 3),
    ('Hydrocortisone Cream', 'Pharma Dynamics', 'Cream', 65.75, 25, 10, '2027-01-10', 2),
    ('Ibuprofen 400mg', 'Adcock Ingram', 'Tablet', 32.50, 8, 25, '2026-08-25', 1),
    ('Vitamin C 1000mg', 'Sandoz', 'Tablet', 55.00, 100, 20, '2027-12-01', 3);
