package ug.ac.vu.greenview.core;

import java.util.Comparator;

/**
 * Sorts text the way people expect: "A2" comes before "A10", and IDs such as
 * G01-T9 come before G01-T10.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class NaturalOrder implements Comparator<String> {

    public static final NaturalOrder INSTANCE = new NaturalOrder();

    private NaturalOrder() { }

    @Override
    public int compare(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int startA = i;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                int startB = j;
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                String na = stripZeros(a.substring(startA, i));
                String nb = stripZeros(b.substring(startB, j));
                if (na.length() != nb.length()) {
                    return na.length() - nb.length();
                }
                int c = na.compareTo(nb);
                if (c != 0) {
                    return c;
                }
            } else {
                int c = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
                if (c != 0) {
                    return c;
                }
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }

    private static String stripZeros(String digits) {
        int k = 0;
        while (k < digits.length() - 1 && digits.charAt(k) == '0') {
            k++;
        }
        return digits.substring(k);
    }
}
