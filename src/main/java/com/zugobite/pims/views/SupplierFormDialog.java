package com.zugobite.pims.views;

import com.zugobite.pims.models.Supplier;
import com.zugobite.pims.utils.InputValidator;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * Modal dialog used to create a brand-new {@link Supplier} or edit an existing one.
 *
 * <p>The dialog is reused for both "Add" and "Edit" flows in {@link ManageSuppliersPanel}:
 * pass {@code existing == null} to get a blank "Add Supplier" form, or pass an existing
 * {@link Supplier} to pre-fill the fields for editing. Either way, the caller reads the
 * outcome back via {@link #getResult()} *after* the dialog has been closed (the dialog is
 * modal, so control only returns to the caller once the user has clicked OK/Cancel or
 * closed the window).</p>
 */
public class SupplierFormDialog extends JDialog {

    // Whether the existing supplier is being edited (true) or a new one is being created (false).
    // Used only to decide whether we need to carry the original supplierId through to the result.
    private final boolean editing;
    private final int existingSupplierId;

    // Form fields. Kept as instance fields so the "OK" button's listener can read their
    // current values when the user confirms the form.
    private final JTextField nameField = new JTextField(24);
    private final JTextField contactPersonField = new JTextField(24);
    private final JTextField phoneField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JTextArea addressArea = new JTextArea(3, 24);

    // The dialog's outcome. Empty until the user successfully confirms the form with valid
    // input; stays empty forever if the user cancels or closes the window.
    private Optional<Supplier> result = Optional.empty();

    /**
     * @param owner    the parent frame the dialog should center itself on and block input to
     * @param existing the supplier being edited, or {@code null} to build a fresh "Add" form
     */
    public SupplierFormDialog(Frame owner, Supplier existing) {
        // Modal so that callers can safely call setVisible(true) and then immediately read
        // getResult() on the next line, knowing the user has already finished interacting.
        super(owner, existing == null ? "Add Supplier" : "Edit Supplier", true);

        this.editing = existing != null;
        this.existingSupplierId = existing != null ? existing.getSupplierId() : -1;

        if (existing != null) {
            // Pre-fill every field with the current values so the user is editing, not
            // retyping, the record.
            nameField.setText(existing.getName());
            contactPersonField.setText(existing.getContactPerson());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            addressArea.setText(existing.getAddress());
        }

        setContentPane(buildFormPanel());
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Lays out the labeled fields plus the OK/Cancel buttons using GridBagLayout so labels
     * and inputs stay aligned even as field widths vary.
     */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        UIUtils.applyStandardPadding(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, gbc, 0, "Name:", nameField);
        addRow(panel, gbc, 1, "Contact Person:", contactPersonField);
        addRow(panel, gbc, 2, "Phone:", phoneField);
        addRow(panel, gbc, 3, "Email:", emailField);
        addRow(panel, gbc, 4, "Address:", new JScrollPane(addressArea));

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        UIUtils.styleAsPrimaryButton(okButton);
        UIUtils.styleAsOutlineButton(cancelButton);
        okButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(okButton);
        buttonRow.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(buttonRow, gbc);

        getRootPane().setDefaultButton(okButton);
        return panel;
    }

    /** Adds one "label: input" row to the form grid at the given row index. */
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
     * Validates the form, and if everything checks out, builds the resulting {@link Supplier}
     * and closes the dialog. On validation failure, the dialog stays open and shows an error
     * so the user can correct the offending field.
     */
    private void onConfirm() {
        if (!InputValidator.isNonEmpty(nameField.getText())) {
            UIUtils.showError(this, "Supplier name is required.");
            return;
        }

        // supplierId is ignored by SupplierService.addSupplier() and only used for the
        // WHERE clause in updateSupplier(), so -1 is a safe placeholder on the "Add" path.
        Supplier supplier = new Supplier(
                editing ? existingSupplierId : -1,
                nameField.getText().trim(),
                contactPersonField.getText().trim(),
                phoneField.getText().trim(),
                emailField.getText().trim(),
                addressArea.getText().trim()
        );

        result = Optional.of(supplier);
        dispose();
    }

    /**
     * @return the supplier the user confirmed, or empty if they cancelled/closed the dialog.
     *         Only meaningful after {@code setVisible(true)} has returned, since this is a
     *         modal dialog and that call blocks until the user is done.
     */
    public Optional<Supplier> getResult() {
        return result;
    }
}
