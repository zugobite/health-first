package com.zugobite.pims.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Stateless validation helpers shared by every Swing form in the {@code views} package
 * (login, medicine/supplier/cashier form dialogs, POS quantity entry).
 *
 * <p>Every method here takes the raw {@link String} straight out of a text field and
 * returns a boolean, so form code can do:
 * <pre>{@code
 * if (!InputValidator.isPositiveDecimal(priceField.getText())) {
 *     UIUtils.showError(this, "Price must be a positive number.");
 *     return;
 * }
 * }</pre>
 * without needing its own try/catch around parsing. This keeps validation logic in one
 * place instead of duplicated across every form dialog.</p>
 */
public final class InputValidator {

    // Utility class: never instantiated.
    private InputValidator() {
    }

    /** True if the string is non-null and contains something other than whitespace. */
    public static boolean isNonEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * True if the string parses as a decimal number strictly greater than zero. Used for
     * the `price` field — a free medicine or a negative price would never make sense for a
     * pharmacy, so zero and negative values are rejected here rather than at the database.
     */
    public static boolean isPositiveDecimal(String input) {
        try {
            return new BigDecimal(input).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * True if the string parses as a whole number >= 0. Used for stock quantities and
     * reorder levels, where zero is a perfectly valid value (out of stock, or "alert on any
     * stock at all") but negative counts are meaningless.
     */
    public static boolean isNonNegativeInteger(String input) {
        try {
            return Integer.parseInt(input) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * True if the string parses as an ISO-8601 date ({@code yyyy-MM-dd}). Used for the
     * expiry date field on {@link com.zugobite.pims.views.MedicineFormDialog} — this format
     * was chosen (over e.g. {@code dd/mm/yyyy}) because it's what {@link LocalDate#parse}
     * accepts with zero extra formatting code, and it sorts correctly as plain text too.
     */
    public static boolean isValidDate(String input) {
        try {
            LocalDate.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
