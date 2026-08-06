package com.zugobite.pims.views;

import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;
import com.zugobite.pims.models.User;
import com.zugobite.pims.services.ReceiptPdfService;
import com.zugobite.pims.utils.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;

/**
 * The receipt window shown to the Cashier right after a successful checkout (PLAN.md §5.2:
 * "Generate a bill window; support print and/or save - simulation is acceptable, no real
 * payment gateway"). Purely a display of an already-completed {@link Sale}; it never writes
 * to the database itself since {@link com.zugobite.pims.services.SalesService#checkout} has
 * already persisted everything by the time this window is opened.
 *
 * <p>Covers both halves of "print and/or save": "Print" goes through Swing's built-in print
 * pipeline on the plain-text receipt shown in this window, while "Save as PDF" hands the
 * same {@link Sale} to {@link ReceiptPdfService} to render as a separate, properly
 * typeset PDF document - the two are independent outputs of the same underlying sale data,
 * not one derived from the other.</p>
 */
public class BillView extends JFrame {

    private final Sale sale;
    private final User cashier;
    private final ReceiptPdfService receiptPdfService = new ReceiptPdfService();

    /**
     * @param sale    the just-completed sale to display/export - must already have its
     *                saleId and saleDate populated by {@link com.zugobite.pims.services.SalesService#checkout}
     * @param cashier the cashier who processed the sale, included on the PDF receipt's
     *                "Served by" line
     */
    public BillView(Sale sale, User cashier) {
        super("Bill - Sale #" + sale.getSaleId());
        this.sale = sale;
        this.cashier = cashier;
        // DISPOSE_ON_CLOSE (not EXIT_ON_CLOSE): closing the bill should only close this
        // window, not the whole application - the Cashier is expected to keep using the POS
        // screen for the next customer afterward.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        UIUtils.applyAppIcon(this);

        JTextArea billText = new JTextArea();
        billText.setEditable(false);
        // Monospaced font so the price column lines up visually, the way a real till
        // receipt would, even though this is plain text rather than a formatted document.
        billText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        billText.setText(buildReceipt(sale));

        JButton printButton = new JButton("Print");
        UIUtils.styleAsOutlineButton(printButton);
        printButton.addActionListener(e -> {
            try {
                // JTextComponent.print() is Swing's built-in, zero-setup print pipeline -
                // sufficient for a quick till-style printout without integrating a
                // dedicated printing library.
                billText.print();
            } catch (PrinterException ex) {
                UIUtils.showError(this, "Printing failed: " + ex.getMessage());
            }
        });

        JButton savePdfButton = new JButton("Save as PDF");
        UIUtils.styleAsPrimaryButton(savePdfButton);
        savePdfButton.addActionListener(e -> saveAsPdf());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.add(printButton);
        buttonRow.add(savePdfButton);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(billText), BorderLayout.CENTER);
        panel.add(buttonRow, BorderLayout.SOUTH);
        setContentPane(panel);

        setSize(400, 500);
        setLocationRelativeTo(null);
    }

    /**
     * Prompts for a save location (defaulting to {@code Receipt-Sale-<id>.pdf}), then renders
     * the receipt to that file via {@link ReceiptPdfService}. Writing the PDF is delegated
     * entirely to that service - this method's only job is the file-chooser UI and reporting
     * success/failure back to the Cashier.
     */
    private void saveAsPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Receipt-Sale-" + sale.getSaleId() + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Document (*.pdf)", "pdf"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        // JFileChooser doesn't append the extension on its own if the user typed a bare
        // filename, so enforce it here rather than silently writing an extension-less file
        // that the OS won't recognise as a PDF.
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getParentFile(), target.getName() + ".pdf");
        }

        try {
            receiptPdfService.generate(sale, cashier, target);
            UIUtils.showInfo(this, "Receipt saved to " + target.getAbsolutePath());
        } catch (IOException ex) {
            UIUtils.showError(this, "Failed to save PDF: " + ex.getMessage());
        }
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
