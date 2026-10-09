package ug.ac.vu.greenview.visitors;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when a tenant already has the maximum number of visitors inside the
 * hostel at the same time.
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public class VisitorLimitExceededException extends HostelException {

    private static final long serialVersionUID = 1L;

    public VisitorLimitExceededException(String message) {
        super(message);
    }
}
