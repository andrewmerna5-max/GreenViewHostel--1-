package ug.ac.vu.greenview.maintenance;

/**
 * What kind of problem needs fixing.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public enum IssueCategory {
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    FURNITURE("Furniture"),
    CLEANING("Cleaning"),
    SECURITY("Security (doors, locks)"),
    OTHER("Other");

    private final String label;

    IssueCategory(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
