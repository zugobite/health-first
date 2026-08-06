# PIMS - Pharmacy Inventory Management System

A multi-user Java Swing desktop application for a pharmacy ("HealthFirst") that manages inventory, processes sales, and generates business reports - with role-based access (Admin vs. Cashier).

[![Java](https://img.shields.io/badge/Java-17+-ED8B00.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1.svg)](https://www.mysql.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- **Role-Based Access Control** - Admins and cashiers have separate permissions and dashboards
- **Inventory Management** - Full CRUD on medicines and suppliers, including expiry date and supplier link
- **Point of Sale** - Search medicines, build a cart, calculate totals from live prices, checkout in a single transaction
- **Stock Control** - Automatic `quantity_in_stock` decrement on checkout, low-stock alerting via reorder level
- **Bill Generation** - Bill window after checkout with both "Print" (Swing's built-in print pipeline) and "Save as PDF" (a professionally typeset PDF receipt via Apache PDFBox)
- **User Management** - Admin creates and manages Cashier accounts
- **Account Menu** - View your own profile and log out to switch users without restarting the app
- **Reporting** - Sales, Item-Wise, Low Stock, and Expiry (next 30 days) reports, each exportable to a paginated PDF or a styled Excel workbook
- **JDBC + MySQL** - `PreparedStatement`-based data access throughout, no string-concatenated SQL
- **Modern, Branded UI** - FlatLaf-based flat design with a HealthFirst teal accent color, a branded app icon, striped/right-aligned data tables, and consistent spacing throughout

## Table of Contents

- [Quick Start](#quick-start)
- [Documentation](#documentation)
- [User Roles](#user-roles)
- [Project Structure](#project-structure)
- [Class Reference](#class-reference)
- [Development](#development)
- [Contributing](#contributing)
- [Disclaimer](#disclaimer)
- [Security](#security)
- [License](#license)

## Quick Start

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- MySQL 8.0 or higher
- `mysql-connector-j` (JDBC), `pdfbox-app` (PDF generation), Apache POI + its runtime
  dependencies (Excel generation), and FlatLaf (the modern Look and Feel) on the classpath -
  see the full jar list below

### Installation

```bash
# Clone the repository
git clone https://github.com/zugobite/health-first.git
cd health-first

# Load the schema and seed data
mysql -u root -p < database.sql

# Fetch the runtime jars into lib/
mkdir -p lib
curl -sL -o lib/mysql-connector-j.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
curl -sL -o lib/pdfbox-app.jar https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-app/3.0.8/pdfbox-app-3.0.8.jar
curl -sL -o lib/poi.jar https://repo1.maven.org/maven2/org/apache/poi/poi/5.5.1/poi-5.5.1.jar
curl -sL -o lib/poi-ooxml.jar https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml/5.5.1/poi-ooxml-5.5.1.jar
curl -sL -o lib/poi-ooxml-lite.jar https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml-lite/5.5.1/poi-ooxml-lite-5.5.1.jar
curl -sL -o lib/xmlbeans.jar https://repo1.maven.org/maven2/org/apache/xmlbeans/xmlbeans/5.3.0/xmlbeans-5.3.0.jar
curl -sL -o lib/commons-compress.jar https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.28.0/commons-compress-1.28.0.jar
curl -sL -o lib/commons-io.jar https://repo1.maven.org/maven2/commons-io/commons-io/2.21.0/commons-io-2.21.0.jar
curl -sL -o lib/commons-collections4.jar https://repo1.maven.org/maven2/org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0.jar
curl -sL -o lib/log4j-api.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.24.3/log4j-api-2.24.3.jar
curl -sL -o lib/flatlaf.jar https://repo1.maven.org/maven2/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar

CP="lib/mysql-connector-j.jar:lib/pdfbox-app.jar:lib/poi.jar:lib/poi-ooxml.jar:lib/poi-ooxml-lite.jar:lib/xmlbeans.jar:lib/commons-compress.jar:lib/commons-io.jar:lib/commons-collections4.jar:lib/log4j-api.jar:lib/flatlaf.jar"

# Compile all Java source files
find src -name "*.java" | xargs javac -cp "$CP" -d out

# Run the application
java -Xdock:name="HealthFirst PIMS" -cp "out:$CP" com.zugobite.pims.Main
```

Default credentials (from seed data): `admin/admin123` (Admin), `cashier/cash123` (Cashier).

## Documentation

Extensive developer documentation is available in the [`docs/`](docs/) folder:

| Document                                                                | Description                                    |
| ----------------------------------------------------------------------- | ---------------------------------------------- |
| [001-APPLICATION_REQUIREMENTS.md](docs/001-APPLICATION_REQUIREMENTS.md) | Full application requirements specification    |
| [002-PROJECT_OVERVIEW.md](docs/002-PROJECT_OVERVIEW.md)                 | Architecture, package map, and class hierarchy |

## User Roles

| Role        | Can do                                                                                           |
| ----------- | -------------------------------------------------------------------------------------------------|
| **Cashier** | Process sales (POS), generate/print bills, check stock/price. Cannot add/edit/delete medicines.  |
| **Admin**   | Full CRUD on medicines & suppliers, manage cashier user accounts, view all reports.               |

## Project Structure

```
health-first/
├── .github/
│   ├── ISSUE_TEMPLATE/                 # Issue templates
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   ├── PULL_REQUEST_TEMPLATE.md        # PR template
│   ├── SECURITY.md                     # Security policy
│   ├── CODEOWNERS                      # Code ownership rules
│   └── FUNDING.yml                     # Sponsorship info
├── src/main/java/com/zugobite/pims/
│   ├── Main.java                       # Application entry point
│   ├── enums/
│   │   └── Role.java                   # ADMIN / CASHIER enum
│   ├── models/
│   │   ├── User.java                   # User account
│   │   ├── Medicine.java               # Medicine domain model
│   │   ├── Supplier.java               # Supplier domain model
│   │   ├── Sale.java                   # Sale header
│   │   └── SaleItem.java               # Sale line item
│   ├── data/
│   │   └── DBConnection.java           # Centralised JDBC connection utility
│   ├── services/
│   │   ├── UserService.java            # Auth & Cashier account management
│   │   ├── MedicineService.java        # Medicine CRUD, low-stock/expiry queries
│   │   ├── SupplierService.java        # Supplier CRUD
│   │   ├── SalesService.java           # Cart checkout transaction
│   │   ├── ReportService.java          # Sales / Item-Wise / Low Stock / Expiry reports
│   │   ├── ReceiptPdfService.java      # Renders a Sale as a typeset PDF receipt (Apache PDFBox)
│   │   └── ReportExportService.java    # Exports any report to paginated PDF or styled Excel (POI)
│   ├── controllers/
│   │   ├── LoginController.java        # Authentication, role routing, and logout
│   │   ├── AdminController.java        # Admin dashboard wiring
│   │   └── CashierController.java      # Cashier dashboard wiring
│   ├── views/                          # Swing screens (Login, dashboards, POS, bill, reports, Account menu)
│   │   └── StripedTable.java           # Shared zebra-striped JTable used by every data table
│   └── utils/
│       ├── InputValidator.java         # Input validation helpers
│       └── UIUtils.java                # Look and feel, brand color/icon, layout helpers
├── data/                               # Runtime data directory
├── docs/                               # Developer documentation
│   ├── 001-APPLICATION_REQUIREMENTS.md
│   └── 002-PROJECT_OVERVIEW.md
├── database.sql                        # CREATE DATABASE / CREATE TABLE / seed INSERTs
├── .gitignore                          # Git ignore rules
├── CHANGELOG.md                        # Version history
├── CONTRIBUTING.md                     # Contribution guidelines
├── LICENSE                             # MIT License
└── README.md
```

## Class Reference

See [docs/002-PROJECT_OVERVIEW.md](docs/002-PROJECT_OVERVIEW.md) for the complete package map and class hierarchy.

## Development

```bash
CP="lib/mysql-connector-j.jar:lib/pdfbox-app.jar:lib/poi.jar:lib/poi-ooxml.jar:lib/poi-ooxml-lite.jar:lib/xmlbeans.jar:lib/commons-compress.jar:lib/commons-io.jar:lib/commons-collections4.jar:lib/log4j-api.jar:lib/flatlaf.jar"

# Compile all source files
find src -name "*.java" | xargs javac -cp "$CP" -d out

# Run the application
java -Xdock:name="HealthFirst PIMS" -cp "out:$CP" com.zugobite.pims.Main
```

### Architecture

This project follows an MVC-inspired architecture with clean package separation:

- **Models** define domain entities with encapsulated fields and behavior
- **Views** are Swing `JFrame`/`JPanel` screens for each workflow
- **Controllers** wire views to services and handle role-based routing
- **Services** contain business logic and JDBC data access
- **Utils** provide stateless helper methods for validation and layout
- **Enums** define type-safe constants for roles

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

### Guidelines

- Follow the existing code style
- Add Javadoc comments for new classes and methods
- Update documentation as needed
- Keep commits atomic and well-described

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## Disclaimer

This project is created **purely for educational and portfolio demonstration purposes** to showcase Java development skills and software engineering knowledge (Programming 732 Assignment 1, Richfield GIT).

**Important notices:**

- It implements **industry-standard patterns** documented in publicly available resources including:
  - Object-oriented design principles (abstraction, inheritance, polymorphism, encapsulation)
  - JDBC data access patterns with `PreparedStatement`
  - MVC architectural separation
- This is **not intended for production use** without proper security audits and additional hardening
- The codebase demonstrates OOP principles, JDBC/MySQL integration, transactional checkout, and modular design

## Security

If you discover a security vulnerability, please report it privately rather than opening a public issue. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
