package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;

import javax.swing.*;

/**
 * The Cashier's main window: a tabbed shell around the two screens PLAN.md §5.2 requires —
 * Point of Sale (search/cart/checkout/bill) and a standalone Stock Check lookup. No Manage
 * Medicines / Suppliers / Users / Reports tabs exist here at all, which is how "Cashier UI
 * must not expose add/edit/delete for medicines" is enforced: those tabs are simply never
 * constructed for a Cashier session.
 */
public class CashierDashboardView extends JFrame {

    public CashierDashboardView(User cashier, MedicineService medicineService, SalesService salesService) {
        super("HealthFirst PIMS — Cashier POS (" + cashier.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Point of Sale", new PosPanel(cashier, medicineService, salesService));
        tabs.addTab("Stock Check", new StockCheckPanel(medicineService));

        setContentPane(tabs);
    }
}
