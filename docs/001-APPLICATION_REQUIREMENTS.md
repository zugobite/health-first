# Application Requirements Specification

> **PIMS - Pharmacy Inventory Management System**
>
> Scenario client: a local pharmacy called **"HealthFirst"**.
>
> Author: Zascia Hugo · Student Number: 402111581 · Course: PRO732
>
> This document defines the complete functional, technical, and submission requirements for the PIMS platform. Converted from Programming 732 Assignment 1 (Richfield GIT).

## 1. Overview

PIMS is a **Java Swing desktop application** that lets a pharmacy manage its medicine inventory, process point-of-sale transactions, and generate business reports.

The system simulates real-world behavior including:

- Role-based access control (Admin vs. Cashier)
- Live stock management with low-stock and expiry alerts
- Transactional checkout (sale + line items + stock decrement)
- JDBC-backed persistence against a MySQL database

## 2. Functional Requirements

### 2.1 User Roles and Access Control

The system supports two user types with distinct permission sets:

#### Admin

| Action                              | Permitted |
| ------------------------------------ | --------- |
| Full CRUD on medicines                | Yes       |
| Full CRUD on suppliers                | Yes       |
| Manage Cashier user accounts          | Yes       |
| View all reports                      | Yes       |
| Process sales (POS)                   | No        |

#### Cashier

| Action                              | Permitted |
| ------------------------------------ | --------- |
| Process sales (POS)                   | Yes       |
| Generate/print bills                  | Yes       |
| Check stock/price                     | Yes       |
| Add, edit, or delete medicines        | No        |
| Manage suppliers or user accounts     | No        |

#### System Behavior

- Prompt the user to log in (username + password) against the `users` table at startup
- On success, route to `AdminDashboard` or `CashierDashboard` based on `role`
- On failure, show a clear error message (no crash, no silent fail)
- Cashier-facing views must not expose add/edit/delete controls for medicines

### 2.2 Inventory Management (Admin)

Each medicine must contain the following attributes:

| Field             | Type    | Format / Constraint                          |
| ------------------ | ------- | --------------------------------------------- |
| Medicine ID         | Integer | Unique, auto-increment                        |
| Name                | String  | Non-empty                                      |
| Company             | String  | Optional                                       |
| Medicine Type       | String  | e.g. Tablet, Capsule, Syrup, Injection, Cream  |
| Price               | Decimal | Positive, 2 decimal places                     |
| Quantity in Stock   | Integer | Non-negative                                   |
| Reorder Level       | Integer | Non-negative, alert threshold                  |
| Expiry Date         | Date    | Required for expiry reporting                  |
| Supplier            | FK      | References `suppliers`                         |

Admin has full CRUD on medicines and suppliers, including expiry date and supplier link, plus create/delete/manage of Cashier user accounts.

### 2.3 Point of Sale (Cashier)

- Search/select medicines, add to cart, adjust quantity
- Cart total calculated from live `price` in `medicines`
- Checkout writes one `sales` row and one `sale_items` row per line item, in a single transaction
- `quantity_in_stock` decremented on checkout
- Bill window generated with print and/or save support (payment gateway is simulated, not real)
- "Clear cart" action available at any point before checkout
- Stock check screen to look up a medicine's price/availability without starting a sale

### 2.4 Reporting

Four reports are required:

- **Sales report** - transactions over a date range
- **Item-Wise report** - sales aggregated per medicine (quantity sold, revenue)
- **Low Stock report** - medicines where `quantity_in_stock <= reorder_level`
- **Expiry report** - medicines expiring within the next 1 month from the current date

## 3. Non-Functional Requirements

- Clean, consistently spaced/aligned Swing layout with readable tables
- All SQL access via `PreparedStatement` - no string-concatenated SQL, especially for login
- Passwords hashed if going beyond baseline requirements

## 4. Deliverables

- `zasciahugo_pims.exe` - bundled executable + MySQL DB for easy testing
- `/src` - all `.java` source files
- `database.sql` - full CREATE DATABASE / CREATE TABLE / INSERT (seed) script
- `/screenshots` - Login screen; Admin Dashboard; Manage Medicines tab with data; Cashier POS with items in cart; generated Bill window; all four reports
- `README.txt` - user manual + default credentials
