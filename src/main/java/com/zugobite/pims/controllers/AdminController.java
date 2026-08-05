package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.views.AdminDashboardView;

public class AdminController {

    private final User admin;
    private final MedicineService medicineService = new MedicineService();
    private final SupplierService supplierService = new SupplierService();
    private final UserService userService = new UserService();
    private final ReportService reportService = new ReportService();

    public AdminController(User admin) {
        this.admin = admin;
    }

    public void start() {
        new AdminDashboardView(admin, medicineService, supplierService, userService, reportService).setVisible(true);
    }
}
