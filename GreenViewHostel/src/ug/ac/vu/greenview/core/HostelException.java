package ug.ac.vu.greenview.core;

/**
 * Parent of every business-rule exception in the system, so menus can catch
 * one type and show a friendly message instead of crashing.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public class HostelException extends Exception {

    private static final long serialVersionUID = 1L;

    public HostelException(String message) {
        super(message);
    }
}
