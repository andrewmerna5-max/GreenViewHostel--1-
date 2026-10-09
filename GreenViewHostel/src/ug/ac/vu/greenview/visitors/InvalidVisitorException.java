package ug.ac.vu.greenview.visitors;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when visitor data or a visit breaks a rule (bad name, visitor already
 * inside, check-out before check-in...).
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public class InvalidVisitorException extends HostelException {

    private static final long serialVersionUID = 1L;

    public InvalidVisitorException(String message) {
        super(message);
    }
}
