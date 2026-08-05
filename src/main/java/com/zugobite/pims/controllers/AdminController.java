package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.views.AdminDashboardView;

/**
 * Wires up the Admin experience after a successful login: owns one instance of every
 * service Admin's screens need (medicines, suppliers, users, reports) and hands them all to
 * {@link AdminDashboardView}, which distributes them across its four tabs.
 *
 * <p>Constructing the services here — rather than inside {@link AdminDashboardView} or its
 * individual tab panels — keeps the views package free of any direct knowledge of how to
 * build a service, and means a future test could swap in mock services just by constructing
 * {@link AdminDashboardView} directly instead of going through this controller.</p>
 */
public class AdminController {

    private final User admin;
    private final MedicineService medicineService = new MedicineService();
    private final SupplierService supplierService = new SupplierService();
    private final UserService userService = new UserService();
    private final ReportService reportService = new ReportService();

    public AdminController(User admin) {
        this.admin = admin;
    }

    /** Opens the Admin dashboard window. Called once by {@link LoginController} right after login. */
    public void start() {
        new AdminDashboardView(admin, medicineService, supplierService, userService, reportService).setVisible(true);
    }
}
