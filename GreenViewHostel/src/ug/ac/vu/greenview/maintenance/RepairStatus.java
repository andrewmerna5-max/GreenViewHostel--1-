package ug.ac.vu.greenview.maintenance;

/**
 * Where a repair is up to.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public enum RepairStatus {
    PENDING("Pending"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    private final String label;

    RepairStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
