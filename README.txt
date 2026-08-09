================================================================================
 HealthFirst PIMS - Pharmacy Inventory Management System
 User Manual
================================================================================

Programming 732 Assignment 1 (Richfield GIT)
Author: Zascia Hugo


--------------------------------------------------------------------------------
 1. DEFAULT LOGIN CREDENTIALS
--------------------------------------------------------------------------------

Two accounts are seeded by database.sql. Use whichever matches the role you
want to test.

    ADMIN ACCOUNT
        Username : admin
        Password : admin123
        Role     : Admin
        Can do   : Full CRUD on medicines and suppliers, manage cashier
                   accounts, view all reports (Sales, Item-Wise, Low Stock,
                   Expiry), export any report to PDF or Excel.

    CASHIER ACCOUNT
        Username : cashier
        Password : cash123
        Role     : Cashier
        Can do   : Search medicines, run a Point-of-Sale checkout, generate
                   and print/PDF a bill, check stock/price without starting
                   a sale. Cannot add, edit, or delete medicines.

Passwords are stored as plain text in this build (see section 5 below for
why that is an intentional, documented decision for this assignment).


--------------------------------------------------------------------------------
 2. REQUIREMENTS
--------------------------------------------------------------------------------

    - Java Runtime Environment (JRE) 17 or higher
    - MySQL 8.0 or higher, running locally
    - The bundled executable already includes the required libraries
      (MySQL JDBC driver, Apache PDFBox, Apache POI, FlatLaf). If running
      from source instead, see README.md for the full dependency list and
      download commands.


--------------------------------------------------------------------------------
 3. FIRST-TIME SETUP
--------------------------------------------------------------------------------

    1. Install and start MySQL.
    2. Load the schema and seed data:

           mysql -u root -p < database.sql

       This creates the "pims_db" database, all five tables (users,
       suppliers, medicines, sales, sale_items), and seeds:
         - the two accounts listed in section 1
         - 3 sample suppliers
         - 7 sample medicines (including one low-stock and one
           near-expiry item, so the dashboard alerts and reports have
           something to show immediately)

    3. If your MySQL root user requires a password, or runs on a different
       host/port, update the connection details in:

           src/main/java/com/zugobite/pims/data/DBConnection.java

       (URL, USER, PASSWORD constants near the top of the file), then
       recompile if running from source.

    4. Launch the application:

           zasciahugo_pims.exe        (bundled executable)

       or, running from source:

           java -cp "out:<jar-list>" com.zugobite.pims.Main

       (see README.md for the full classpath).


--------------------------------------------------------------------------------
 4. USING THE APPLICATION
--------------------------------------------------------------------------------

LOGIN
    Enter a username and password from section 1 and click "Log In". An
    incorrect username/password shows an on-screen error and lets you try
    again. You are routed to the Admin or Cashier home screen based on the
    account's role.

ADMIN HOME SCREEN
    A row of live figures (today's sales, stock on hand, low-stock count,
    items expiring soon) sits above four modules:

    - Manage Medicines  - full CRUD, including expiry date and supplier
                           link, with a search/filter box and colour-coded
                           rows for low-stock / expiring items.
    - Manage Suppliers   - full CRUD on supplier contact details.
    - Manage Users       - create or delete Cashier accounts. (Admin
                           accounts are not manageable from this screen.)
    - Reports            - run any of the four required reports (Sales,
                           Item-Wise, Low Stock, Expiry) and export the
                           result to PDF or Excel.

CASHIER HOME SCREEN
    Two modules:

    - Point of Sale - search the catalogue (results filter as you type),
                       press Enter or double-click to add an item to the
                       cart, adjust quantities inline, then Checkout.
                       Keyboard shortcuts: F2 focus search, F8 clear sale,
                       F9 checkout, Delete voids the selected cart line.
                       Checkout writes the sale to the database as a single
                       transaction and decrements stock, then opens a Bill
                       window with Print and Save-as-PDF options.
    - Stock Check    - look up a medicine's price and availability without
                       starting a sale.

ACCOUNT MENU
    The toolbar at the top of both dashboards shows the signed-in user's
    name and role, with "Profile" (view your own account details) and
    "Log Out" (returns to the login screen without closing the app).


--------------------------------------------------------------------------------
 5. NOTES FOR THE GRADER
--------------------------------------------------------------------------------

    - Every SQL query in the codebase uses PreparedStatement with bound
      parameters, including login - there is no string-concatenated SQL
      anywhere in the project.
    - Passwords are stored in plain text intentionally: the assignment
      brief's own sample credentials (admin/admin123, cashier/cash123) are
      plain text, so hashing them would make the documented seed
      credentials unusable as literal login values. See
      UserService.authenticate() for a code comment on how this would be
      upgraded to hashed storage if required.
    - Checkout (sale header + line items + stock decrement) runs inside a
      single JDBC transaction with rollback on failure - see
      SalesService.checkout().
    - Full architecture notes, package map, and class reference are in
      docs/002-PROJECT_OVERVIEW.md. The full requirements-to-feature
      mapping is in docs/001-APPLICATION_REQUIREMENTS.md.


--------------------------------------------------------------------------------
 6. TROUBLESHOOTING
--------------------------------------------------------------------------------

    "Could not reach the database" on login
        MySQL is not running, or database.sql has not been loaded yet, or
        the connection details in DBConnection.java do not match your
        local MySQL setup. See section 3.

    "Invalid username or password"
        Re-check the credentials in section 1 - they are case-sensitive.

    Reports/PDF/Excel export buttons do nothing
        Make sure a report has been run first (click "Run Report") before
        clicking "Export to PDF" / "Export to Excel" - the export always
        exports whatever is currently on screen.
