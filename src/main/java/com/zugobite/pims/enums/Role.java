package com.zugobite.pims.enums;

/**
 * The two account types PIMS supports, mirroring the `role` ENUM column on the `users`
 * table in {@code database.sql}. Every access-control decision in the app - which
 * dashboard a user lands on after login, which tabs/buttons are shown - ultimately branches
 * on this enum.
 *
 * <p>The enum constants are upper-case ({@code ADMIN}, {@code CASHIER}) while the database
 * stores mixed-case strings ({@code 'Admin'}, {@code 'Cashier'}); see
 * {@link com.zugobite.pims.services.UserService#authenticate} for the conversion.</p>
 */
public enum Role {
    ADMIN,
    CASHIER
}
