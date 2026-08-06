package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.ReportService;
import com.zugobite.pims.services.SupplierService;
import com.zugobite.pims.services.UserService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;

/**
 * The Admin's main window: a left sidebar (see {@link SidebarNav}) for switching between the
 * four screens PLAN.md §5.3 requires — Manage Medicines, Manage Suppliers, Manage Users, and
 * Reports — with the currently-selected one shown in a {@link CardLayout} content area on
 * the right. This class itself contains no business logic; it only assembles the screens and
 * hands each one the specific service(s) it needs, all supplied by
 * {@link com.zugobite.pims.controllers.AdminController}.
 *
 * <p>Uses a sidebar instead of the {@link JTabbedPane} this app used previously — a vertical
 * nav rail with the account/logout controls anchored at the bottom is the standard shape of
 * modern admin dashboards, and reads as considerably more "product-like" than a row of tabs
 * across the top.</p>
 */
public class AdminDashboardView extends JFrame {

    // Card names are just the nav index as a string — CardLayout requires a String key, and
    // there's no more meaningful name needed than "which position in the sidebar is this".
    private static final List<String> NAV_LABELS = List.of("Manage Medicines", "Manage Suppliers", "Manage Users", "Reports");

    public AdminDashboardView(User admin, MedicineService medicineService, SupplierService supplierService,
                               UserService userService, ReportService reportService, Runnable onLogout) {
        super("HealthFirst PIMS — Admin Dashboard (" + admin.getFullName() + ")");
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

        // ManageMedicinesPanel needs SupplierService too, purely to populate the supplier
        // dropdown in its Add/Edit dialog — it never writes to the suppliers table itself.
        content.add(new ManageMedicinesPanel(medicineService, supplierService), "0");
        content.add(new ManageSuppliersPanel(supplierService), "1");
        content.add(new ManageUsersPanel(userService), "2");
        content.add(new ReportsPanel(reportService), "3");

        SidebarNav sidebar = new SidebarNav("Admin", NAV_LABELS,
                index -> cardLayout.show(content, String.valueOf(index)), admin, onLogout);

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }
}
