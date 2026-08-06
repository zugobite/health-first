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

/**
 * The Cashier's Point of Sale screen — the app's core workflow (PLAN.md §5.2): search for a
 * medicine, add it to a running cart with a quantity, see the live total, and check out.
 *
 * <p>This panel owns the in-progress {@link Sale} object as UI state (the "cart"); it is
 * only handed off to {@link SalesService#checkout} once, at the moment the Cashier clicks
 * "Checkout". Everything before that point — search, add-to-cart, clear-cart — is purely
 * local state with zero database writes, so a Cashier can freely browse and adjust the cart
 * without side effects until they actually commit to the sale.</p>
 */
public class PosPanel extends JPanel {

    private final User cashier;
    private final MedicineService medicineService;
    private final SalesService salesService;

    private final JTextField searchField = new JTextField(20);
    // ID/Price/In Stock are numeric — declaring their real column type right-aligns them via
    // Swing's built-in Number renderer (see ManageMedicinesPanel for the full explanation).
    private final DefaultTableModel searchResultsModel = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "In Stock"}, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 3 -> Integer.class;
                case 2 -> java.math.BigDecimal.class;
                default -> Object.class;
            };
        }
    };
    private final JTable searchResultsTable = new StripedTable(searchResultsModel);

    private final DefaultTableModel cartModel = new DefaultTableModel(new Object[]{"ID", "Name", "Qty", "Price", "Line Total"}, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 2 -> Integer.class;
                case 3, 4 -> java.math.BigDecimal.class;
                default -> Object.class;
            };
        }
    };
    private final JTable cartTable = new StripedTable(cartModel);
    private final JLabel totalLabel = new JLabel("Total: R0.00");

    // The in-progress cart. Reassigned (not mutated in place) by clearCart() — see that
    // method's comment for why a fresh Sale is created rather than emptying the old one.
    private Sale currentSale;

    public PosPanel(User cashier, MedicineService medicineService, SalesService salesService) {
        this.cashier = cashier;
        this.medicineService = medicineService;
        this.salesService = salesService;
        this.currentSale = new Sale(cashier.getUserId());

        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Top card: search box + results table for finding medicines to add to the cart.
        JButton searchButton = new JButton("Search");
        JButton addToCartButton = new JButton("Add to Cart");
        UIUtils.styleAsOutlineButton(searchButton);
        UIUtils.styleAsPrimaryButton(addToCartButton);
        searchButton.addActionListener(e -> search());
        addToCartButton.addActionListener(e -> addSelectedToCart());

        JPanel searchToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchToolbar.setOpaque(false);
        searchToolbar.add(new JLabel("Medicine name:"));
        searchToolbar.add(searchField);
        searchToolbar.add(searchButton);
        searchToolbar.add(addToCartButton);

        CardPanel searchCard = new CardPanel(new BorderLayout(0, 12));
        searchCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        searchCard.add(searchToolbar, BorderLayout.NORTH);
        searchCard.add(new JScrollPane(searchResultsTable), BorderLayout.CENTER);

        // Bottom card: the cart itself, its running total, and the Clear/Checkout actions.
        // The running total is the single most important number on this screen, so it gets
        // a visibly larger, bolder, accent-colored treatment rather than blending in as
        // just another label — the same "make the total impossible to miss" convention
        // real point-of-sale software follows.
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 18f));
        totalLabel.setForeground(UIUtils.ACCENT_COLOR);

        JButton clearButton = new JButton("Clear Cart");
        JButton checkoutButton = new JButton("Checkout");
        UIUtils.styleAsOutlineButton(clearButton);
        UIUtils.styleAsPrimaryButton(checkoutButton);
        clearButton.addActionListener(e -> clearCart());
        checkoutButton.addActionListener(e -> checkout());

        JPanel cartButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cartButtons.setOpaque(false);
        cartButtons.add(clearButton);
        cartButtons.add(checkoutButton);

        JPanel cartFooter = new JPanel(new BorderLayout());
        cartFooter.setOpaque(false);
        cartFooter.add(totalLabel, BorderLayout.WEST);
        cartFooter.add(cartButtons, BorderLayout.EAST);

        CardPanel cartCard = new CardPanel(new BorderLayout(0, 12));
        cartCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        cartCard.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartCard.add(cartFooter, BorderLayout.SOUTH);

        // Split pane lets the Cashier resize search-results vs. cart space to taste.
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchCard, cartCard);
        split.setResizeWeight(0.5);
        split.setOpaque(false);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    /** Partial-match search by name; results populate the top table for the Cashier to pick from. */
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

    /**
     * Prompts for a quantity and adds the selected search result to {@link #currentSale} as
     * a new {@link SaleItem}, capturing the medicine's *current* price at this exact moment
     * (see {@link SaleItem}'s class comment on why that matters). Updates both the cart
     * table and the running total label immediately, so the Cashier always sees an
     * up-to-date total without needing to click anything else.
     */
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

    /**
     * PLAN.md §5.2's "Clear cart" action. Starts a brand-new {@link Sale} rather than
     * emptying {@code currentSale.getItems()} in place, since {@code Sale} has no
     * "removeAllItems" method — its {@code addItem()}-only API keeps the running total
     * always consistent with its item list by construction, so the simplest way to reset
     * both together is to just replace the whole object.
     */
    private void clearCart() {
        currentSale = new Sale(cashier.getUserId());
        cartModel.setRowCount(0);
        totalLabel.setText("Total: R0.00");
    }

    /**
     * Commits the cart: hands {@link #currentSale} to {@link SalesService#checkout} (which
     * persists it as a single DB transaction — sale header, line items, and stock decrement
     * all together), then opens the generated {@link BillView} and resets the cart for the
     * next customer.
     */
    private void checkout() {
        if (currentSale.getItems().isEmpty()) {
            UIUtils.showError(this, "The cart is empty.");
            return;
        }
        try {
            salesService.checkout(currentSale);
            // By this point currentSale.getSaleId() and .getSaleDate() have been populated
            // by checkout(), so the bill can reference the real, persisted sale number and
            // timestamp — and, via `cashier`, name whoever processed it on the PDF receipt.
            new BillView(currentSale, cashier).setVisible(true);
            clearCart();
        } catch (SQLException e) {
            UIUtils.showError(this, "Checkout failed: " + e.getMessage());
        }
    }
}
