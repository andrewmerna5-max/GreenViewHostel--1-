package ug.ac.vu.greenview.core;

import java.util.Collection;

/**
 * Shared abstract base class for every stored item in the hostel system
 * (rooms, tenants, payments, requests, visitors...). Every record has an ID that
 * starts with the group code, for example G01-R001.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public abstract class Record {

    private final String id;

    protected Record(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty.");
        }
        this.id = id.trim();
    }

    public final String getId() {
        return id;
    }

    /** A one-line, human readable description. Each subclass writes its own. */
    public abstract String describe();

    /** The values saved to file for this record. The first value must be the ID. */
    public abstract String[] toFields();

    /**
     * Polymorphism in action: loops over records of different classes and lets
     * each one describe itself.
     */
    public static String describeAll(Collection<? extends Record> records) {
        StringBuilder sb = new StringBuilder();
        for (Record r : records) {
            sb.append(r.describe()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return describe();
    }
}
