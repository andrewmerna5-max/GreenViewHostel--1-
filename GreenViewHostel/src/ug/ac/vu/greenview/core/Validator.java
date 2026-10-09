package ug.ac.vu.greenview.core;

import java.util.regex.Pattern;

/**
 * Small checks shared by every module's model classes.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class Validator {

    private static final Pattern NAME = Pattern.compile("[\\p{L}][\\p{L} .'-]{1,59}");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9]{9,13}");

    private Validator() { }

    /** Trims, removes control characters and collapses repeated spaces. */
    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean isValidName(String s) {
        return s != null && NAME.matcher(clean(s)).matches();
    }

    public static boolean isValidPhone(String s) {
        return s != null && PHONE.matcher(s.trim()).matches();
    }

    /** True when the cleaned text is between min and max characters long. */
    public static boolean isTextOfLength(String s, int min, int max) {
        int length = clean(s).length();
        return length >= min && length <= max;
    }
}
