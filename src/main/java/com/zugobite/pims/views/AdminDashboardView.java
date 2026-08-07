package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.utils.UIUtils;
import com.zugobite.pims.views.ModuleHomePanel.IconKind;
import com.zugobite.pims.views.ModuleHomePanel.ModuleTile;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;

/**
 * The Admin's main window: a desktop-native home screen of module tiles, with a top toolbar
 * for Home / Profile / Log Out. Opening a tile swaps the center {@link CardLayout} to that
 * module (Manage Medicines, Manage Suppliers, Manage Users, or Reports). This class itself
 * contains no business logic; it only assembles the screens and hands each one the
 * service(s) it needs, all supplied by {@link com.zugobite.pims.controllers.AdminController}.
 */
public class AdminDashboardView extends JFrame {

    private static final List<ModuleTile> MODULES = List.of(
            new ModuleTile("Manage Medicines", IconKind.MEDICINES, 0),
            new ModuleTile("Manage Suppliers", IconKind.SUPPLIERS, 1),
            new ModuleTile("Manage Users", IconKind.USERS, 2),
            new ModuleTile("Reports", IconKind.REPORTS, 3)
    );

    public AdminDashboardView(User admin, MedicineService medicineService, SupplierService supplierService,
                               UserService userService, ReportService reportService, Runnable onLogout) {
        super("HealthFirst PIMS - Admin Dashboard (" + admin.getFullName() + ")");
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

        content.add(new ModuleHomePanel("Admin", MODULES, index -> cardLayout.show(content, String.valueOf(index))), "home");
        // ManageMedicinesPanel needs SupplierService too, purely to populate the supplier
        // dropdown in its Add/Edit dialog - it never writes to the suppliers table itself.
        content.add(new ManageMedicinesPanel(medicineService, supplierService), "0");
        content.add(new ManageSuppliersPanel(supplierService), "1");
        content.add(new ManageUsersPanel(userService), "2");
        content.add(new ReportsPanel(reportService), "3");

        JPanel root = new JPanel(new BorderLayout());
        root.add(new DashboardToolbar("Admin", admin, () -> cardLayout.show(content, "home"), onLogout), BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        cardLayout.show(content, "home");
    }
}
