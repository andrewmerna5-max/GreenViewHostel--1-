package ug.ac.vu.greenview.storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ug.ac.vu.greenview.core.IdGenerator;

/**
 * Saves the ID counters so an ID is never reused after a restart.
 *
 * @author Bijeneza Ndege Fidele (Member 7 - File Storage and Testing)
 */
public final class CounterStore {

    private CounterStore() { }

    public static void load(Path file) {
        Map<String, Integer> saved = new HashMap<>();
        for (String line : FileStorage.readLines(file)) {
            String[] parts = line.trim().split("=");
            if (parts.length == 2 && parts[0].length() == 1) {
                try {
                    saved.put(parts[0], Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    // damaged counter line - ignore it; loaded record IDs fix the counter anyway
                }
            }
        }
        IdGenerator.restore(saved);
    }

    public static void save(Path file) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : new TreeMap<>(IdGenerator.snapshot()).entrySet()) {
            lines.add(e.getKey() + "=" + e.getValue());
        }
        FileStorage.writeLines(file, lines);
    }
}
