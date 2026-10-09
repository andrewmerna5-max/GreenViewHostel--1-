package ug.ac.vu.greenview.core;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The ONE class used by every module to read from the keyboard. Every method
 * keeps asking until the person types something valid, so bad input can never
 * crash the program.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class InputHelper {

    /** A check that turns typed text into a valid value or explains the problem. */
    public interface Check<T> {
        T apply(String text) throws Exception;
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private static Scanner scanner = new Scanner(System.in);

    private InputHelper() { }

    /** Replaces the keyboard with another stream. Used by the automated tests. */
    public static void setInput(InputStream in) {
        scanner = new Scanner(in);
    }

    // ------------------------------------------------------------ raw line

    /** Shows the prompt and returns whatever was typed, trimmed (may be empty). */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            if (!scanner.hasNextLine()) {
                throw new InputClosedException();
            }
            return scanner.nextLine().trim();
        } catch (IllegalStateException | NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    /** Keeps asking until the check accepts the text. */
    public static <T> T readValid(String prompt, Check<T> check) {
        while (true) {
            String text = readLine(prompt);
            try {
                return check.apply(text);
            } catch (InputClosedException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("  Invalid: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- text

    public static String readRequired(String prompt) {
        return readRequired(prompt, 100);
    }

    public static String readRequired(String prompt, int maxLength) {
        while (true) {
            String value = Validator.clean(readLine(prompt));
            if (value.isEmpty()) {
                System.out.println("  Input cannot be empty. Please try again.");
            } else if (value.length() > maxLength) {
                System.out.println("  Too long (maximum " + maxLength + " characters).");
            } else {
                return value;
            }
        }
    }

    /** Returns an empty string when the person just presses Enter. */
    public static String readOptional(String prompt, int maxLength) {
        while (true) {
            String value = Validator.clean(readLine(prompt));
            if (value.length() <= maxLength) {
                return value;
            }
            System.out.println("  Too long (maximum " + maxLength + " characters).");
        }
    }

    public static String readName(String prompt) {
        while (true) {
            String value = Validator.clean(readLine(prompt));
            if (Validator.isValidName(value)) {
                return value;
            }
            System.out.println("  Use letters only (2 to 60 characters); spaces, - . ' are allowed.");
        }
    }

    /** Enter keeps the current value. */
    public static String readNameOrKeep(String prompt, String current) {
        while (true) {
            String value = Validator.clean(readLine(prompt));
            if (value.isEmpty()) {
                return current;
            }
            if (Validator.isValidName(value)) {
                return value;
            }
            System.out.println("  Use letters only (2 to 60 characters); spaces, - . ' are allowed.");
        }
    }

    /** Enter keeps the current value. */
    public static String readTextOrKeep(String prompt, int maxLength, String current) {
        while (true) {
            String value = Validator.clean(readLine(prompt));
            if (value.isEmpty()) {
                return current;
            }
            if (value.length() <= maxLength) {
                return value;
            }
            System.out.println("  Too long (maximum " + maxLength + " characters).");
        }
    }

    public static String readPhone(String prompt) {
        while (true) {
            String value = readLine(prompt).replace(" ", "");
            if (Validator.isValidPhone(value)) {
                return value;
            }
            System.out.println("  Enter 9 to 13 digits (a leading + is allowed). Use a FICTIONAL number.");
        }
    }

    /** Enter gives an empty string (no phone). */
    public static String readOptionalPhone(String prompt) {
        while (true) {
            String value = readLine(prompt).replace(" ", "");
            if (value.isEmpty() || Validator.isValidPhone(value)) {
                return value;
            }
            System.out.println("  Enter 9 to 13 digits (a leading + is allowed), or press Enter to skip.");
        }
    }

    /** Enter keeps the current phone; a single "-" clears it. */
    public static String readPhoneOrKeep(String prompt, String current) {
        while (true) {
            String value = readLine(prompt).replace(" ", "");
            if (value.isEmpty()) {
                return current;
            }
            if (value.equals("-")) {
                return "";
            }
            if (Validator.isValidPhone(value)) {
                return value;
            }
            System.out.println("  Enter 9 to 13 digits, \"-\" to clear, or press Enter to keep.");
        }
    }

    // ------------------------------------------------------------- numbers

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int n = Integer.parseInt(text);
                if (n >= min && n <= max) {
                    return n;
                }
                System.out.println("  Enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Enter a valid whole number.");
            }
        }
    }

    /** Whole numbers only (UGX has no cents). Commas such as 450,000 are accepted. */
    public static long readLong(String prompt, long min, long max) {
        while (true) {
            Long n = parseLong(readLine(prompt), min, max);
            if (n != null) {
                return n;
            }
        }
    }

    /** Returns null when the person just presses Enter. */
    public static Long readOptionalLong(String prompt, long min, long max) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return null;
            }
            Long n = parseLong(text, min, max);
            if (n != null) {
                return n;
            }
        }
    }

    private static Long parseLong(String text, long min, long max) {
        try {
            long n = Long.parseLong(text.replace(",", "").replace(" ", ""));
            if (n >= min && n <= max) {
                return n;
            }
            System.out.println("  Enter an amount from " + String.format("%,d", min)
                    + " to " + String.format("%,d", max) + ".");
        } catch (NumberFormatException e) {
            System.out.println("  Enter a valid whole number (digits only).");
        }
        return null;
    }

    // ------------------------------------------------------- dates & times

    public static LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readLine(prompt));
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format YYYY-MM-DD, for example 2026-10-07 (a real date).");
            }
        }
    }

    /** Enter means today. */
    public static LocalDate readDateOrToday(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format YYYY-MM-DD, or press Enter for today.");
            }
        }
    }

    public static LocalTime readTime(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(readLine(prompt), TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("  Use 24-hour time as HH:mm, for example 14:30.");
            }
        }
    }

    /** Enter means the current time. */
    public static LocalTime readTimeOrNow(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                return LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
            }
            try {
                return LocalTime.parse(text, TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("  Use 24-hour time as HH:mm, or press Enter for now.");
            }
        }
    }

    public static YearMonth readYearMonth(String prompt) {
        while (true) {
            try {
                return YearMonth.parse(readLine(prompt));
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format YYYY-MM, for example 2026-10.");
            }
        }
    }

    // ------------------------------------------------------------- choices

    public static boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt + " (y/n): ").toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            System.out.println("  Please type y or n.");
        }
    }

    /** Shows the constants of an enum as a numbered list and returns the one chosen. */
    public static <E extends Enum<E>> E readEnum(String title, Class<E> type) {
        E[] values = type.getEnumConstants();
        System.out.println(title);
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i]);
        }
        int choice = readInt("  Choose (1-" + values.length + "): ", 1, values.length);
        return values[choice - 1];
    }
}
