package ug.ac.vu.greenview.rent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.rooms.Room;
import ug.ac.vu.greenview.tenants.Tenant;

/**
 * One rent charge for one tenant, in one room, for one month. It holds object
 * references to the Tenant and the Room it belongs to, and the list of payments
 * made against it.
 *
 * @author Nabongo Mark (Member 3 - Rent Payment Management)
 */
public class RentPayment extends Record {

    public static final long MAX_AMOUNT = 100_000_000L;

    // Encapsulation: everything is private; changes go through validated methods.
    private final Tenant tenant;
    private final Room room;
    private final YearMonth period;
    private long amountDue;
    private final List<PaymentTransaction> transactions = new ArrayList<>();

    public RentPayment(String id, Tenant tenant, Room room, YearMonth period, long amountDue)
            throws InvalidPaymentException {
        super(id);
        if (tenant == null) {
            throw new InvalidPaymentException("Tenant is required.");
        }
        if (room == null) {
            throw new InvalidPaymentException("Room is required.");
        }
        this.tenant = tenant;
        this.room = room;
        this.period = validatePeriod(period);
        this.amountDue = validateAmountDue(amountDue);
    }

    // ------------------------------------------------------ validation rules

    public static YearMonth validatePeriod(YearMonth period) throws InvalidPaymentException {
        if (period == null || period.getYear() < 2000 || period.getYear() > 2100) {
            throw new InvalidPaymentException("Rent period must be a month between 2000-01 and 2100-12 (format YYYY-MM).");
        }
        return period;
    }

    public static long validateAmountDue(long amount) throws InvalidPaymentException {
        if (amount <= 0) {
            throw new InvalidPaymentException("Amount due must be greater than zero.");
        }
        if (amount > MAX_AMOUNT) {
            throw new InvalidPaymentException("Amount due cannot be more than " + AppConfig.money(MAX_AMOUNT) + ".");
        }
        return amount;
    }

    // ---------------------------------------------------------------- getters

    public Tenant getTenant() { return tenant; }
    public Room getRoom() { return room; }
    public YearMonth getPeriod() { return period; }
    public long getAmountDue() { return amountDue; }

    public long getAmountPaid() {
        long total = 0;
        for (PaymentTransaction t : transactions) {
            total += t.getAmount();
        }
        return total;
    }

    public long getBalance() { return amountDue - getAmountPaid(); }

    public List<PaymentTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /** Date of the latest payment, or null if nothing has been paid yet. */
    public LocalDate getPaymentDate() {
        LocalDate latest = null;
        for (PaymentTransaction t : transactions) {
            if (latest == null || t.getDate().isAfter(latest)) {
                latest = t.getDate();
            }
        }
        return latest;
    }

    // ----------------------------------------------------------- status check

    public PaymentStatus getStatus() {
        long paid = getAmountPaid();
        if (paid >= amountDue) {
            return PaymentStatus.PAID;
        }
        if (paid <= 0) {
            return PaymentStatus.UNPAID;
        }
        return PaymentStatus.PARTIAL;
    }

    // ---------------------------------------------- record a payment (adds up)

    public void applyPayment(long amount, LocalDate date) throws InvalidPaymentException {
        PaymentTransaction t = new PaymentTransaction(amount, date);   // validates amount and date
        if (amount > getBalance()) {
            throw new InvalidPaymentException("Payment of " + AppConfig.money(amount)
                    + " is more than the balance of " + AppConfig.money(getBalance()) + ".");
        }
        transactions.add(t);
    }

    // ------------------------------------------------ update the record (fixes)

    /** Changes the amount due. It cannot be less than what was already paid. */
    public void changeAmountDue(long newAmountDue) throws InvalidPaymentException {
        validateAmountDue(newAmountDue);
        if (newAmountDue < getAmountPaid()) {
            throw new InvalidPaymentException("Amount due cannot be less than the "
                    + AppConfig.money(getAmountPaid()) + " already paid.");
        }
        this.amountDue = newAmountDue;
    }

    /** Corrects a mistake in one earlier payment (index starts at 1). */
    public void correctTransaction(int number, long newAmount, LocalDate newDate) throws InvalidPaymentException {
        int index = checkNumber(number);
        PaymentTransaction replacement = new PaymentTransaction(newAmount, newDate);
        long newTotal = getAmountPaid() - transactions.get(index).getAmount() + newAmount;
        if (newTotal > amountDue) {
            throw new InvalidPaymentException("That correction would make the total paid ("
                    + AppConfig.money(newTotal) + ") more than the amount due (" + AppConfig.money(amountDue) + ").");
        }
        transactions.set(index, replacement);
    }

    /** Deletes one wrongly entered payment (number starts at 1). */
    public void removeTransaction(int number) throws InvalidPaymentException {
        transactions.remove(checkNumber(number));
    }

    private int checkNumber(int number) throws InvalidPaymentException {
        if (number < 1 || number > transactions.size()) {
            throw new InvalidPaymentException("There is no payment number " + number
                    + " (this record has " + transactions.size() + ").");
        }
        return number - 1;
    }

    // ------------------------------------------------------- Record methods

    // Polymorphism: Record.toString() calls this overridden method.
    @Override
    public String describe() {
        LocalDate last = getPaymentDate();
        return String.format("%s | Tenant: %s (%s) | Room: %s | Period: %s | Due: %s | Paid: %s | Balance: %s | Last payment: %s | %s",
                getId(), tenant.getFullName(), tenant.getId(), room.getRoomNumber(), period,
                AppConfig.money(amountDue), AppConfig.money(getAmountPaid()), AppConfig.money(getBalance()),
                last == null ? "-" : last.toString(), getStatus());
    }

    @Override
    public String[] toFields() {
        List<String> parts = new ArrayList<>();
        for (PaymentTransaction t : transactions) {
            parts.add(t.toFileText());
        }
        return new String[] {getId(), tenant.getId(), room.getId(), period.toString(),
                String.valueOf(amountDue), String.join(";", parts)};
    }
}
