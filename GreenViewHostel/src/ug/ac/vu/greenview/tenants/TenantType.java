package ug.ac.vu.greenview.tenants;

/**
 * The two kinds of tenant Green View Hostel accepts.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public enum TenantType {
    STUDENT("Student"),
    WORKING("Working professional");

    private final String label;

    TenantType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
