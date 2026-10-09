package ug.ac.vu.greenview.tenants;

import ug.ac.vu.greenview.core.Validator;

/**
 * The person to call if something happens to a tenant. Use FICTIONAL details only.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public class EmergencyContact {

    private String name;
    private String relationship;
    private String phone;

    public EmergencyContact(String name, String relationship, String phone) throws InvalidTenantException {
        setName(name);
        setRelationship(relationship);
        setPhone(phone);
    }

    public String getName() { return name; }
    public String getRelationship() { return relationship; }
    public String getPhone() { return phone; }

    public final void setName(String name) throws InvalidTenantException {
        if (!Validator.isValidName(name)) {
            throw new InvalidTenantException("Emergency contact name must be 2 to 60 letters.");
        }
        this.name = Validator.clean(name);
    }

    public final void setRelationship(String relationship) throws InvalidTenantException {
        if (!Validator.isTextOfLength(relationship, 2, 30)) {
            throw new InvalidTenantException("Relationship must be 2 to 30 characters (e.g. Mother).");
        }
        this.relationship = Validator.clean(relationship);
    }

    public final void setPhone(String phone) throws InvalidTenantException {
        String p = phone == null ? "" : phone.replace(" ", "");
        if (!Validator.isValidPhone(p)) {
            throw new InvalidTenantException("Emergency contact phone must be 9 to 13 digits.");
        }
        this.phone = p;
    }

    @Override
    public String toString() {
        return name + " (" + relationship + ", " + phone + ")";
    }
}
