package ug.ac.vu.greenview.maintenance;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when a maintenance request breaks a rule (bad description, wrong
 * status change, request not found...).
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public class InvalidMaintenanceException extends HostelException {

    private static final long serialVersionUID = 1L;

    public InvalidMaintenanceException(String message) {
        super(message);
    }
}
