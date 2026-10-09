package ug.ac.vu.greenview.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates record IDs that start with the group code, for example G01-R001
 * (room) or G01-P014 (payment). Counters never go backwards, so an ID is never
 * handed out twice, even after records are deleted and the program restarts.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class IdGenerator {

    private static final Map<String, Integer> COUNTERS = new HashMap<>();
    private static Runnable changeListener;

    private IdGenerator() { }

    /** Next ID for a record type letter, e.g. next('R') gives G01-R001. */
    public static String next(char type) {
        String key = String.valueOf(Character.toUpperCase(type));
        int number;
        Runnable listener;
        synchronized (IdGenerator.class) {
            number = COUNTERS.getOrDefault(key, 0) + 1;
            COUNTERS.put(key, number);
            listener = changeListener;
        }
        if (listener != null) {
            listener.run();
        }
        return AppConfig.GROUP_CODE + "-" + key + String.format("%03d", number);
    }

    /** Makes sure an ID loaded from file will never be generated again. */
    public static synchronized void register(String id) {
        if (id == null) {
            return;
        }
        String prefix = AppConfig.GROUP_CODE + "-";
        if (!id.startsWith(prefix) || id.length() < prefix.length() + 2) {
            return;
        }
        String key = id.substring(prefix.length(), prefix.length() + 1);
        String digits = id.substring(prefix.length() + 1);
        try {
            int number = Integer.parseInt(digits);
            if (number > COUNTERS.getOrDefault(key, 0)) {
                COUNTERS.put(key, number);
            }
        } catch (NumberFormatException e) {
            // not one of our IDs - ignore
        }
    }

    public static synchronized Map<String, Integer> snapshot() {
        return new HashMap<>(COUNTERS);
    }

    public static synchronized void restore(Map<String, Integer> saved) {
        for (Map.Entry<String, Integer> e : saved.entrySet()) {
            if (e.getValue() > COUNTERS.getOrDefault(e.getKey(), 0)) {
                COUNTERS.put(e.getKey(), e.getValue());
            }
        }
    }

    /** Called every time a counter changes, so the storage layer can save it. */
    public static synchronized void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    /** Clears everything. Used by the automated tests. */
    public static synchronized void reset() {
        COUNTERS.clear();
        changeListener = null;
    }
}
