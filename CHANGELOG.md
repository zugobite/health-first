# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Fixed

- `PosPanel`: the POS total-due figure and the change-due readout were rendering blank
  (only the "TOTAL DUE"/tender-field chrome was visible, with no amount underneath) because
  their containing blocks' `maximumSize` was computed from `totalLabel`/`changeLabel`'s
  *empty-string* preferred size before `refreshCart()` ever set real text - under FlatLaf an
  empty `JLabel` reports a near-zero height, permanently capping the block too short for the
  30pt total once real text arrived. Fixed by initialising both labels with real-shaped text
  (`"R 0.00"` / `" "`) instead of `""`, so the initial size measurement is representative.

### Added

- `/screenshots`: the submission deliverable's screenshot set, captured end-to-end against a
  live local MySQL instance and organised into `admin/` and `cashier/` subfolders, each
  numbered in the order that role's own login flow visits the screens:
  - `admin/`: login → dashboard overview (live stats + module grid) → Manage Medicines
    (filter box, low-stock/expiring row tinting) → Manage Suppliers → Manage Users → all
    four reports (Sales, Item-Wise, Low Stock, Expiry), the first two showing a real
    transaction rung up during capture
  - `cashier/`: dashboard overview → POS with an empty cart → POS with an item in the cart
    → the generated Bill window → the native "Save as PDF" dialog → Stock Check before and
    after a search

- `README.txt`: the plain-text user manual + default-credentials deliverable PLAN.md's
  submission checklist requires, alongside `/src`, `database.sql`, and `/screenshots`.
  Covers login credentials for both seeded accounts, first-time MySQL setup, a walkthrough
  of every Admin/Cashier screen, grader-facing notes (PreparedStatement usage, the
  intentional plaintext-password decision, the transactional checkout), and troubleshooting.
  `README.md` now links to it and shows the same credentials as a table.
- Full UI redesign toward an "enterprise SaaS dashboard" visual language (a Swing/FlatLaf
  approximation of shadcn/ui's neutral, card-based design system - ShadCN itself is a
  React/Tailwind library with no Java equivalent, so this reproduces its *look*, not the
  library):
  - `CardPanel`: a bordered, rounded-corner white container used to wrap every screen's main
    content, replacing bare panels sitting directly on the window background
  - `SidebarNav`: a left-hand navigation rail (brand header, nav items, user/logout footer)
    that replaces the top `JTabbedPane` and the old `AccountMenuBar` on both dashboards -
    `AdminDashboardView` and `CashierDashboardView` now use a `CardLayout` content area
    driven by the sidebar instead of tabs
  - A neutral "zinc" design-token palette plus a button variant system in `UIUtils`
    (`styleAsPrimaryButton`/`styleAsOutlineButton`/`styleAsDestructiveButton`/`styleAsGhostButton`,
    built on FlatLaf's per-component `FlatClientProperties.STYLE` API) applied consistently
    across every screen and dialog - one dark primary action per screen, neutral outline
    buttons for secondary actions, red-outline for destructive ones
  - Global FlatLaf tuning (`UIUtils.installLookAndFeel()`): rounded corners app-wide, a
    thinner focus ring, pill-shaped minimal scrollbars, and table colors matching the new
    palette
  - Redesigned login screen: a centered `CardPanel` "auth card" on a muted page background,
    replacing the previous edge-to-edge header-band layout
  - A new procedurally-drawn app icon in the style of macOS's "Liquid Glass" icon redesign -
    a squircle (superellipse, not a rounded rectangle) with a lit gradient surface, a glossy
    highlight, a darkened lower edge, and a layered drop-shadowed glyph - replacing the
    previous flat single-color rounded-square icon
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
  "Log Out", which returns to the login screen without restarting the JVM - closes the login
  loop that previously only went one direction
- `ReceiptPdfService`, built on Apache PDFBox: a "Save as PDF" button on `BillView` renders the
  bill as a professionally typeset, single-page A4 PDF receipt (header, ruled sale-info block,
  shaded item table with right-aligned currency columns, bold total, footer) - alongside the
  existing "Print" button
- `SalesService.checkout` now reads back and populates `Sale.saleDate` from the database
  (previously left null), so bills/receipts show the real persisted transaction timestamp
- End-to-end verified against a live local MySQL instance: login (both roles), medicine
  search/CRUD, POS checkout (transaction + stock decrement), bill print/PDF export, and logout
- `ReportExportService`, built on Apache PDFBox and Apache POI: "Export to PDF" and "Export
  to Excel" buttons on the Reports tab render any of the four reports as either a paginated,
  titled PDF table (repeats the column header on every page, right-aligns numeric columns)
  or a styled `.xlsx` workbook (bold shaded header row, real numeric/date/currency cell
  types, auto-sized columns) - both driven off the same generic report row shape
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
    `getColumnClass()` overrides feeding Swing's built-in `Number` cell renderer - consistent
    with the alignment already used in `ReceiptPdfService`/`ReportExportService`'s output

### Changed

- Nothing yet

### Fixed

- Nothing yet

---

[Unreleased]: https://github.com/zugobite/health-first/compare/main...HEAD
