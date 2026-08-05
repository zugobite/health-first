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
    private final MedicineService medicineService = new MedicineService();
    private final SalesService salesService = new SalesService();

    public CashierController(User cashier) {
        this.cashier = cashier;
    }

    /** Opens the Cashier dashboard window. Called once by {@link LoginController} right after login. */
    public void start() {
        new CashierDashboardView(cashier, medicineService, salesService).setVisible(true);
    }
}
