package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.views.CashierDashboardView;

/**
 * Wires up the Cashier experience after a successful login: owns the services Cashier's
 * screens need (medicine lookup for POS/Stock Check, sales checkout) and hands them to
 * {@link CashierDashboardView}.
 *
 * <p>Notably absent: {@link com.zugobite.pims.services.SupplierService},
 * {@link com.zugobite.pims.services.UserService}, and
 * {@link com.zugobite.pims.services.ReportService} — a Cashier session simply never
 * constructs them, which is a cheap but effective way of keeping "Cashier UI must not
 * expose add/edit/delete for medicines" (PLAN.md §5.2) true: the Cashier-side views have no
 * service reference that would even let them attempt a medicine CRUD call.</p>
 */
public class CashierController {

    private final User cashier;
    private final Runnable onLogout;
    private final MedicineService medicineService = new MedicineService();
    private final SalesService salesService = new SalesService();

    /**
     * @param cashier  the authenticated Cashier this session belongs to
     * @param onLogout callback wired to the dashboard's "Log Out" menu action (see
     *                 {@link com.zugobite.pims.views.AccountMenuBar}); supplied by
     *                 {@link LoginController} so it can return to the login screen without
     *                 this controller needing to know anything about how login itself works
     */
    public CashierController(User cashier, Runnable onLogout) {
        this.cashier = cashier;
        this.onLogout = onLogout;
    }

    /** Opens the Cashier dashboard window. Called once by {@link LoginController} right after login. */
    public void start() {
        new CashierDashboardView(cashier, medicineService, salesService, onLogout).setVisible(true);
    }
}
