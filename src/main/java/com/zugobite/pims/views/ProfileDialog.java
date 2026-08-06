package com.zugobite.pims.views;

import com.zugobite.pims.models.User;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Read-only "who am I logged in as" dialog, reachable from the "View Profile" button in the
 * sidebar footer on both {@link AdminDashboardView} and {@link CashierDashboardView} (see
 * {@link SidebarNav}).
 *
 * <p>Deliberately view-only: there's no edit/save flow here. {@link User} itself has no
 * setters (see its class comment), and profile self-editing (changing your own password,
 * etc.) isn't part of PLAN.md's scope - Admin's only user-management capability is
 * create/delete of *Cashier* accounts, not self-service profile edits for anyone.</p>
 */
public class ProfileDialog extends JDialog {

    public ProfileDialog(Frame owner, User user) {
        super(owner, "My Profile", true);

        JPanel panel = new JPanel(new GridBagLayout());
        UIUtils.applyStandardPadding(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, gbc, 0, "Full Name:", user.getFullName());
        addRow(panel, gbc, 1, "Username:", user.getUsername());
        addRow(panel, gbc, 2, "Role:", user.getRole().toString());
        addRow(panel, gbc, 3, "User ID:", String.valueOf(user.getUserId()));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        UIUtils.styleAsPrimaryButton(closeButton);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(closeButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(buttonRow, gbc);

        getRootPane().setDefaultButton(closeButton);
        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Adds one "label: value" row, using a plain (non-editable) {@link JLabel} for the value
     *  since this dialog never lets the user change anything - a text field would wrongly
     *  imply these fields are editable. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));
        panel.add(valueLabel, gbc);
    }
}
