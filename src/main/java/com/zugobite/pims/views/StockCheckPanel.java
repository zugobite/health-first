package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Cashier's "look up a medicine's price/availability without starting a sale" screen
 * (PLAN.md §5.2). Deliberately separate from {@link PosPanel}'s own search box - a Cashier
 * answering a customer's "do you have X, how much is it?" question shouldn't need to touch
 * an in-progress cart to do it, and this tab makes that a read-only, side-effect-free action.
 */
public class StockCheckPanel extends JPanel {

    private final MedicineService medicineService;
    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Name", "Type", "Price", "In Stock"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        // Price/In Stock are numeric - declaring their real column type right-aligns them
        // via Swing's built-in Number renderer (see ManageMedicinesPanel for the explanation).
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 2 -> java.math.BigDecimal.class;
                case 3 -> Integer.class;
                default -> Object.class;
            };
        }
    };
    private final JTable table = new StripedTable(tableModel);

    public StockCheckPanel(MedicineService medicineService) {
        this.medicineService = medicineService;
        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JButton searchButton = new JButton("Search");
        UIUtils.styleAsPrimaryButton(searchButton);
        searchButton.addActionListener(e -> search());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Medicine Name"));
        UIUtils.setPlaceholder(searchField, "Medicine Name");
        toolbar.add(searchField);
        toolbar.add(searchButton);

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(UIUtils.createRoundedScrollPane(table), BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    /** Runs a partial-match search and repopulates the results table - read-only, no cart involved. */
    private void search() {
        try {
            List<Medicine> results = medicineService.searchByName(searchField.getText().trim());
            tableModel.setRowCount(0);
            for (Medicine m : results) {
                tableModel.addRow(new Object[]{m.getName(), m.getMedicineType(), m.getPrice(), m.getQuantityInStock()});
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to search medicines: " + e.getMessage());
        }
    }
}
