package com.zugobite.pims.views;

import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

/**
 * The receipt window shown to the Cashier right after a successful checkout (PLAN.md §5.2:
 * "Generate a bill window; support print and/or save — simulation is acceptable, no real
 * payment gateway"). Purely a display of an already-completed {@link Sale}; it never writes
 * to the database itself since {@link com.zugobite.pims.services.SalesService#checkout} has
 * already persisted everything by the time this window is opened.
 */
public class BillView extends JFrame {

    public BillView(Sale sale) {
        super("Bill — Sale #" + sale.getSaleId());
        // DISPOSE_ON_CLOSE (not EXIT_ON_CLOSE): closing the bill should only close this
        // window, not the whole application — the Cashier is expected to keep using the POS
        // screen for the next customer afterward.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea billText = new JTextArea();
        billText.setEditable(false);
        // Monospaced font so the price column lines up visually, the way a real till
        // receipt would, even though this is plain text rather than a formatted document.
        billText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        billText.setText(buildReceipt(sale));

        JButton printButton = new JButton("Print");
        printButton.addActionListener(e -> {
            try {
                // JTextComponent.print() is Swing's built-in, zero-setup print pipeline —
                // sufficient for the "simulation is acceptable" bar this feature needs to
                // clear, without integrating a real printing/PDF library.
                billText.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage());
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(billText), BorderLayout.CENTER);
        panel.add(printButton, BorderLayout.SOUTH);
        setContentPane(panel);

        setSize(400, 500);
        setLocationRelativeTo(null);
    }

    /** Formats the sale as a plain-text, till-receipt-style layout: one line per item, then the total. */
    private String buildReceipt(Sale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("HealthFirst Pharmacy\n");
        sb.append("Sale #").append(sale.getSaleId()).append("\n");
        sb.append("--------------------------------\n");
        for (SaleItem item : sale.getItems()) {
            sb.append(String.format("%-16s x%-3d %8s%n", item.getMedicineName(), item.getQuantitySold(), item.getLineTotal()));
        }
        sb.append("--------------------------------\n");
        sb.append("Total: R").append(sale.getTotalAmount()).append("\n");
        return sb.toString();
    }
}
