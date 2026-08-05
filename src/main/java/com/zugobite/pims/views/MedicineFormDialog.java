package com.zugobite.pims.views;

import com.zugobite.pims.models.Medicine;
import com.zugobite.pims.models.Supplier;
import com.zugobite.pims.utils.InputValidator;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Modal dialog used to create a brand-new {@link Medicine} or edit an existing one.
 *
 * <p>Mirrors the pattern used by {@link SupplierFormDialog}: a single dialog class handles
 * both "Add" and "Edit" depending on whether an existing {@link Medicine} is passed in, and
 * the caller retrieves the outcome via {@link #getResult()} once the modal dialog closes.</p>
 *
 * <p>The supplier field is a dropdown populated from the list of suppliers already in the
 * database, so a medicine can only ever be linked to a supplier that actually exists
 * (enforced here in the UI, and again by the `supplier_id` foreign key in the schema).</p>
 */
public class MedicineFormDialog extends JDialog {

    private final boolean editing;
    private final int existingMedicineId;

    private final JTextField nameField = new JTextField(22);
    private final JTextField companyField = new JTextField(22);
    private final JTextField medicineTypeField = new JTextField(22);
    private final JTextField priceField = new JTextField(22);
    private final JTextField quantityField = new JTextField(22);
    private final JTextField reorderLevelField = new JTextField(22);
    // Expiry is entered as plain ISO text (yyyy-MM-dd) rather than a date-picker widget,
    // since Swing has no built-in date picker and pulling in a third-party component would
    // be overkill for a single field. InputValidator.isValidDate() parses this same format.
    private final JTextField expiryDateField = new JTextField(22);
    private final JComboBox<Supplier> supplierCombo;

    private Optional<Medicine> result = Optional.empty();

    /**
     * @param owner     parent frame to center on and block input to
     * @param existing  the medicine being edited, or {@code null} for a fresh "Add" form
     * @param suppliers the full supplier list, used to populate the supplier dropdown
     */
    public MedicineFormDialog(Frame owner, Medicine existing, List<Supplier> suppliers) {
        super(owner, existing == null ? "Add Medicine" : "Edit Medicine", true);

        this.editing = existing != null;
        this.existingMedicineId = existing != null ? existing.getMedicineId() : -1;
        this.supplierCombo = new JComboBox<>(suppliers.toArray(new Supplier[0]));

        if (existing != null) {
            nameField.setText(existing.getName());
            companyField.setText(existing.getCompany());
            medicineTypeField.setText(existing.getMedicineType());
            priceField.setText(existing.getPrice().toString());
            quantityField.setText(String.valueOf(existing.getQuantityInStock()));
            reorderLevelField.setText(String.valueOf(existing.getReorderLevel()));
            expiryDateField.setText(existing.getExpiryDate() != null ? existing.getExpiryDate().toString() : "");
            selectSupplierById(existing.getSupplierId());
        }

        setContentPane(buildFormPanel());
        pack();
        setLocationRelativeTo(owner);
    }

    /** Pre-selects the combo box entry whose supplierId matches, if the medicine already has one. */
    private void selectSupplierById(int supplierId) {
        for (int i = 0; i < supplierCombo.getItemCount(); i++) {
            if (supplierCombo.getItemAt(i).getSupplierId() == supplierId) {
                supplierCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        UIUtils.applyStandardPadding(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, gbc, 0, "Name:", nameField);
        addRow(panel, gbc, 1, "Company:", companyField);
        addRow(panel, gbc, 2, "Type (Tablet/Capsule/...):", medicineTypeField);
        addRow(panel, gbc, 3, "Price:", priceField);
        addRow(panel, gbc, 4, "Quantity in Stock:", quantityField);
        addRow(panel, gbc, 5, "Reorder Level:", reorderLevelField);
        addRow(panel, gbc, 6, "Expiry Date (yyyy-mm-dd):", expiryDateField);
        addRow(panel, gbc, 7, "Supplier:", supplierCombo);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(okButton);
        buttonRow.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        panel.add(buttonRow, gbc);

        getRootPane().setDefaultButton(okButton);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    /**
     * Validates every field, and only builds/stores the resulting {@link Medicine} (closing
     * the dialog) if all of them pass. Each failure points at the specific offending field
     * rather than a generic "invalid form" message, since that's faster to fix for the user.
     */
    private void onConfirm() {
        if (!InputValidator.isNonEmpty(nameField.getText())) {
            UIUtils.showError(this, "Medicine name is required.");
            return;
        }
        if (!InputValidator.isPositiveDecimal(priceField.getText())) {
            UIUtils.showError(this, "Price must be a positive number.");
            return;
        }
        if (!InputValidator.isNonNegativeInteger(quantityField.getText())) {
            UIUtils.showError(this, "Quantity in stock must be a non-negative whole number.");
            return;
        }
        if (!InputValidator.isNonNegativeInteger(reorderLevelField.getText())) {
            UIUtils.showError(this, "Reorder level must be a non-negative whole number.");
            return;
        }
        if (!InputValidator.isValidDate(expiryDateField.getText())) {
            UIUtils.showError(this, "Expiry date must be in yyyy-mm-dd format.");
            return;
        }
        Supplier selectedSupplier = (Supplier) supplierCombo.getSelectedItem();
        if (selectedSupplier == null) {
            UIUtils.showError(this, "Select a supplier (add one first if the list is empty).");
            return;
        }

        // medicineId is ignored by MedicineService.addMedicine() and only used for the
        // WHERE clause in updateMedicine(), so -1 is a safe placeholder on the "Add" path.
        Medicine medicine = new Medicine(
                editing ? existingMedicineId : -1,
                nameField.getText().trim(),
                companyField.getText().trim(),
                medicineTypeField.getText().trim(),
                new BigDecimal(priceField.getText().trim()),
                Integer.parseInt(quantityField.getText().trim()),
                Integer.parseInt(reorderLevelField.getText().trim()),
                LocalDate.parse(expiryDateField.getText().trim()),
                selectedSupplier.getSupplierId()
        );

        result = Optional.of(medicine);
        dispose();
    }

    /**
     * @return the medicine the user confirmed, or empty if they cancelled/closed the dialog.
     */
    public Optional<Medicine> getResult() {
        return result;
    }
}
