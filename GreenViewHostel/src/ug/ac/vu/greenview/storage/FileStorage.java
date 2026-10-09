package ug.ac.vu.greenview.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Safe, low-level text file handling shared by all modules.
 * - A missing file is normal (first run) and gives an empty list.
 * - A damaged / unreadable file never crashes the program; it is backed up.
 * - Saving writes a temporary file first and then swaps it in, so a crash in the
 *   middle of a save cannot destroy the old data.
 *
 * @author Bijeneza Ndege Fidele (Member 7 - File Storage and Testing)
 */
public final class FileStorage {

    private static final char SEPARATOR = '|';

    private FileStorage() { }

    /** Joins fields into one line, escaping the separator, backslash and line breaks. */
    public static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            String f = fields[i] == null ? "" : fields[i];
            for (int k = 0; k < f.length(); k++) {
                char c = f.charAt(k);
                switch (c) {
                    case '\\': sb.append("\\\\"); break;
                    case '|': sb.append("\\|"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    default: sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /** The exact reverse of join. */
    public static String[] split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                switch (next) {
                    case 'n': current.append('\n'); break;
                    case 'r': current.append('\r'); break;
                    default: current.append(next);
                }
            } else if (c == SEPARATOR) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /** Reads all lines. Never throws: a missing or unreadable file gives an empty list. */
    public static List<String> readLines(Path file) {
        try {
            if (!Files.exists(file)) {
                return new ArrayList<>();
            }
            return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            System.out.println("WARNING: could not read " + file.getFileName()
                    + " (" + e.getMessage() + "). It will be backed up and treated as empty.");
            backup(file);
            return new ArrayList<>();
        }
    }

    /**
     * Writes all lines safely. Returns true when saved; on failure prints a
     * warning and returns false (the program keeps running).
     */
    public static boolean writeLines(Path file, List<String> lines) {
        Path temp = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            System.out.println("WARNING: could not save " + file.getFileName() + " (" + e.getMessage()
                    + "). Your change is still in memory; please check the data folder.");
            try {
                if (temp != null) {
                    Files.deleteIfExists(temp);
                }
            } catch (IOException ignored) {
                // nothing more we can do
            }
            return false;
        }
    }

    /** Copies a damaged file to a time-stamped backup so nothing is ever lost. */
    public static void backup(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Path copy = file.resolveSibling(file.getFileName() + ".damaged-" + stamp);
                Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("  A backup was kept as " + copy.getFileName());
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("  (Could not make a backup: " + e.getMessage() + ")");
        }
    }
}
