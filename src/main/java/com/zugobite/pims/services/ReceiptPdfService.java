package com.zugobite.pims.services;

import com.zugobite.pims.models.Sale;
import com.zugobite.pims.models.SaleItem;
import com.zugobite.pims.models.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a completed {@link Sale} as a professionally laid-out, single-page A4 PDF receipt
 * — the "save" half of PLAN.md §5.2's "support print and/or save" bill requirement
 * ({@link com.zugobite.pims.views.BillView}'s "Print" button already covers the "print"
 * half via Swing's built-in print pipeline).
 *
 * <p>Built directly on Apache PDFBox's low-level content-stream API (page background, ruled
 * lines, a shaded table header, right-aligned currency columns) rather than a plain
 * text dump, since a bill handed to a pharmacy customer is expected to look like an actual
 * printed receipt/invoice, not a debug log. PDFBox was chosen over alternatives (e.g. iText)
 * specifically because it's Apache-2.0 licensed, which is safe to bundle and redistribute
 * with this assignment's deliverable without any copyleft licensing complications.</p>
 *
 * <p>Uses only the 14 standard PDF fonts (Helvetica / Helvetica-Bold) rather than an
 * embedded custom font — those render identically in every PDF viewer with zero embedding
 * overhead, which keeps the generated file small and avoids any font-licensing questions.</p>
 */
public class ReceiptPdfService {

    // Layout constants. Centralised here so the whole receipt's proportions can be tuned in
    // one place instead of hunting through drawing code for magic numbers.
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 56f; // 0.78" side margins
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // Table column layout, as x-offsets from the left margin. The item name column gets the
    // remaining space; the other three are fixed-width and right-aligned since they're all
    // numeric (quantity/currency reads far more naturally right-aligned than left-aligned).
    // The PRICE/TOTAL gap in particular needs to comfortably clear each header label's
    // rendered width at HELVETICA_BOLD 10pt (~45-50pt for "PRICE"/"TOTAL") — a narrower gap
    // here previously let the two right-aligned headers overlap into unreadable text.
    private static final float COL_ITEM_X = 0f;
    private static final float COL_QTY_RIGHT_X = 300f;
    private static final float COL_PRICE_RIGHT_X = 400f;
    private static final float COL_TOTAL_RIGHT_X = CONTENT_WIDTH;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /**
     * Renders {@code sale} to {@code outputFile} as a one-page PDF receipt.
     *
     * @param sale    the completed, already-checked-out sale to render — its saleId and
     *                saleDate must already be populated (i.e. this is called *after*
     *                {@link SalesService#checkout}, never before)
     * @param cashier the cashier who processed the sale, shown in the receipt's "Served by"
     *                line; purely informational, never re-validated against {@code sale.getUserId()}
     * @throws IOException if the PDF can't be written to {@code outputFile} (e.g. permissions,
     *                      disk full, or an invalid path)
     */
    public void generate(Sale sale, User cashier, File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // Cursor tracks the current baseline y-position as we draw top-to-bottom, so
                // each drawing helper below can just report how much vertical space it used
                // and the caller advances the cursor by that amount — avoids hardcoding an
                // absolute y-coordinate for every single line on the page.
                float cursorY = PAGE_HEIGHT - MARGIN;

                cursorY = drawHeader(content, boldFont, regularFont, cursorY);
                cursorY = drawSaleInfo(content, regularFont, boldFont, sale, cashier, cursorY);
                cursorY = drawItemsTable(content, regularFont, boldFont, sale.getItems(), cursorY);
                cursorY = drawTotal(content, boldFont, sale, cursorY);
                drawFooter(content, regularFont, cursorY);
            }

