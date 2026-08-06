package com.zugobite.pims.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports any of the four generic reports produced by {@link ReportService} — Sales,
 * Item-Wise, Low Stock, Expiry — to either a paginated PDF or a styled Excel workbook.
 *
 * <p>Both formats are driven off the exact same input shape {@link ReportService} already
 * returns for its on-screen table ({@link com.zugobite.pims.views.ReportsPanel}):
 * {@code List<Map<String, Object>>}, where every row uses the same ordered set of keys.
 * That genericity is what lets one exporter class handle all four reports without knowing
 * anything about sales, medicines, or suppliers specifically — it only ever deals in column
 * names and cell values.</p>
 *
 * <p>PDF export is built directly on PDFBox's content-stream API (same approach as
 * {@link ReceiptPdfService}, but with pagination added, since a report — unlike a single
 * bill — can run to any number of rows). Excel export uses Apache POI's {@code XSSFWorkbook}
 * for a real, styled {@code .xlsx} file: bold shaded header row, right-aligned numeric
 * columns with proper numeric/date cell types (not just text), and auto-sized columns —
 * something a hand-rolled OOXML writer would be much riskier to get right.</p>
 */
public class ReportExportService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 48f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float ROW_HEIGHT = 20f;
    private static final float HEADER_ROW_HEIGHT = 22f;

    private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter CELL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter CELL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ---------------------------------------------------------------------
    // PDF export
    // ---------------------------------------------------------------------

    /**
     * Renders {@code rows} as a titled, paginated PDF table. A fresh page is started
     * whenever the next row wouldn't fit above the bottom margin (see {@link PdfCursor}),
     * with the column header row redrawn at the top of every page so a reader flipping to
     * page 3 doesn't have to flip back to page 1 to know what each column means.
     *
     * @param title      report title shown at the top of the first page (e.g. "Sales Report")
     * @param rows       the report data, one {@link Map} per row, all sharing the same key order
     * @param outputFile where to write the PDF
     */
    public void exportToPdf(String title, List<Map<String, Object>> rows, File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            List<String> columns = rows.isEmpty() ? List.of() : List.copyOf(rows.get(0).keySet());
            float[] columnRightX = computeColumnRightEdges(columns.size());
            boolean[] rightAligned = rows.isEmpty() ? new boolean[0] : computeRightAlignment(rows.get(0), columns);

            PdfCursor cursor = new PdfCursor(document, regularFont, boldFont);
            cursor.startNewPage();
            cursor.drawTitleBlock(title);
            cursor.drawColumnHeader(columns, columnRightX);

            if (rows.isEmpty()) {
                cursor.drawText(regularFont, 11, MARGIN, cursor.y - 20, "No data for this report.");
            } else {
                for (Map<String, Object> row : rows) {
                    cursor.ensureRoomForRow(columns, columnRightX);
                    cursor.drawDataRow(row, columns, columnRightX, rightAligned);
                }
            }

            cursor.close();
            document.save(outputFile);
        }
    }

    /** Evenly splits {@link #CONTENT_WIDTH} across {@code columnCount} columns, returning each column's right edge as an x-offset from the margin. */
    private float[] computeColumnRightEdges(int columnCount) {
        if (columnCount == 0) {
            return new float[0];
        }
        float columnWidth = CONTENT_WIDTH / columnCount;
        float[] rightEdges = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            rightEdges[i] = columnWidth * (i + 1);
        }
        return rightEdges;
    }

    /** A column is right-aligned (numbers, currency) if its first row's value is a {@link Number}; everything else (text, dates) is left-aligned. */
    private boolean[] computeRightAlignment(Map<String, Object> firstRow, List<String> columns) {
        boolean[] result = new boolean[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            Object value = firstRow.get(columns.get(i));
            result[i] = value instanceof Number;
        }
        return result;
    }

    /**
     * Bundles the mutable per-document PDF drawing state (current page, content stream,
     * vertical cursor, page number) so the pagination logic in {@link #exportToPdf} doesn't
     * have to juggle those as loose local variables. Every method advances {@link #y}
     * downward as it draws, and {@link #ensureRoomForRow} is what triggers a page break.
     */
    private static final class PdfCursor {
        private final PDDocument document;
        private final PDFont regularFont;
        private final PDFont boldFont;
        private PDPageContentStream content;
        private float y;
        private int pageNumber;

        PdfCursor(PDDocument document, PDFont regularFont, PDFont boldFont) {
            this.document = document;
            this.regularFont = regularFont;
            this.boldFont = boldFont;
        }

        void startNewPage() throws IOException {
            if (content != null) {
                drawPageFooter();
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
            pageNumber++;
        }

        void close() throws IOException {
            if (content != null) {
                drawPageFooter();
                content.close();
            }
        }

        private void drawPageFooter() throws IOException {
            String label = "Page " + pageNumber;
            float width = regularFont.getStringWidth(label) / 1000 * 8;
            drawText(regularFont, 8, PAGE_WIDTH - MARGIN - width, MARGIN / 2, label);
        }

        void drawTitleBlock(String title) throws IOException {
            drawText(boldFont, 18, MARGIN, y, title);
            y -= 16;
            drawText(regularFont, 9, MARGIN, y,
                    "Generated: " + LocalDateTime.now().format(GENERATED_AT_FORMAT));
            y -= 20;
        }

        void drawColumnHeader(List<String> columns, float[] columnRightX) throws IOException {
            content.setNonStrokingColor(0.90f, 0.90f, 0.90f);
            content.addRect(MARGIN, y - HEADER_ROW_HEIGHT + 6, CONTENT_WIDTH, HEADER_ROW_HEIGHT);
            content.fill();
            content.setNonStrokingColor(0f, 0f, 0f);

            float textY = y - 8;
            float leftX = MARGIN;
            for (int i = 0; i < columns.size(); i++) {
                String label = prettifyColumnName(columns.get(i)).toUpperCase();
                drawText(boldFont, 9, leftX + 4, textY, label);
                leftX = MARGIN + columnRightX[i];
            }
            y -= HEADER_ROW_HEIGHT;
        }

        /** Starts a new page (redrawing the column header) if the next row would run past the bottom margin. */
        void ensureRoomForRow(List<String> columns, float[] columnRightX) throws IOException {
            if (y - ROW_HEIGHT < MARGIN + 20) {
                startNewPage();
                drawColumnHeader(columns, columnRightX);
            }
        }

        void drawDataRow(Map<String, Object> row, List<String> columns, float[] columnRightX, boolean[] rightAligned) throws IOException {
            float textY = y - 6;
            float leftX = MARGIN;
            for (int i = 0; i < columns.size(); i++) {
                String text = formatCellValue(row.get(columns.get(i)));
                float rightX = MARGIN + columnRightX[i];
                if (rightAligned[i]) {
                    float textWidth = regularFont.getStringWidth(text) / 1000 * 9;
                    drawText(regularFont, 9, rightX - 4 - textWidth, textY, text);
                } else {
                    drawText(regularFont, 9, leftX + 4, textY, text);
                }
                leftX = rightX;
            }
            y -= ROW_HEIGHT;
            content.setLineWidth(0.4f);
            content.moveTo(MARGIN, y + 5);
            content.lineTo(MARGIN + CONTENT_WIDTH, y + 5);
            content.stroke();
        }

        private void drawText(PDFont font, float size, float x, float y, String text) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }
    }

    // ---------------------------------------------------------------------
    // Excel export
    // ---------------------------------------------------------------------

    /**
     * Renders {@code rows} as a single-sheet {@code .xlsx} workbook: a title, a
     * "Generated at" note, then a bold shaded header row followed by one data row per
     * report row, with numeric/date cells written as real Excel numeric/date types (not
     * text) so they sort and total correctly if a user pivots or charts them further.
     *
     * @param title      used as both the sheet name (sanitised — Excel forbids
     *                   {@code \ / ? * [ ]} and a 31-character limit in sheet names) and the
     *                   worksheet's title row
     * @param rows       the report data, one {@link Map} per row, all sharing the same key order
     * @param outputFile where to write the {@code .xlsx} file
     */
    public void exportToExcel(String title, List<Map<String, Object>> rows, File outputFile) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(title));

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle subtitleStyle = subtitleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle currencyStyle = numericStyle(workbook, "#,##0.00");
            CellStyle integerStyle = numericStyle(workbook, "#,##0");
            CellStyle dateStyle = dateStyle(workbook, "dd mmm yyyy");
            CellStyle dateTimeStyle = dateStyle(workbook, "dd mmm yyyy hh:mm");

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue(title);
            titleRow.getCell(0).setCellStyle(titleStyle);

            Row subtitleRow = sheet.createRow(rowIndex++);
            subtitleRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(GENERATED_AT_FORMAT));
            subtitleRow.getCell(0).setCellStyle(subtitleStyle);

            rowIndex++; // blank spacer row between the title block and the table

            List<String> columns = rows.isEmpty() ? List.of() : List.copyOf(rows.get(0).keySet());
            Row headerRow = sheet.createRow(rowIndex++);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(prettifyColumnName(columns.get(c)));
                cell.setCellStyle(headerStyle);
            }

            for (Map<String, Object> reportRow : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < columns.size(); c++) {
                    writeCell(row.createCell(c), reportRow.get(columns.get(c)), currencyStyle, integerStyle, dateStyle, dateTimeStyle);
                }
            }

            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                workbook.write(out);
            }
        }
    }

    /**
     * Writes one Excel cell, choosing the cell type/style based on the Java type of
     * {@code value}. Written as an {@code instanceof} chain rather than a type-pattern
     * {@code switch} (JEP 441) since that syntax only became a stable, non-preview language
     * feature in Java 21 — this project targets Java 17+ (see README's Prerequisites), so
     * the whole codebase has to stay compilable on a plain Java 17 {@code javac} with no
     * {@code --enable-preview} flag.
     */
    private void writeCell(Cell cell, Object value, CellStyle currencyStyle, CellStyle integerStyle,
                            CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof BigDecimal bigDecimal) {
            cell.setCellValue(bigDecimal.doubleValue());
            cell.setCellStyle(currencyStyle);
        } else if (value instanceof Integer integer) {
            cell.setCellValue(integer);
            cell.setCellStyle(integerStyle);
        } else if (value instanceof java.sql.Timestamp timestamp) {
            cell.setCellValue(timestamp.toLocalDateTime());
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof java.sql.Date sqlDate) {
            cell.setCellValue(sqlDate.toLocalDate());
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDate localDate) {
            cell.setCellValue(localDate);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime localDateTime) {
            cell.setCellValue(localDateTime);
            cell.setCellStyle(dateTimeStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle subtitleStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle numericStyle(XSSFWorkbook workbook, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    /** Strips characters Excel forbids in sheet names and truncates to its 31-character limit. */
    private String sanitizeSheetName(String title) {
        String cleaned = title.replaceAll("[\\\\/?*\\[\\]:]", "");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    /**
     * Converts a report row's raw map key (camelCase like "totalAmount", or snake_case like
     * "quantity_in_stock" — {@link ReportService}'s four report queries produce a mix of
     * both) into a human-readable column header, e.g. "Total Amount" / "Quantity In Stock".
     */
    private static String prettifyColumnName(String key) {
        String spaced = key.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2");
        StringBuilder result = new StringBuilder();
        for (String word : spaced.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(' ');
        }
        return result.toString().trim();
    }

    /**
     * Formats a single report cell value as PDF display text — mirrors the type handling in
     * {@link #writeCell}, minus the Excel-specific cell typing. Written as an
     * {@code instanceof} chain rather than a type-pattern {@code switch} for the same
     * Java-17-compatibility reason explained on {@link #writeCell}.
     */
    private static String formatCellValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } else if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(CELL_DATETIME_FORMAT);
        } else if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(CELL_DATE_FORMAT);
        } else if (value instanceof LocalDate localDate) {
            return localDate.format(CELL_DATE_FORMAT);
        } else if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(CELL_DATETIME_FORMAT);
        } else {
            return value.toString();
        }
    }
}
