package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;

/**
 * The Cashier's main window: a tabbed shell around the two screens PLAN.md §5.2 requires —
 * Point of Sale (search/cart/checkout/bill) and a standalone Stock Check lookup. No Manage
 * Medicines / Suppliers / Users / Reports tabs exist here at all, which is how "Cashier UI
 * must not expose add/edit/delete for medicines" is enforced: those tabs are simply never
 * constructed for a Cashier session.
 */
public class CashierDashboardView extends JFrame {

    /**
     * @param onLogout callback that returns to the login screen; wired into this window's
     *                 {@link AccountMenuBar} "Log Out" action
     */
    public CashierDashboardView(User cashier, MedicineService medicineService, SalesService salesService, Runnable onLogout) {
        super("HealthFirst PIMS — Cashier POS (" + cashier.getFullName() + ")");
        // Closing this window via the titlebar button quits the app, same as LoginView —
        // see that class's comment for why. Logging out is the separate, non-quitting path
        // provided by the "Log Out" item in the menu bar below.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        UIUtils.applyAppIcon(this);

        // Gives the Cashier a way to see who they're logged in as and to log out at shift
        // change — without this, switching users meant quitting and relaunching the app.
        setJMenuBar(new AccountMenuBar(cashier, this, onLogout));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Point of Sale", new PosPanel(cashier, medicineService, salesService));
        tabs.addTab("Stock Check", new StockCheckPanel(medicineService));

        setContentPane(tabs);
    }
}
