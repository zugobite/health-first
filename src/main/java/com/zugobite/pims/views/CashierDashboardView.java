package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;

import javax.swing.*;

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
