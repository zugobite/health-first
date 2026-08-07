package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.utils.UIUtils;
import com.zugobite.pims.views.ModuleHomePanel.IconKind;
import com.zugobite.pims.views.ModuleHomePanel.ModuleTile;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;

/**
 * The Cashier's main window: a desktop-native home screen of module tiles, with a top
 * toolbar for Home / Profile / Log Out. Opening a tile swaps the center {@link CardLayout}
 * to Point of Sale or Stock Check. No Manage Medicines/Suppliers/Users/Reports screens
 * exist here at all, which is how "Cashier UI must not expose add/edit/delete for medicines"
 * is enforced: those screens are simply never constructed for a Cashier session.
 */
public class CashierDashboardView extends JFrame {

    private static final List<ModuleTile> MODULES = List.of(
            new ModuleTile("Point of Sale", IconKind.POS, 0),
            new ModuleTile("Stock Check", IconKind.STOCK, 1)
    );

    public CashierDashboardView(User cashier, MedicineService medicineService, SalesService salesService, Runnable onLogout) {
        super("HealthFirst PIMS - Cashier POS (" + cashier.getFullName() + ")");
        // Closing this window via the titlebar button quits the app, same as LoginView -
        // see that class's comment for why. Logging out is the separate, non-quitting path
        // provided by the toolbar's "Log Out" button.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        UIUtils.applyAppIcon(this);

        CardLayout cardLayout = new CardLayout();
        JPanel content = new JPanel(cardLayout);
        content.setBackground(UIUtils.BACKGROUND);
        content.add(new ModuleHomePanel("Cashier", MODULES, index -> cardLayout.show(content, String.valueOf(index))), "home");
        content.add(new PosPanel(cashier, medicineService, salesService), "0");
        content.add(new StockCheckPanel(medicineService), "1");

        JPanel root = new JPanel(new BorderLayout());
        root.add(new DashboardToolbar("Cashier", cashier, () -> cardLayout.show(content, "home"), onLogout), BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        cardLayout.show(content, "home");
    }
}