            document.save(outputFile);
        }
    }

    /** Pharmacy name/tagline banner plus a ruled line underneath it. Returns the new cursor y. */
    private float drawHeader(PDPageContentStream content, PDFont boldFont, PDFont regularFont, float cursorY) throws IOException {
        drawText(content, boldFont, 22, MARGIN, cursorY, "HealthFirst Pharmacy");
        cursorY -= 18;
        drawText(content, regularFont, 10, MARGIN, cursorY, "Pharmacy Inventory Management System — Official Receipt");
        cursorY -= 14;
        drawRule(content, cursorY);
        return cursorY - 26;
    }

    /**
     * Two-column metadata block: sale number/date on the left, the cashier who served the
     * customer on the right — mirrors the "receipt number + date" / "served by" layout of a
     * typical retail till slip.
     */
    private float drawSaleInfo(PDPageContentStream content, PDFont regularFont, PDFont boldFont,
                                Sale sale, User cashier, float cursorY) throws IOException {
        float labelSize = 9;
        float valueSize = 11;
        float rightColumnX = MARGIN + CONTENT_WIDTH / 2f;

        drawText(content, regularFont, labelSize, MARGIN, cursorY, "RECEIPT NO.");
        drawText(content, regularFont, labelSize, rightColumnX, cursorY, "SERVED BY");
        cursorY -= 14;
        drawText(content, boldFont, valueSize, MARGIN, cursorY, "#" + sale.getSaleId());
        drawText(content, boldFont, valueSize, rightColumnX, cursorY,
                cashier != null ? cashier.getFullName() : "—");
        cursorY -= 20;

        drawText(content, regularFont, labelSize, MARGIN, cursorY, "DATE & TIME");
        cursorY -= 14;
        String dateText = sale.getSaleDate() != null ? sale.getSaleDate().format(DATE_FORMAT) : "—";
        drawText(content, boldFont, valueSize, MARGIN, cursorY, dateText);
        cursorY -= 24;

        drawRule(content, cursorY);
        return cursorY - 22;
    }

    /**
     * The line-item table: a shaded header row, one row per {@link SaleItem}, and a ruled
     * line under each row. Quantity/price/line-total are right-aligned within their columns
     * (see {@link #drawRightAligned}), which is what makes a column of numbers actually read
     * as a column rather than a ragged list.
     */
    private float drawItemsTable(PDPageContentStream content, PDFont regularFont, PDFont boldFont,
                                  List<SaleItem> items, float cursorY) throws IOException {
        float rowHeight = 20f;
        float headerHeight = 22f;

        // Shaded header background, drawn as a filled rectangle before the header text so
        // the text sits on top of it.
        content.setNonStrokingColor(0.90f, 0.90f, 0.90f);
        content.addRect(MARGIN, cursorY - headerHeight + 6, CONTENT_WIDTH, headerHeight);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);

        float headerTextY = cursorY - 8;
        drawText(content, boldFont, 10, MARGIN + COL_ITEM_X + 6, headerTextY, "ITEM");
        drawRightAligned(content, boldFont, 10, MARGIN + COL_QTY_RIGHT_X, headerTextY, "QTY");
        drawRightAligned(content, boldFont, 10, MARGIN + COL_PRICE_RIGHT_X, headerTextY, "PRICE");
        drawRightAligned(content, boldFont, 10, MARGIN + COL_TOTAL_RIGHT_X, headerTextY, "TOTAL");
        cursorY -= headerHeight;

        for (SaleItem item : items) {
            float textY = cursorY - 6;
            drawText(content, regularFont, 10, MARGIN + COL_ITEM_X + 6, textY, item.getMedicineName());
            drawRightAligned(content, regularFont, 10, MARGIN + COL_QTY_RIGHT_X, textY, String.valueOf(item.getQuantitySold()));
            drawRightAligned(content, regularFont, 10, MARGIN + COL_PRICE_RIGHT_X, textY, formatCurrency(item.getPriceAtSale()));
            drawRightAligned(content, regularFont, 10, MARGIN + COL_TOTAL_RIGHT_X, textY, formatCurrency(item.getLineTotal()));
            cursorY -= rowHeight;
            drawRule(content, cursorY + 4);
        }

        return cursorY - 10;
    }

    /** The bold, larger-type grand total line, right-aligned under the "LINE TOTAL" column. */
    private float drawTotal(PDPageContentStream content, PDFont boldFont, Sale sale, float cursorY) throws IOException {
        drawText(content, boldFont, 12, MARGIN, cursorY, "TOTAL");
        drawRightAligned(content, boldFont, 14, MARGIN + COL_TOTAL_RIGHT_X, cursorY, "R " + formatCurrency(sale.getTotalAmount()));
        cursorY -= 16;
        drawRule(content, cursorY);
        return cursorY - 30;
    }

    /** Closing thank-you line and a small-print note, centered near wherever the table left off. */
    private void drawFooter(PDPageContentStream content, PDFont regularFont, float cursorY) throws IOException {
        drawCentered(content, regularFont, 11, PAGE_WIDTH / 2f, cursorY, "Thank you for choosing HealthFirst Pharmacy");
        cursorY -= 16;
        drawCentered(content, regularFont, 8, PAGE_WIDTH / 2f, cursorY,
                "This receipt is computer-generated and does not require a signature.");
    }

    /** Draws a full-width horizontal rule at the given y, spanning the page's content width. */
    private void drawRule(PDPageContentStream content, float y) throws IOException {
        content.setLineWidth(0.75f);
        content.moveTo(MARGIN, y);
        content.lineTo(MARGIN + CONTENT_WIDTH, y);
        content.stroke();
    }

    /** Left-aligned text at (x, y), in the given font/size. The common case for labels and item names. */
    private void drawText(PDPageContentStream content, PDFont font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    /**
     * Right-aligned text whose right edge lands exactly at {@code rightX} — computed by
     * measuring the string's width in the given font and shifting the start position left
     * by that amount before drawing. This is what keeps the QTY/UNIT PRICE/LINE TOTAL
     * columns visually aligned as a column, regardless of how many digits each value has.
     */
    private void drawRightAligned(PDPageContentStream content, PDFont font, float size, float rightX, float y, String text) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        drawText(content, font, size, rightX - textWidth, y, text);
    }

    /** Horizontally centered text around {@code centerX} — used for the footer's closing lines. */
    private void drawCentered(PDPageContentStream content, PDFont font, float size, float centerX, float y, String text) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        drawText(content, font, size, centerX - textWidth / 2, y, text);
    }

    /** Formats a currency amount to exactly two decimal places, independent of the BigDecimal's own scale. */
    private String formatCurrency(java.math.BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
