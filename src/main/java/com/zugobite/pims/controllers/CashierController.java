package com.zugobite.pims.controllers;

import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.views.CashierDashboardView;

public class CashierController {

    private final User cashier;
    private final MedicineService medicineService = new MedicineService();
    private final SalesService salesService = new SalesService();

    public CashierController(User cashier) {
        this.cashier = cashier;
    }

    public void start() {
        new CashierDashboardView(cashier, medicineService, salesService).setVisible(true);
    }
}
