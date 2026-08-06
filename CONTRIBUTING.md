# Contributing to PIMS (HealthFirst)

First off, thank you for considering contributing to PIMS! Your help makes this project better for everyone.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Style Guidelines](#style-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)

---

## Code of Conduct

This project and everyone participating in it is governed by our commitment to providing a welcoming and inclusive environment. By participating, you are expected to uphold this standard. Please report unacceptable behavior to the repository maintainers.

---

## Getting Started

Before you begin:

- Make sure you have a [GitHub account](https://github.com/signup)
- Ensure you have **Java 17+** installed (`java -version`)
- Ensure you have **MySQL 8.0+** running locally, with the JDBC driver (`mysql-connector-j`) on the classpath
- Familiarize yourself with the [project structure](README.md#project-structure)
- Check existing [issues](../../issues) to see if your contribution is already being discussed

---

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues. When filing a bug report, use our [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) and include:

- A clear and descriptive title
- Steps to reproduce the behavior
- Expected vs actual behavior
- Your environment (OS, Java version, MySQL version)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. Use our [feature request template](.github/ISSUE_TEMPLATE/feature_request.md) and provide:

- A clear and descriptive title
- A detailed description of the proposed enhancement
- Why this enhancement would be useful
- Any alternatives you've considered

### Pull Requests

1. Fork the repository
2. Create a new branch from `main`
3. Make your changes
4. Submit a pull request

---

## Development Setup

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- MySQL 8.0+, the `mysql-connector-j` JDBC driver, `pdfbox-app` (Apache PDFBox), and Apache
  POI + its runtime dependencies (see the full jar list in [README.md](README.md#installation))
- Git

### Local Setup

```bash
# 1. Clone your fork
git clone https://github.com/YOUR_USERNAME/health-first.git
cd health-first

# 2. Load the schema and seed data
mysql -u root -p < database.sql

# 3. Fetch the runtime jars into lib/ — see the full curl commands in
#    README.md#installation (JDBC driver, PDFBox, and Apache POI + dependencies)

CP="lib/mysql-connector-j.jar:lib/pdfbox-app.jar:lib/poi.jar:lib/poi-ooxml.jar:lib/poi-ooxml-lite.jar:lib/xmlbeans.jar:lib/commons-compress.jar:lib/commons-io.jar:lib/commons-collections4.jar:lib/log4j-api.jar:lib/flatlaf.jar"

# 4. Compile all source files
find src -name "*.java" | xargs javac -cp "$CP" -d out

# 5. Run the application
java -cp "out:$CP" com.zugobite.pims.Main
```

### Project Structure

```
src/main/java/com/zugobite/pims/
├── Main.java              # Application entry point
├── enums/                 # Enumerations (Role, MedicineType, ...)
├── models/                # Domain objects (User, Medicine, Supplier, Sale, SaleItem)
├── data/                  # JDBC connection utility and DAOs
├── services/              # Business logic layer (inventory, sales, reports, users)
├── controllers/           # Login/session and role-based routing
├── views/                 # Swing screens (Login, dashboards, POS, reports)
└── utils/                 # Utility classes (validation, UI helpers)
```

---

## Style Guidelines

### Java Code Style

- Use **4 spaces** for indentation (no tabs)
- Use **meaningful variable and method names** in camelCase
- Use **PascalCase** for class names
- Use **UPPER_SNAKE_CASE** for constants
- Add **Javadoc comments** for all public classes, methods, and fields
- Keep methods short and focused - one responsibility per method
- Prefer **early returns** over deep nesting
- Always use **`PreparedStatement`** for JDBC queries - never string-concatenate SQL

### Example

```java
/**
 * Validates that the given string is not null or empty.
 *
 * @param input the string to validate
 * @return true if the string is non-empty, false otherwise
 */
public static boolean isNonEmpty(String input) {
    return input != null && !input.trim().isEmpty();
}
```

### Package Conventions

- `models/` - Plain Java objects representing domain entities
- `enums/` - Enumeration types
- `data/` - JDBC connection management and data access
- `services/` - Business logic and operations on models
- `controllers/` - Login/session handling and role-based routing
- `views/` - Swing `JFrame`/`JPanel` screens
- `utils/` - Stateless utility/helper classes

### Security Guidelines

- **Validate all user input** before processing
- **Never hardcode credentials** or sensitive data
- Use **exception handling** for all database operations
- Use **`PreparedStatement`** everywhere, especially for login

---

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation changes only |
| `style` | Formatting, missing semicolons, etc. (no code change) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or updating tests |
| `chore` | Build process, tooling, or auxiliary changes |

### Examples

```
feat(views): add Cashier POS screen with cart total calculation
fix(services): resolve stock not decrementing on checkout
docs(readme): update installation instructions
refactor(controllers): extract role-based routing to base class
chore(gitignore): add IDE-specific excludes
```

---

## Pull Request Process

1. **Update documentation** if you're changing functionality
2. **Update the CHANGELOG.md** with your changes under `[Unreleased]`
3. **Ensure your code compiles** without errors
4. **Fill out the PR template** completely
5. **Request a review** from a maintainer
6. **Address feedback** promptly

### PR Checklist

- [ ] Code follows the project's style guidelines
- [ ] Self-review completed
- [ ] Javadoc comments added for public classes and methods
- [ ] Documentation updated (if applicable)
- [ ] No new compiler warnings generated
- [ ] CHANGELOG.md updated under `[Unreleased]`
- [ ] Changes compile and run successfully

---

## Questions?

Feel free to open an issue with the `question` label if you need help getting started.
