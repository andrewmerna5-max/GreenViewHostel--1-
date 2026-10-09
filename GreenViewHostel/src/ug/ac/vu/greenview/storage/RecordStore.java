package ug.ac.vu.greenview.storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.Record;

/**
 * Loads and saves a list of records of one type to one file. Used by every
 * module service: load() at start-up, save() after every change.
 * A damaged line is skipped with a warning; the good lines still load.
 *
 * @param <T> the kind of record stored in the file
 * @author Bijeneza Ndege Fidele (Member 7 - File Storage and Testing)
 */
public final class RecordStore<T extends Record> {

    /** Turns the fields of one line into a record, or throws if the line is bad. */
    public interface Parser<T> {
        T parse(String[] fields) throws Exception;
    }

    private final Path file;
    private final Parser<T> parser;

    public RecordStore(Path file, Parser<T> parser) {
        this.file = file;
        this.parser = parser;
    }

    public Path getFile() {
        return file;
    }

    /** Never throws. Returns every record that could be read correctly. */
    public List<T> load() {
        List<T> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int damaged = 0;
        int lineNumber = 0;
        for (String line : FileStorage.readLines(file)) {
            lineNumber++;
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                T record = parser.parse(FileStorage.split(line));
                if (!seenIds.add(record.getId())) {
                    throw new IllegalArgumentException("duplicate ID " + record.getId());
                }
                IdGenerator.register(record.getId());
                result.add(record);
            } catch (Exception e) {
                damaged++;
                System.out.println("WARNING: skipped damaged line " + lineNumber + " in "
                        + file.getFileName() + " (" + e.getMessage() + ")");
            }
        }
        if (damaged > 0) {
            FileStorage.backup(file);
        }
        return result;
    }

    /** Saves the whole list. Returns false (and warns) if the file could not be written. */
    public boolean save(Collection<? extends T> records) {
        List<String> lines = new ArrayList<>();
        for (T record : records) {
            lines.add(FileStorage.join(record.toFields()));
        }
        return FileStorage.writeLines(file, lines);
    }
}
