package ug.ac.vu.greenview.maintenance;

/**
 * How urgent a repair is. The order of the constants matters: later = more urgent.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public enum Priority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
