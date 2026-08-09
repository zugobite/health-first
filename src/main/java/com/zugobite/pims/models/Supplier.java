package com.zugobite.pims.models;

/**
 * A pharmaceutical supplier - maps 1:1 to a row in the `suppliers` table. Every
 * {@link Medicine} links back to exactly one {@code Supplier} via {@code supplier_id}, which
 * is why the schema forbids deleting a supplier that's still referenced by a medicine (see
 * the `FOREIGN KEY` in {@code database.sql}, and the resulting error surfaced in
 * {@code ManageSuppliersPanel.deleteSelected()}).
 *
 * <p>Mutable (unlike {@link User}) because Admin needs to edit an existing supplier's
 * details in place via {@link com.zugobite.pims.views.SupplierFormDialog} - that dialog
 * builds a fresh {@code Supplier} from the form fields rather than mutating one in place,
 * but the setters exist for callers who do want to mutate a loaded instance directly.</p>
 */
public class Supplier {

    private int supplierId;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;

    /**
     * Builds a supplier from a {@code suppliers} row (or from form fields before insert).
     * On insert, {@code supplierId} is ignored - MySQL assigns it via AUTO_INCREMENT.
     */
    public Supplier(int supplierId, String name, String contactPerson, String phone, String email, String address) {
        this.supplierId = supplierId;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Used by Swing's {@link javax.swing.JComboBox} to render this supplier as just its name
     * in the dropdown on {@link com.zugobite.pims.views.MedicineFormDialog} - without this
     * override, the combo box would fall back to the default {@code Object.toString()} and
     * show something unreadable like {@code Supplier@1a2b3c}.
     */
    @Override
    public String toString() {
        return name;
    }
}
