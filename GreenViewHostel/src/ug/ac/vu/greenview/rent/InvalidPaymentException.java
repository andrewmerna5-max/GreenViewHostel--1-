package ug.ac.vu.greenview.rent;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when a payment breaks a rule (zero amount, overpayment, future date,
 * duplicate rent record...).
 *
 * @author Nabongo Mark (Member 3 - Rent Payment Management)
 */
public class InvalidPaymentException extends HostelException {

    private static final long serialVersionUID = 1L;

    public InvalidPaymentException(String message) {
        super(message);
    }
}
