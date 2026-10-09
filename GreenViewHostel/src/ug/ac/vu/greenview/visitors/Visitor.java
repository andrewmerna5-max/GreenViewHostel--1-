package ug.ac.vu.greenview.visitors;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Validator;

/**
 * A person registered as a hostel visitor. Use FICTIONAL names and phone numbers.
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public class Visitor extends Record {

    private String fullName;
    private String phone;                    // "" when none given
    private final LocalDate dateRegistered;

    public Visitor(String id, String fullName, String phone, LocalDate dateRegistered)
            throws InvalidVisitorException {
        super(id);
        this.fullName = validateName(fullName);
        this.phone = validatePhone(phone);
        if (dateRegistered == null || dateRegistered.isAfter(LocalDate.now())) {
            throw new InvalidVisitorException("Registration date is required and cannot be in the future.");
        }
        this.dateRegistered = dateRegistered;
    }

    public static String validateName(String name) throws InvalidVisitorException {
        if (!Validator.isValidName(name)) {
            throw new InvalidVisitorException("Visitor name must be 2 to 60 letters (spaces, - . ' allowed).");
        }
        return Validator.clean(name);
    }

    public static String validatePhone(String phone) throws InvalidVisitorException {
        String p = phone == null ? "" : phone.replace(" ", "");
        if (!p.isEmpty() && !Validator.isValidPhone(p)) {
            throw new InvalidVisitorException("Phone must be 9 to 13 digits (use a fictional number).");
        }
        return p;
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public LocalDate getDateRegistered() { return dateRegistered; }

    public final void setFullName(String fullName) throws InvalidVisitorException {
        this.fullName = validateName(fullName);
    }

    public final void setPhone(String phone) throws InvalidVisitorException {
        this.phone = validatePhone(phone);
    }

    /** Checks both values first so a bad value changes nothing. */
    public final void updateDetails(String newName, String newPhone) throws InvalidVisitorException {
        String n = validateName(newName);
        String p = validatePhone(newPhone);
        this.fullName = n;
        this.phone = p;
    }

    @Override
    public String describe() {
        return String.format("%s | Visitor: %s | Phone: %s | Registered: %s",
                getId(), fullName, phone.isEmpty() ? "-" : phone, dateRegistered);
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), fullName, phone, dateRegistered.toString()};
    }

    public static Visitor fromFields(String[] f) throws Exception {
        if (f.length < 4) {
            throw new IllegalArgumentException("expected 4 fields but found " + f.length);
        }
        return new Visitor(f[0], f[1], f[2], LocalDate.parse(f[3].trim()));
    }
}
