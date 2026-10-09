package ug.ac.vu.greenview.visitors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Validator;
import ug.ac.vu.greenview.tenants.Tenant;

/**
 * One visit: a Visitor came to see a Tenant on a date, checked in at a time and
 * (later) checked out. Holds object references to both the Visitor and the Tenant.
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public class Visit extends Record {

    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final int MAX_PURPOSE = 80;

    private final Visitor visitor;
    private final Tenant tenant;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;       // null while the visitor is still inside
    private String purpose;

    public Visit(String id, Visitor visitor, Tenant tenant, LocalDate date, LocalTime checkIn, String purpose)
            throws InvalidVisitorException {
        super(id);
        if (visitor == null) {
            throw new InvalidVisitorException("A visitor is required.");
        }
        if (tenant == null) {
            throw new InvalidVisitorException("The tenant being visited is required.");
        }
        this.visitor = visitor;
        this.tenant = tenant;
        checkWhen(date, checkIn);
        this.date = date;
        this.checkIn = checkIn;
        this.purpose = validatePurpose(purpose);
    }

    // ------------------------------------------------------ validation rules

    public static String validatePurpose(String purpose) throws InvalidVisitorException {
        if (!Validator.isTextOfLength(purpose, 2, MAX_PURPOSE)) {
            throw new InvalidVisitorException("Purpose of visit must be 2 to " + MAX_PURPOSE + " characters.");
        }
        return Validator.clean(purpose);
    }

    /** A visit cannot start in the future. */
    private static void checkWhen(LocalDate date, LocalTime time) throws InvalidVisitorException {
        if (date == null || time == null) {
            throw new InvalidVisitorException("Visit date and check-in time are required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidVisitorException("Visit date cannot be in the future.");
        }
        if (date.equals(LocalDate.now()) && time.isAfter(LocalTime.now())) {
            throw new InvalidVisitorException("Check-in time cannot be in the future.");
        }
    }

    // --------------------------------------------------------------- getters

    public Visitor getVisitor() { return visitor; }
    public Tenant getTenant() { return tenant; }
    public LocalDate getDate() { return date; }
    public LocalTime getCheckIn() { return checkIn; }
    public LocalTime getCheckOut() { return checkOut; }
    public String getPurpose() { return purpose; }
    public boolean isInside() { return checkOut == null; }

    // --------------------------------------------------------------- changes

    public void checkOutAt(LocalTime time) throws InvalidVisitorException {
        if (!isInside()) {
            throw new InvalidVisitorException("This visitor has already checked out at " + checkOut.format(TIME_FORMAT) + ".");
        }
        if (time == null) {
            throw new InvalidVisitorException("Check-out time is required.");
        }
        if (!time.isAfter(checkIn)) {
            throw new InvalidVisitorException("Check-out time must be after the check-in time (" + checkIn.format(TIME_FORMAT) + ").");
        }
        if (date.equals(LocalDate.now()) && time.isAfter(LocalTime.now())) {
            throw new InvalidVisitorException("Check-out time cannot be in the future.");
        }
        this.checkOut = time;
    }

    /** Corrects the date, check-in time and purpose. Everything is checked before anything changes. */
    public void reschedule(LocalDate newDate, LocalTime newCheckIn, String newPurpose) throws InvalidVisitorException {
        checkWhen(newDate, newCheckIn);
        String p = validatePurpose(newPurpose);
        if (checkOut != null && !checkOut.isAfter(newCheckIn)) {
            throw new InvalidVisitorException("Check-in must be before the check-out time (" + checkOut.format(TIME_FORMAT) + ").");
        }
        this.date = newDate;
        this.checkIn = newCheckIn;
        this.purpose = p;
    }

    // ------------------------------------------------------- Record methods

    @Override
    public String describe() {
        return String.format("%s | Visitor: %s (%s) | Visiting: %s (%s) | Date: %s | In: %s | Out: %s | Purpose: %s",
                getId(), visitor.getFullName(), visitor.getId(), tenant.getFullName(), tenant.getId(),
                date, checkIn.format(TIME_FORMAT),
                checkOut == null ? "still inside" : checkOut.format(TIME_FORMAT), purpose);
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), visitor.getId(), tenant.getId(), date.toString(),
                checkIn.format(TIME_FORMAT), checkOut == null ? "" : checkOut.format(TIME_FORMAT), purpose};
    }
}
