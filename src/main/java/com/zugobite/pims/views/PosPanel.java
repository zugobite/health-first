package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;
import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.utils.InputValidator;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class PosPanel extends JPanel {

    private final User cashier;
    private final MedicineService medicineService;
    private final SalesService salesService;

    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel searchResultsModel = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "In Stock"}, 0);
    private final JTable searchResultsTable = new JTable(searchResultsModel);

    private final DefaultTableModel cartModel = new DefaultTableModel(new Object[]{"ID", "Name", "Qty", "Price", "Line Total"}, 0);
    private final JTable cartTable = new JTable(cartModel);
    private final JLabel totalLabel = new JLabel("Total: R0.00");

    private Sale currentSale;

    public PosPanel(User cashier, MedicineService medicineService, SalesService salesService) {
        this.cashier = cashier;
        this.medicineService = medicineService;
        this.salesService = salesService;
        this.currentSale = new Sale(cashier.getUserId());

        setLayout(new BorderLayout(8, 8));
        UIUtils.applyStandardPadding(this);

        JPanel searchPanel = new JPanel(new BorderLayout(4, 4));
        JPanel searchTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton addToCartButton = new JButton("Add to Cart");
        searchButton.addActionListener(e -> search());
        addToCartButton.addActionListener(e -> addSelectedToCart());
        searchTop.add(new JLabel("Medicine name:"));
        searchTop.add(searchField);
        searchTop.add(searchButton);
        searchTop.add(addToCartButton);
        searchPanel.add(searchTop, BorderLayout.NORTH);
        searchPanel.add(new JScrollPane(searchResultsTable), BorderLayout.CENTER);

        JPanel cartPanel = new JPanel(new BorderLayout(4, 4));
        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel cartFooter = new JPanel(new BorderLayout());
        cartFooter.add(totalLabel, BorderLayout.WEST);
        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Cart");
        JButton checkoutButton = new JButton("Checkout");
        clearButton.addActionListener(e -> clearCart());
        checkoutButton.addActionListener(e -> checkout());
        cartButtons.add(clearButton);
        cartButtons.add(checkoutButton);
        cartFooter.add(cartButtons, BorderLayout.EAST);
        cartPanel.add(cartFooter, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPanel, cartPanel);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    private void search() {
        try {
            List<Medicine> results = medicineService.searchByName(searchField.getText().trim());
            searchResultsModel.setRowCount(0);
            for (Medicine m : results) {
                searchResultsModel.addRow(new Object[]{m.getMedicineId(), m.getName(), m.getPrice(), m.getQuantityInStock()});
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to search medicines: " + e.getMessage());
        }
    }

    private void addSelectedToCart() {
        int row = searchResultsTable.getSelectedRow();
        if (row < 0) {
            UIUtils.showError(this, "Select a medicine to add to the cart.");
            return;
        }
        String qtyInput = JOptionPane.showInputDialog(this, "Quantity:", "1");
        if (!InputValidator.isNonNegativeInteger(qtyInput) || Integer.parseInt(qtyInput) <= 0) {
            UIUtils.showError(this, "Enter a valid positive quantity.");
            return;
        }
        int medicineId = (int) searchResultsModel.getValueAt(row, 0);
        String name = (String) searchResultsModel.getValueAt(row, 1);
        java.math.BigDecimal price = (java.math.BigDecimal) searchResultsModel.getValueAt(row, 2);
        int quantity = Integer.parseInt(qtyInput);

        SaleItem item = new SaleItem(medicineId, name, quantity, price);
        currentSale.addItem(item);
        cartModel.addRow(new Object[]{medicineId, name, quantity, price, item.getLineTotal()});
        totalLabel.setText("Total: R" + currentSale.getTotalAmount());
    }

    private void clearCart() {
        currentSale = new Sale(cashier.getUserId());
        cartModel.setRowCount(0);
        totalLabel.setText("Total: R0.00");
    }

    private void checkout() {
        if (currentSale.getItems().isEmpty()) {
            UIUtils.showError(this, "The cart is empty.");
            return;
        }
        try {
            salesService.checkout(currentSale);
            new BillView(currentSale).setVisible(true);
            clearCart();
        } catch (SQLException e) {
            UIUtils.showError(this, "Checkout failed: " + e.getMessage());
        }
    }
}
