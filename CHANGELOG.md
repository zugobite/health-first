# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- Project scaffold: package structure (`enums`, `models`, `data`, `services`, `controllers`, `views`, `utils`)
- `database.sql` schema (`users`, `suppliers`, `medicines`, `sales`, `sale_items`)
- `docs/001-APPLICATION_REQUIREMENTS.md` and `docs/002-PROJECT_OVERVIEW.md`
- `.github` meta (issue templates, PR template, security policy, CODEOWNERS, FUNDING)
- `LICENSE`, `CONTRIBUTING.md`, `README.md`, `.gitignore`
- Full Admin CRUD: `MedicineFormDialog`, `SupplierFormDialog`, and `CashierFormDialog` wired into
  `ManageMedicinesPanel`, `ManageSuppliersPanel`, and `ManageUsersPanel` (Add/Edit/Delete with
  confirmation prompts and `InputValidator`-backed form validation)
- Comprehensive Javadoc and inline maintainer comments across every class in `src/`
- Account menu (`AccountMenuBar`) on both dashboards: "View Profile..." (`ProfileDialog`) and
  "Log Out", which returns to the login screen without restarting the JVM — closes the login
  loop that previously only went one direction
- `ReceiptPdfService`, built on Apache PDFBox: a "Save as PDF" button on `BillView` renders the
  bill as a professionally typeset, single-page A4 PDF receipt (header, ruled sale-info block,
  shaded item table with right-aligned currency columns, bold total, footer) — alongside the
  existing "Print" button
- `SalesService.checkout` now reads back and populates `Sale.saleDate` from the database
  (previously left null), so bills/receipts show the real persisted transaction timestamp
- End-to-end verified against a live local MySQL instance: login (both roles), medicine
  search/CRUD, POS checkout (transaction + stock decrement), bill print/PDF export, and logout
- `ReportExportService`, built on Apache PDFBox and Apache POI: "Export to PDF" and "Export
  to Excel" buttons on the Reports tab render any of the four reports as either a paginated,
  titled PDF table (repeats the column header on every page, right-aligns numeric columns)
  or a styled `.xlsx` workbook (bold shaded header row, real numeric/date/currency cell
  types, auto-sized columns) — both driven off the same generic report row shape
  `ReportService` already produces
- Full visual redesign for PLAN.md §6's "neatly designed interface" grading criterion:
  - FlatLaf (light theme) replaces Swing's default "Metal" look and feel app-wide
    (`UIUtils.installLookAndFeel()`, installed first thing in `Main.main()`)
  - A single teal brand accent color (`UIUtils.ACCENT_COLOR`) drives FlatLaf's focus rings,
    default-button highlight, and selection colors, plus the login screen's header band and
    the POS screen's total label
  - A procedurally-drawn app icon (`UIUtils.createAppIcon()`, no bitmap asset) on every
    window's title bar and, where supported, the OS Dock/taskbar
  - Redesigned login screen: branded header band, stacked label-above-field layout,
    full-width primary button
  - `StripedTable`: a shared zebra-striped `JTable` now used by every data table in the app
    (Manage Medicines/Suppliers/Users, Reports, POS search/cart, Stock Check), with taller
    rows and no default grid lines
  - Numeric table columns (IDs, prices, quantities) now right-align automatically via
    `getColumnClass()` overrides feeding Swing's built-in `Number` cell renderer — consistent
    with the alignment already used in `ReceiptPdfService`/`ReportExportService`'s output

### Changed

- Nothing yet

### Fixed

- Nothing yet

---

[Unreleased]: https://github.com/zugobite/health-first/compare/main...HEAD
