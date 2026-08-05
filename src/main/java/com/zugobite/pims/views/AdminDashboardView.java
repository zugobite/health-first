package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;

import javax.swing.*;

public class AdminDashboardView extends JFrame {

    public AdminDashboardView(User admin, MedicineService medicineService, SupplierService supplierService,
                               UserService userService, ReportService reportService) {
        super("HealthFirst PIMS — Admin Dashboard (" + admin.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Medicines", new ManageMedicinesPanel(medicineService));
        tabs.addTab("Manage Suppliers", new ManageSuppliersPanel(supplierService));
        tabs.addTab("Manage Users", new ManageUsersPanel(userService));
        tabs.addTab("Reports", new ReportsPanel(reportService));

        setContentPane(tabs);
    }
}
