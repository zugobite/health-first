package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;

import javax.swing.*;

/**
 * The Admin's main window: a tabbed shell around the four screens PLAN.md §5.3 requires —
 * Manage Medicines, Manage Suppliers, Manage Users, and Reports. This class itself contains
 * no business logic; it only assembles the tabs and hands each one the specific service(s)
 * it needs, all supplied by {@link com.zugobite.pims.controllers.AdminController}.
 */
public class AdminDashboardView extends JFrame {

    public AdminDashboardView(User admin, MedicineService medicineService, SupplierService supplierService,
                               UserService userService, ReportService reportService) {
        super("HealthFirst PIMS — Admin Dashboard (" + admin.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

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
