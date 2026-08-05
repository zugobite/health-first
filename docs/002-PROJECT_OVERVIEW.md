# Project Overview

## Architecture

PIMS is a Java Swing desktop Pharmacy Inventory Management System backed by MySQL via JDBC. The application follows object-oriented principles with a clear separation of concerns across well-defined packages.

### Application Flow

```
Main.java (Entry Point)
    │
    ├── DBConnection.getConnection()          # Verify JDBC connectivity to pims_db
    │
    ├── LoginController.showLogin()           # Display login screen, authenticate against `users`
    │       │
    │       ├── AdminController               # If role = Admin
    │       └── CashierController              # If role = Cashier
    │
    ├── Role-based Controller
            │
            ├── AdminController(user)
            │       ├── Manage Medicines      # Full CRUD via MedicineService
            │       ├── Manage Suppliers      # Full CRUD via SupplierService
            │       ├── Manage Users          # Create/delete Cashier accounts via UserService
            │       └── Reports               # Sales, Item-Wise, Low Stock, Expiry
            │
            └── CashierController(user)
                    ├── POS                   # Search medicines, build cart, checkout
                    ├── Bill                  # Generate/print bill after checkout
                    └── Stock Check           # Look up price/availability
```

---

## Package Map

```
com.zugobite.pims
├── Main.java                                 # Application entry point
│
├── enums/
│   └── Role.java                             # ADMIN, CASHIER enum
│
├── models/
│   ├── User.java                             # User account (OOP: encapsulation)
│   ├── Medicine.java                         # Medicine domain model
│   ├── Supplier.java                         # Supplier domain model
│   ├── Sale.java                             # Sale header
│   └── SaleItem.java                         # Sale line item
│
├── data/
│   └── DBConnection.java                     # Centralised JDBC connection utility
│
├── services/
│   ├── UserService.java                      # Authentication, Cashier account management
│   ├── MedicineService.java                  # Medicine CRUD, low-stock/expiry queries
│   ├── SupplierService.java                  # Supplier CRUD
│   ├── SalesService.java                     # Cart checkout, transaction handling
│   └── ReportService.java                    # Sales, Item-Wise, Low Stock, Expiry reports
│
├── controllers/
│   ├── LoginController.java                  # Authentication and role-based routing
│   ├── AdminController.java                  # Admin dashboard wiring
│   └── CashierController.java                # Cashier dashboard wiring
│
├── views/
│   ├── LoginView.java                        # Login screen
│   ├── AdminDashboardView.java                # Tabbed Admin shell
│   ├── ManageMedicinesPanel.java              # Medicines CRUD tab
│   ├── ManageSuppliersPanel.java              # Suppliers CRUD tab
│   ├── ManageUsersPanel.java                  # Cashier account management tab
│   ├── ReportsPanel.java                      # Reports tab (all four reports)
│   ├── CashierDashboardView.java              # Cashier shell
│   ├── PosPanel.java                          # POS screen (search, cart, checkout)
│   ├── BillView.java                          # Generated bill window
│   └── StockCheckPanel.java                   # Price/availability lookup
│
└── utils/
    ├── InputValidator.java                   # Input validation utilities
    └── UIUtils.java                          # Swing layout/formatting helpers
```

---

## Class Hierarchy

```
User
├── userId: int
├── username, fullName: String
├── role: Role
│
Medicine
├── medicineId: int (final)
├── name, company, medicineType: String
├── price: BigDecimal
├── quantityInStock, reorderLevel: int
├── expiryDate: LocalDate
├── supplierId: int
│
Supplier
├── supplierId: int (final)
├── name, contactPerson, phone, email, address: String
│
Sale
├── saleId: int (final)
├── saleDate: LocalDateTime
├── totalAmount: BigDecimal
├── userId: int
├── items: List<SaleItem>
│
SaleItem
├── saleItemId: int (final)
├── saleId, medicineId, quantitySold: int
├── priceAtSale: BigDecimal
```

---

## Class Reference

| Class                   | Type       | Description                                              |
| ----------------------- | ---------- | ---------------------------------------------------------|
| `User`                  | Model      | User account with id, username, role, full name           |
| `Medicine`              | Model      | Inventory item with stock, reorder level, expiry, supplier|
| `Supplier`              | Model      | Supplier contact details                                  |
| `Sale`                  | Model      | Sale header with date, total, cashier                     |
| `SaleItem`              | Model      | Line item linking a sale to a medicine                    |
| `Role`                  | Enum       | ADMIN and CASHIER role constants                           |
| `DBConnection`          | Data       | Centralised JDBC `Connection` factory                     |
| `UserService`           | Service    | Login authentication, Cashier account CRUD                |
| `MedicineService`       | Service    | Medicine CRUD, low-stock/expiry lookups                   |
| `SupplierService`       | Service    | Supplier CRUD                                              |
| `SalesService`          | Service    | Cart checkout as a single JDBC transaction                |
| `ReportService`         | Service    | Sales, Item-Wise, Low Stock, Expiry report queries         |
| `LoginController`       | Controller | Authenticates and routes to Admin/Cashier dashboards       |
| `AdminController`       | Controller | Wires Admin views to services                              |
| `CashierController`     | Controller | Wires Cashier views to services                            |
| `InputValidator`        | Utility    | Validation for prices, quantities, dates, and forms         |
| `UIUtils`               | Utility    | Swing layout, spacing, and table formatting helpers         |
| `Main`                  | Entry      | Application entry point                                    |

For the full requirements specification, see [docs/001-APPLICATION_REQUIREMENTS.md](001-APPLICATION_REQUIREMENTS.md).
