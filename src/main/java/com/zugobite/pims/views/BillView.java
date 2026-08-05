package com.zugobite.pims.views;

import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

public class BillView extends JFrame {

    public BillView(Sale sale) {
        super("Bill — Sale #" + sale.getSaleId());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea billText = new JTextArea();
        billText.setEditable(false);
        billText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        billText.setText(buildReceipt(sale));

        JButton printButton = new JButton("Print");
        printButton.addActionListener(e -> {
            try {
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
