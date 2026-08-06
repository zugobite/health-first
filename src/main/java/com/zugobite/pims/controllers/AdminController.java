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
 * {@link AdminDashboardView}, which distributes them across its four sidebar screens.
 *
 * <p>Constructing the services here — rather than inside {@link AdminDashboardView} or its
 * individual tab panels — keeps the views package free of any direct knowledge of how to
 * build a service, and means a future test could swap in mock services just by constructing
 * {@link AdminDashboardView} directly instead of going through this controller.</p>
 */
public class AdminController {

    private final User admin;
    private final Runnable onLogout;
    private final MedicineService medicineService = new MedicineService();
    private final SupplierService supplierService = new SupplierService();
    private final UserService userService = new UserService();
    private final ReportService reportService = new ReportService();

    /**
     * @param admin    the authenticated Admin this session belongs to
     * @param onLogout callback wired to the dashboard sidebar's "Log Out" button (see
     *                 {@link com.zugobite.pims.views.SidebarNav}); supplied by
     *                 {@link LoginController} so it can return to the login screen without
     *                 this controller needing to know anything about how login itself works
     */
    public AdminController(User admin, Runnable onLogout) {
        this.admin = admin;
        this.onLogout = onLogout;
    }

    /** Opens the Admin dashboard window. Called once by {@link LoginController} right after login. */
    public void start() {
        new AdminDashboardView(admin, medicineService, supplierService, userService, reportService, onLogout).setVisible(true);
    }
}
