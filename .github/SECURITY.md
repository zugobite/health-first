# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of PIMS (HealthFirst) seriously. If you discover a security vulnerability, please follow these steps:

### 1. Do Not Open a Public Issue

Please **do not** open a GitHub issue for security vulnerabilities, as this could expose the vulnerability to malicious actors.

### 2. Contact Us Privately

Send a detailed report to the maintainer via:

- **GitHub:** Open a private security advisory via the "Security" tab of this repository
- **Email:** Contact the repository owner directly

### 3. Include the Following Information

- A description of the vulnerability
- Steps to reproduce the issue
- Potential impact of the vulnerability
- Any suggested fixes (optional but appreciated)

### 4. Response Timeline

- **Initial Response:** Within 48 hours
- **Status Update:** Within 7 days
- **Resolution:** Depends on complexity, typically within 30 days

## Security Best Practices for Contributors

When contributing to PIMS, please ensure:

1. **Never commit sensitive data** (passwords, DB credentials, API keys) to the repository
2. **Use `PreparedStatement`** for all JDBC queries - never string-concatenate SQL
3. **Validate all user input** before processing to prevent injection attacks
4. **Use exception handling** for all database and file I/O operations
5. **Do not hardcode credentials** or configuration values in source code
6. **Enforce role-based access control** on every Admin-only and Cashier-only action, both in the UI and the service layer

## Known Security Features

PIMS implements the following security measures:

- :white_check_mark: `PreparedStatement`-based JDBC access throughout, including login
- :white_check_mark: Input validation for all user-facing fields
- :white_check_mark: Role-based access control preventing Cashier accounts from reaching Admin CRUD actions
- :white_check_mark: Single-transaction checkout (sale + sale items + stock decrement)
- :white_check_mark: Graceful error handling for invalid input and database failures

## Acknowledgments

We appreciate security researchers who responsibly disclose vulnerabilities. Contributors who report valid security issues will be acknowledged in our release notes (with permission).
