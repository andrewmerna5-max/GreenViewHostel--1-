package ug.ac.vu.greenview.rent;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.AppConfig;

/**
 * One single payment: an amount received on a date. A rent record can be paid
 * in several of these.
 *
 * @author Nabongo Mark (Member 3 - Rent Payment Management)
 */
public class PaymentTransaction {

    private final long amount;
    private final LocalDate date;

    public PaymentTransaction(long amount, LocalDate date) throws InvalidPaymentException {
        if (amount <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero.");
        }
        if (amount > RentPayment.MAX_AMOUNT) {
            throw new InvalidPaymentException("Payment amount is too large.");
        }
        if (date == null) {
            throw new InvalidPaymentException("Payment date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidPaymentException("Payment date cannot be in the future.");
        }
        this.amount = amount;
        this.date = date;
    }

    public long getAmount() { return amount; }
    public LocalDate getDate() { return date; }

    /** Text used in the data file, e.g. 2026-10-03~50000 */
    public String toFileText() {
        return date + "~" + amount;
    }

    public static PaymentTransaction fromFileText(String text) throws InvalidPaymentException {
        String[] parts = text.split("~");
        if (parts.length != 2) {
            throw new InvalidPaymentException("bad payment entry '" + text + "'");
        }
        try {
            return new PaymentTransaction(Long.parseLong(parts[1].trim()), LocalDate.parse(parts[0].trim()));
        } catch (RuntimeException e) {
            throw new InvalidPaymentException("bad payment entry '" + text + "'");
        }
    }

    @Override
    public String toString() {
        return AppConfig.money(amount) + " on " + date;
    }
}
