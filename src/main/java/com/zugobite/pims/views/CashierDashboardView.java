package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;

/**
 * The Cashier's main window: a left sidebar (see {@link SidebarNav}) for switching between
 * the two screens PLAN.md §5.2 requires — Point of Sale (search/cart/checkout/bill) and a
 * standalone Stock Check lookup — with the selected one shown in a {@link CardLayout}
 * content area. No Manage Medicines/Suppliers/Users/Reports screens exist here at all, which
 * is how "Cashier UI must not expose add/edit/delete for medicines" is enforced: those
 * screens are simply never constructed for a Cashier session.
 */
public class CashierDashboardView extends JFrame {

    private static final List<String> NAV_LABELS = List.of("Point of Sale", "Stock Check");

    public CashierDashboardView(User cashier, MedicineService medicineService, SalesService salesService, Runnable onLogout) {
        super("HealthFirst PIMS — Cashier POS (" + cashier.getFullName() + ")");
        // Closing this window via the titlebar button quits the app, same as LoginView —
        // see that class's comment for why. Logging out is the separate, non-quitting path
        // provided by the sidebar's "Log Out" button (see SidebarNav).
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        UIUtils.applyAppIcon(this);

        CardLayout cardLayout = new CardLayout();
        JPanel content = new JPanel(cardLayout);
        content.setBackground(UIUtils.BACKGROUND);
        content.add(new PosPanel(cashier, medicineService, salesService), "0");
        content.add(new StockCheckPanel(medicineService), "1");

        SidebarNav sidebar = new SidebarNav("Cashier", NAV_LABELS,
                index -> cardLayout.show(content, String.valueOf(index)), cashier, onLogout);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }
}
