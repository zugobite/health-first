package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;
import com.zugobite.pims.models.User;
import com.zugobite.pims.services.MedicineService;
import com.zugobite.pims.services.SalesService;
import com.zugobite.pims.utils.AppIcons;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Cashier's Point of Sale screen - the app's core workflow (PLAN.md §5.2) and the one
 * screen a cashier spends an entire shift inside. Search for a medicine, add it to a running
 * cart, take payment, check out.
 *
 * <p>The layout is a catalogue on the left and the till on the right, which is how physical
 * point-of-sale terminals are arranged: the thing you search stays put while the thing you
 * are building up grows beside it, both fully visible at once.</p>
 *
 * <h2>Why the interaction model matters here</h2>
 * <p>An earlier revision required six actions to sell one item: type, click Search, click a
 * row, click "Add to Cart", type a quantity into a modal, click OK. At a real dispensary
 * counter that is unusable. This version rings an item up in <em>one</em> action - type part
 * of the name and press Enter - with every other operation available without ever leaving the
 * keyboard:</p>
 * <ul>
 *   <li>Results filter live as you type; no "Search" button to press.</li>
 *   <li>Enter (or double-click) adds the highlighted item at quantity 1.</li>
 *   <li>Adding something already in the cart increments that line instead of creating a
 *       second one - so a customer buying three of an item gets one receipt line, not three.</li>
 *   <li>The cart's Quantity column is edited in place; clearing it to zero voids the line.</li>
 *   <li>F2 focuses search, F8 clears the sale, F9 checks out, Delete voids the selected line.</li>
 * </ul>
 *
 * <h2>Stock is enforced, not merely displayed</h2>
 * <p>Every path that increases a cart quantity is checked against the quantity actually on
 * the shelf ({@link #stockOnHandFor}). Previously the cart would happily accept 100 units of
 * an item with 8 in stock and only fail - or worse, silently drive stock negative - at
 * checkout. Refusing the input at the point of entry is both correct and kinder: the cashier
 * finds out while the customer is still standing there, not after the transaction commits.</p>
 *
 * <p>This panel owns the in-progress {@link Sale} object as UI state (the "cart"); it is only
 * handed to {@link SalesService#checkout} at the moment the Cashier commits. Everything
 * before that - search, add, edit, void, clear - is purely local with zero database writes.</p>
 */
public class PosPanel extends JPanel {

    /** Cart column indices. Quantity is the only user-editable one. */
    private static final int CART_COL_ITEM = 0;
    private static final int CART_COL_QTY = 1;
    private static final int CART_COL_UNIT = 2;
    private static final int CART_COL_LINE_TOTAL = 3;

    private final User cashier;
    private final MedicineService medicineService;
    private final SalesService salesService;

    private final JTextField searchField = new JTextField();
    private final JTextField tenderField = new JTextField();

    /**
     * Backing objects for the rows currently shown in the results table, in display order.
     * Kept alongside the table model so adding to the cart can read a real {@link Medicine}
     * (price, stock, id) instead of parsing values back out of table cells.
     */
    private final List<Medicine> searchResults = new ArrayList<>();

    /**
     * Last-known shelf quantity per medicine id. Accumulates across searches rather than
     * being replaced, so an item added to the cart earlier can still be stock-checked after
     * the cashier has searched for something else and the results table has moved on.
     */
    private final Map<Integer, Integer> stockOnHand = new HashMap<>();

    private final DefaultTableModel searchResultsModel = new DefaultTableModel(
            new Object[]{"Item", "Price", "In Stock"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 1 -> BigDecimal.class;
                case 2 -> Integer.class;
                default -> Object.class;
            };
        }
    };
    private final StripedTable searchResultsTable = new StripedTable(searchResultsModel);

    private final DefaultTableModel cartModel = new DefaultTableModel(
            new Object[]{"Item", "Qty", "Unit", "Line Total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // Quantity only - the item, its unit price, and the computed line total are all
            // either historical fact or derived, and must not be hand-edited on a receipt.
            return column == CART_COL_QTY;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case CART_COL_QTY -> Integer.class;
                case CART_COL_UNIT, CART_COL_LINE_TOTAL -> BigDecimal.class;
                default -> Object.class;
            };
        }
    };
    private final StripedTable cartTable = new StripedTable(cartModel);

    private final JLabel itemCountLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final JLabel changeLabel = new JLabel();
    private final JButton checkoutButton = new JButton("Checkout  (F9)");

    /**
     * Guards {@link #refreshCart()} against its own table-model events. Rebuilding the cart
     * table fires {@code tableChanged} for every row, which the quantity-edit listener would
     * otherwise interpret as user edits and re-apply - a feedback loop.
     */
    private boolean syncingCart;

    /** The in-progress cart. Replaced wholesale by {@link #clearCart()} at the end of a sale. */
    private Sale currentSale;

    public PosPanel(User cashier, MedicineService medicineService, SalesService salesService) {
        this.cashier = cashier;
        this.medicineService = medicineService;
        this.salesService = salesService;
        this.currentSale = new Sale(cashier.getUserId());

        setLayout(new BorderLayout());
        setBackground(UIUtils.MUTED_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCatalogueCard(), buildTillCard());
        // The till needs less width than the catalogue but must never be squeezed out; a
        // 0.58 weight keeps the proportions stable as the window resizes.
        split.setResizeWeight(0.58);
        split.setDividerSize(20);
        split.setOpaque(false);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        installKeyboardShortcuts();
        refreshSearch();
        refreshCart();
    }

    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    /** Left card: the live-filtered medicine catalogue. */
    private JComponent buildCatalogueCard() {
        UIUtils.setPlaceholder(searchField, "Search by medicine name...");
        searchField.putClientProperty("JTextField.leadingIcon",
                AppIcons.lucide(AppIcons.CLIPBOARD, 15, UIUtils.MUTED_FOREGROUND));
        // Live filtering: every keystroke re-queries. The catalogue of a single pharmacy is
        // small and the query is indexed on name, so this is comfortably fast enough to do
        // per-character - and it removes an entire click from the critical path.
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshSearch();
            }
        });
        // Enter anywhere in the search box rings up the highlighted row, so a cashier who
        // knows the stock can work entirely by touch: type, Enter, type, Enter.
        searchField.addActionListener(e -> addHighlightedResultToCart());

        searchResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addHighlightedResultToCart();
                }
            }
        });
        // Rows for items that are out of stock, or already fully committed to the cart, are
        // tinted so the cashier can see there is nothing left to add before trying.
        searchResultsTable.setRowAccent(modelRow -> {
            if (modelRow < 0 || modelRow >= searchResults.size()) {
                return null;
            }
            Medicine medicine = searchResults.get(modelRow);
            return remainingStockFor(medicine.getMedicineId()) <= 0
                    ? new java.awt.Color(0xFE, 0xF2, 0xF2)
                    : null;
        });

        JButton addButton = new JButton("Add to Cart");
        UIUtils.styleAsPrimaryButton(addButton);
        addButton.addActionListener(e -> addHighlightedResultToCart());

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchField, BorderLayout.CENTER);
        toolbar.add(addButton, BorderLayout.EAST);

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(sectionHeader("Catalogue", "Type to filter, then press Enter to add"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(UIUtils.createRoundedScrollPane(searchResultsTable), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Right card: the cart, the totals, cash tendered, and the commit actions. */
    private JComponent buildTillCard() {
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Applies inline quantity edits back onto the Sale. Runs for user edits only - see
        // the syncingCart guard's field comment.
        cartModel.addTableModelListener(e -> {
            if (syncingCart || e.getType() != TableModelEvent.UPDATE || e.getColumn() != CART_COL_QTY) {
                return;
            }
            applyQuantityEdit(e.getFirstRow());
        });

        CardPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(sectionHeader("Current Sale", "Served by " + cashier.getFullName()), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(UIUtils.createRoundedScrollPane(cartTable), BorderLayout.CENTER);
        body.add(buildTillFooter(), BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Line-level controls, the totals block, the tender field, and the commit buttons. */
    private JPanel buildTillFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);

        footer.add(buildLineControls());
        footer.add(Box.createVerticalStrut(12));
        footer.add(horizontalRule());
        footer.add(Box.createVerticalStrut(12));
        footer.add(buildTotalsBlock());
        footer.add(Box.createVerticalStrut(12));
        footer.add(buildPaymentBlock());
        footer.add(Box.createVerticalStrut(14));
        footer.add(buildCommitButtons());
        return footer;
    }

    /** Per-line quantity nudges and void, operating on the selected cart row. */
    private JPanel buildLineControls() {
        JButton decreaseButton = new JButton("-");
        JButton increaseButton = new JButton("+");
        JButton voidButton = new JButton("Void Line");
        UIUtils.styleAsOutlineButton(decreaseButton);
        UIUtils.styleAsOutlineButton(increaseButton);
        UIUtils.styleAsDestructiveButton(voidButton);

        decreaseButton.addActionListener(e -> nudgeSelectedLine(-1));
        increaseButton.addActionListener(e -> nudgeSelectedLine(1));
        voidButton.addActionListener(e -> voidSelectedLine());

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        itemCountLabel.setFont(itemCountLabel.getFont().deriveFont(Font.PLAIN, 12f));
        itemCountLabel.setForeground(UIUtils.MUTED_FOREGROUND);

        row.add(itemCountLabel);
        row.add(Box.createHorizontalGlue());
        row.add(decreaseButton);
        row.add(Box.createRigidArea(new Dimension(6, 0)));
        row.add(increaseButton);
        row.add(Box.createRigidArea(new Dimension(6, 0)));
        row.add(voidButton);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /** The amount due - by far the most important number on the screen. */
    private JPanel buildTotalsBlock() {
        JLabel caption = new JLabel("TOTAL DUE");
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 11f));
        caption.setForeground(UIUtils.MUTED_FOREGROUND);

        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 30f));
        totalLabel.setForeground(UIUtils.PRIMARY);

        JPanel block = new JPanel(new BorderLayout());
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(caption, BorderLayout.NORTH);
        block.add(totalLabel, BorderLayout.CENTER);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height));
        return block;
    }

    /**
     * Cash tendered and the change owed back. Optional by design: leaving the field blank
     * means "exact amount", which is the common case for card payment. When it <em>is</em>
     * filled in, the change is computed live - a cashier should never be doing subtraction in
     * their head with a queue waiting.
     */
    private JPanel buildPaymentBlock() {
        UIUtils.setPlaceholder(tenderField, "Cash tendered (optional)");
        tenderField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshChangeDue();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshChangeDue();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshChangeDue();
            }
        });

        changeLabel.setFont(changeLabel.getFont().deriveFont(Font.BOLD, 13f));
        changeLabel.setForeground(UIUtils.MUTED_FOREGROUND);

        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(tenderField, BorderLayout.NORTH);
        block.add(changeLabel, BorderLayout.CENTER);
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, block.getPreferredSize().height + 6));
        return block;
    }

    /** Clear-sale and checkout, with their shortcut keys spelled out on the labels. */
    private JPanel buildCommitButtons() {
        JButton clearButton = new JButton("Clear  (F8)");
        UIUtils.styleAsOutlineButton(clearButton);
        clearButton.addActionListener(e -> clearCart());

        UIUtils.styleAsPrimaryButton(checkoutButton);
        checkoutButton.addActionListener(e -> checkout());
        checkoutButton.setPreferredSize(new Dimension(0, 40));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(clearButton, BorderLayout.WEST);
        row.add(checkoutButton, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return row;
    }

    /** A card's title and one-line subtitle. */
    private JPanel sectionHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
        titleLabel.setForeground(UIUtils.FOREGROUND);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        subtitleLabel.setForeground(UIUtils.MUTED_FOREGROUND);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    /** A hairline divider between footer sections. */
    private JComponent horizontalRule() {
        JPanel rule = new JPanel();
        rule.setBackground(UIUtils.BORDER_COLOR);
        rule.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        return rule;
    }

    /**
     * Binds the function keys a till operator expects. Registered on the panel's
     * {@code WHEN_IN_FOCUSED_WINDOW} input map so they fire regardless of which field
     * currently holds focus - the entire point of a shortcut at a busy counter.
     */
    private void installKeyboardShortcuts() {
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusSearch", () -> {
            searchField.requestFocusInWindow();
            searchField.selectAll();
        });
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "clearCart", this::clearCart);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "checkout", this::checkout);
        // Delete only voids a line while the cart itself has focus, so pressing Delete to
        // correct a typo in the search box cannot silently destroy a cart line.
        cartTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "voidLine");
        cartTable.getActionMap().put("voidLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                voidSelectedLine();
            }
        });
    }

    private void bindKey(KeyStroke keyStroke, String actionName, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Catalogue
    // ---------------------------------------------------------------------

    /**
     * Re-runs the name filter and repopulates the results table. An empty query lists the
     * whole catalogue rather than nothing, so the screen is useful the instant it opens
     * instead of showing an empty table until the cashier types.
     */
    private void refreshSearch() {
        try {
            List<Medicine> results = medicineService.searchByName(searchField.getText().trim());
            searchResults.clear();
            searchResults.addAll(results);
            searchResultsModel.setRowCount(0);
            for (Medicine medicine : results) {
                // Remember the shelf quantity even after this row scrolls out of the
                // results - the cart may still hold this item and need stock-checking.
                stockOnHand.put(medicine.getMedicineId(), medicine.getQuantityInStock());
                searchResultsModel.addRow(new Object[]{
                        medicine.getName(), medicine.getPrice(), medicine.getQuantityInStock()});
            }
            if (!results.isEmpty()) {
                // Pre-select the top hit so Enter has something to act on without the
                // cashier reaching for the mouse or arrow keys.
                searchResultsTable.setRowSelectionInterval(0, 0);
            }
        } catch (SQLException e) {
            UIUtils.showError(this, "Failed to search medicines: " + e.getMessage());
        }
    }

    /** Adds the selected result (or the top hit when nothing is selected) to the cart. */
    private void addHighlightedResultToCart() {
        if (searchResults.isEmpty()) {
            return;
        }
        int viewRow = searchResultsTable.getSelectedRow();
        int modelRow = viewRow < 0 ? 0 : searchResultsTable.convertRowIndexToModel(viewRow);
        addToCart(searchResults.get(modelRow));
    }

    // ---------------------------------------------------------------------
    // Cart
    // ---------------------------------------------------------------------

    /**
     * Adds one unit of {@code medicine} to the cart, merging into the existing line when that
     * medicine is already present. Refuses to exceed the quantity on the shelf.
     */
    private void addToCart(Medicine medicine) {
        int existingIndex = currentSale.indexOfMedicine(medicine.getMedicineId());
        int currentQuantity = existingIndex < 0 ? 0 : currentSale.getItems().get(existingIndex).getQuantitySold();

        if (currentQuantity + 1 > stockOnHandFor(medicine.getMedicineId())) {
            UIUtils.showError(this, "Only " + stockOnHandFor(medicine.getMedicineId())
                    + " unit(s) of \"" + medicine.getName() + "\" are in stock.");
            return;
        }

        if (existingIndex >= 0) {
            currentSale.setItemQuantity(existingIndex, currentQuantity + 1);
        } else {
            // Price is captured now, at the moment of sale - see SaleItem's class comment for
            // why the receipt must not re-read a price that could change later.
            currentSale.addItem(new SaleItem(
                    medicine.getMedicineId(), medicine.getName(), 1, medicine.getPrice()));
        }
        refreshCart();
    }

    /**
     * Applies an inline Quantity edit from the cart table back onto the {@link Sale},
     * rejecting anything that is not a positive whole number within available stock. On
     * rejection the cart is simply re-rendered from the Sale, which restores the previous
     * value - the model stays the single source of truth, so a bad edit cannot linger on
     * screen looking accepted.
     */
    private void applyQuantityEdit(int row) {
        if (row < 0 || row >= currentSale.getItems().size()) {
            return;
        }
        SaleItem item = currentSale.getItems().get(row);
        Object rawValue = cartModel.getValueAt(row, CART_COL_QTY);
        int requested;
        try {
            requested = rawValue instanceof Integer intValue ? intValue : Integer.parseInt(String.valueOf(rawValue).trim());
        } catch (NumberFormatException e) {
            UIUtils.showError(this, "Quantity must be a whole number.");
            refreshCart();
            return;
        }

        if (requested > stockOnHandFor(item.getMedicineId())) {
            UIUtils.showError(this, "Only " + stockOnHandFor(item.getMedicineId())
                    + " unit(s) of \"" + item.getMedicineName() + "\" are in stock.");
            refreshCart();
            return;
        }

        currentSale.setItemQuantity(row, requested);
        refreshCart();
    }

    /** Steps the selected line's quantity by {@code delta}, clamped to stock and to zero. */
    private void nudgeSelectedLine(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        SaleItem item = currentSale.getItems().get(row);
        int target = item.getQuantitySold() + delta;
        if (target > stockOnHandFor(item.getMedicineId())) {
            UIUtils.showError(this, "Only " + stockOnHandFor(item.getMedicineId())
                    + " unit(s) of \"" + item.getMedicineName() + "\" are in stock.");
            return;
        }
        currentSale.setItemQuantity(row, target);
        refreshCart();
        // Keep the same line selected so repeated taps on "+" keep operating on it.
        if (row < cartModel.getRowCount()) {
            cartTable.setRowSelectionInterval(row, row);
        }
    }

    /** Removes the selected line from the sale entirely. */
    private void voidSelectedLine() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        currentSale.removeItem(row);
        refreshCart();
    }

    /**
     * Rebuilds the cart table and every derived figure from {@link #currentSale}. One-way
     * render from the model rather than incremental patching, so the table can never drift
     * out of step with the sale it is supposed to be showing.
     */
    private void refreshCart() {
        syncingCart = true;
        try {
            cartModel.setRowCount(0);
            for (SaleItem item : currentSale.getItems()) {
                cartModel.addRow(new Object[]{
                        item.getMedicineName(), item.getQuantitySold(), item.getPriceAtSale(), item.getLineTotal()});
            }
        } finally {
            syncingCart = false;
        }

        int lines = currentSale.getItems().size();
        int units = currentSale.getTotalQuantity();
        itemCountLabel.setText(lines == 0
                ? "Cart is empty"
                : lines + (lines == 1 ? " line" : " lines") + " - " + units + (units == 1 ? " unit" : " units"));

        totalLabel.setText(UIUtils.formatCurrency(currentSale.getTotalAmount()));
        checkoutButton.setEnabled(lines > 0);
        refreshChangeDue();
        // Search rows tint by remaining stock, which every cart change can alter.
        searchResultsTable.repaint();
    }

    /**
     * Updates the change-due readout from the tender field. Shows nothing when the field is
     * blank (the "exact amount" case), a muted shortfall when the tender is too low, and the
     * change owed in brand teal once it covers the total.
     */
    private void refreshChangeDue() {
        String raw = tenderField.getText().trim();
        if (raw.isEmpty()) {
            changeLabel.setText(" ");
            return;
        }
        BigDecimal tendered;
        try {
            tendered = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            changeLabel.setText("Enter a valid cash amount");
            changeLabel.setForeground(UIUtils.DESTRUCTIVE);
            return;
        }
        BigDecimal difference = tendered.subtract(currentSale.getTotalAmount());
        if (difference.signum() < 0) {
            changeLabel.setText("Short by " + UIUtils.formatCurrency(difference.negate()));
            changeLabel.setForeground(UIUtils.DESTRUCTIVE);
        } else {
            changeLabel.setText("Change due: " + UIUtils.formatCurrency(difference));
            changeLabel.setForeground(UIUtils.PRIMARY);
        }
    }

    /** Shelf quantity for a medicine, defaulting to zero when we have never seen it. */
    private int stockOnHandFor(int medicineId) {
        return stockOnHand.getOrDefault(medicineId, 0);
    }

    /** Shelf quantity minus whatever this in-progress sale has already claimed. */
    private int remainingStockFor(int medicineId) {
        int index = currentSale.indexOfMedicine(medicineId);
        int claimed = index < 0 ? 0 : currentSale.getItems().get(index).getQuantitySold();
        return stockOnHandFor(medicineId) - claimed;
    }

    /**
     * PLAN.md §5.2's "Clear cart" action. Starts a brand-new {@link Sale} rather than emptying
     * the existing one, which also resets the tender field - a new customer's cash should
     * never be pre-filled from the previous sale.
     */
    private void clearCart() {
        currentSale = new Sale(cashier.getUserId());
        tenderField.setText("");
        refreshCart();
    }

    // ---------------------------------------------------------------------
    // Checkout
    // ---------------------------------------------------------------------

    /**
     * Commits the cart: hands {@link #currentSale} to {@link SalesService#checkout} (which
     * persists it as a single DB transaction - sale header, line items, and stock decrement
     * all together), opens the generated {@link BillView}, then resets for the next customer.
     */
    private void checkout() {
        if (currentSale.getItems().isEmpty()) {
            UIUtils.showError(this, "Add at least one item before checking out.");
            return;
        }
        // Block an under-payment before it reaches the database. Only enforced when the
        // cashier actually entered a tender - a blank field means card/exact payment.
        String tenderText = tenderField.getText().trim();
        if (!tenderText.isEmpty()) {
            try {
                if (new BigDecimal(tenderText).compareTo(currentSale.getTotalAmount()) < 0) {
                    UIUtils.showError(this, "Cash tendered is less than the total due.");
                    return;
                }
            } catch (NumberFormatException e) {
                UIUtils.showError(this, "Cash tendered is not a valid amount.");
                return;
            }
        }

        try {
            Sale completed = currentSale;
            salesService.checkout(completed);
            // By this point the sale's id and date have been populated by checkout(), so the
            // bill can reference the real, persisted sale number and timestamp - and, via
            // `cashier`, name whoever processed it on the PDF receipt.
            new BillView(completed, cashier).setVisible(true);
            clearCart();
            // Stock on the shelf just changed; re-query so the catalogue and every subsequent
            // stock check reflect post-sale quantities rather than stale pre-sale ones.
            refreshSearch();
        } catch (SQLException e) {
            UIUtils.showError(this, "Checkout failed: " + e.getMessage());
        }
    }
}
