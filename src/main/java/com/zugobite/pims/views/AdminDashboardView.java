package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;

/**
 * The Admin's main window: a tabbed shell around the four screens PLAN.md §5.3 requires —
 * Manage Medicines, Manage Suppliers, Manage Users, and Reports. This class itself contains
 * no business logic; it only assembles the tabs and hands each one the specific service(s)
 * it needs, all supplied by {@link com.zugobite.pims.controllers.AdminController}.
 */
public class AdminDashboardView extends JFrame {

    /**
     * @param onLogout callback that returns to the login screen; wired into this window's
     *                 {@link AccountMenuBar} "Log Out" action
     */
    public AdminDashboardView(User admin, MedicineService medicineService, SupplierService supplierService,
                               UserService userService, ReportService reportService, Runnable onLogout) {
        super("HealthFirst PIMS — Admin Dashboard (" + admin.getFullName() + ")");
        // Closing this window via the titlebar button quits the app, same as LoginView —
        // see that class's comment for why. Logging out is the separate, non-quitting path
        // provided by the "Log Out" item in the menu bar below.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        UIUtils.applyAppIcon(this);

        // Gives the Admin a way to see who they're logged in as and to log out — without
        // this, the only way to switch users was to quit and relaunch the whole app.
        setJMenuBar(new AccountMenuBar(admin, this, onLogout));

        JTabbedPane tabs = new JTabbedPane();
        // ManageMedicinesPanel needs SupplierService too, purely to populate the supplier
        // dropdown in its Add/Edit dialog — it never writes to the suppliers table itself.
        tabs.addTab("Manage Medicines", new ManageMedicinesPanel(medicineService, supplierService));
        tabs.addTab("Manage Suppliers", new ManageSuppliersPanel(supplierService));
        tabs.addTab("Manage Users", new ManageUsersPanel(userService));
        tabs.addTab("Reports", new ReportsPanel(reportService));

        setContentPane(tabs);
    }
}
