package ug.ac.vu.greenview.core;

import java.util.Locale;

/**
 * Central settings for the whole Green View Hostel system.
 * Change GROUP_CODE to your real group number (for example "G07") and every
 * record ID in the system changes with it.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class AppConfig {

    public static final String HOSTEL_NAME = "Green View Hostel";
    public static final String GROUP_CODE = "G01";
    public static final String DATA_DIR = "data";
    public static final String CURRENCY = "UGX";

    private AppConfig() { }

    /** Formats an amount of money, for example "UGX 450,000". */
    public static String money(long amount) {
        return String.format(Locale.US, "%s %,d", CURRENCY, amount);
    }
}
