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
    │       ├── AdminController(user, this::logout)    # If role = Admin
    │       └── CashierController(user, this::logout)  # If role = Cashier
    │
    ├── Role-based Controller
            │
            ├── AdminController(user, onLogout)
            │       ├── Account menu           # View Profile / Log Out (AccountMenuBar)
            │       ├── Manage Medicines        # Full CRUD via MedicineService + MedicineFormDialog
            │       ├── Manage Suppliers        # Full CRUD via SupplierService + SupplierFormDialog
            │       ├── Manage Users            # Create/delete Cashier accounts via UserService + CashierFormDialog
            │       └── Reports                 # Sales, Item-Wise, Low Stock, Expiry
            │
            └── CashierController(user, onLogout)
                    ├── Account menu           # View Profile / Log Out (AccountMenuBar)
                    ├── POS                    # Search medicines, build cart, checkout
                    ├── Bill                   # Print, or Save as PDF via ReceiptPdfService
                    └── Stock Check            # Look up price/availability
    │
    └── "Log Out" (from either dashboard) ──▶ LoginController.logout() ──▶ back to LoginView
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
│   ├── ReportService.java                    # Sales, Item-Wise, Low Stock, Expiry reports
│   ├── ReceiptPdfService.java                 # Renders a Sale as a typeset PDF receipt (Apache PDFBox)
│   └── ReportExportService.java               # Exports any report to paginated PDF or styled Excel (Apache POI)
│
├── controllers/
│   ├── LoginController.java                  # Authentication, role-based routing, and logout
│   ├── AdminController.java                  # Admin dashboard wiring
│   └── CashierController.java                # Cashier dashboard wiring
│
├── views/
│   ├── LoginView.java                        # Login screen
│   ├── AdminDashboardView.java                # Tabbed Admin shell (with AccountMenuBar)
│   ├── CashierDashboardView.java              # Cashier shell (with AccountMenuBar)
│   ├── AccountMenuBar.java                    # Shared "View Profile / Log Out" menu bar
│   ├── ProfileDialog.java                     # Read-only "who am I logged in as" dialog
│   ├── ManageMedicinesPanel.java              # Medicines CRUD tab
│   ├── MedicineFormDialog.java                 # Add/Edit form for a single medicine
│   ├── ManageSuppliersPanel.java              # Suppliers CRUD tab
│   ├── SupplierFormDialog.java                 # Add/Edit form for a single supplier
│   ├── ManageUsersPanel.java                  # Cashier account management tab
│   ├── CashierFormDialog.java                  # Add form for a new Cashier account
│   ├── ReportsPanel.java                      # Reports tab (all four reports)
│   ├── PosPanel.java                          # POS screen (search, cart, checkout)
│   ├── BillView.java                          # Bill window - Print, or Save as PDF
│   ├── StockCheckPanel.java                   # Price/availability lookup
│   └── StripedTable.java                      # Shared zebra-striped JTable used by every data table
│
└── utils/
    ├── InputValidator.java                   # Input validation utilities
    └── UIUtils.java                          # Look and feel, brand color/icon, layout helpers
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
| `ReceiptPdfService`     | Service    | Renders a Sale as a typeset PDF receipt (Apache PDFBox)     |
| `ReportExportService`   | Service    | Exports any report to paginated PDF or styled Excel (Apache POI) |
| `LoginController`       | Controller | Authenticates, routes to Admin/Cashier dashboards, and handles logout |
| `AdminController`       | Controller | Wires Admin views to services                              |
| `CashierController`     | Controller | Wires Cashier views to services                            |
| `AccountMenuBar`        | View       | Shared "View Profile / Log Out" menu bar on both dashboards |
| `ProfileDialog`         | View       | Read-only display of the logged-in user's own details       |
| `StripedTable`          | View       | Zebra-striped `JTable` used by every data table in the app  |
| `InputValidator`        | Utility    | Validation for prices, quantities, dates, and forms         |
| `UIUtils`               | Utility    | Look and feel, brand color/icon, layout, and dialog helpers  |
| `Main`                  | Entry      | Application entry point                                    |

For the full requirements specification, see [docs/001-APPLICATION_REQUIREMENTS.md](001-APPLICATION_REQUIREMENTS.md).

---

## Visual Design

PLAN.md §6 explicitly calls out "a neatly designed interface" as a grading criterion, so the
UI layer makes a few deliberate choices beyond plain default-Swing widgets:

- **FlatLaf, not Metal** - `UIUtils.installLookAndFeel()` installs FlatLaf's light theme
  (`FlatLightLaf`) in place of Java's default "Metal" look, before any component is
  constructed (see `Main.main()`). This alone replaces every button, field, table, and
  scrollbar's rendering app-wide with a modern, flat style, without any other class needing
  to change.
- **One brand color** - `UIUtils.ACCENT_COLOR` (a medical/pharmacy-appropriate teal) is
  passed to FlatLaf as the global accent color, so focus rings, the default-button
  highlight, and selection backgrounds are all derived from a single value. The same color
  drives the login screen's header band and the POS screen's total label.
- **A branded app icon** - `UIUtils.createAppIcon()` procedurally draws a simple
  rounded-square "pharmacy cross" mark (no bitmap asset to track) that every top-level
  window uses as its title-bar icon (`UIUtils.applyAppIcon`), and that `Main` also sets as
  the OS Dock/taskbar icon where supported.
- **Consistent, professional data tables** - every `JTable` in the app is a
  `StripedTable`, giving zebra-striped rows, taller row height, and no default grid lines
  app-wide from one shared class. Numeric columns (IDs, prices, quantities) declare their
  real Java type via `getColumnClass()` overrides on the underlying `DefaultTableModel`,
  which lets Swing's own built-in `Number` renderer right-align them automatically - the
  same convention `ReportExportService`'s PDF/Excel exports use, so what's on screen matches
  what gets exported.
