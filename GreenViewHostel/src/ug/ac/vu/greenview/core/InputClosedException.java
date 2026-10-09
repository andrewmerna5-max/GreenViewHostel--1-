package ug.ac.vu.greenview.core;

/**
 * Thrown when the keyboard input stream ends (for example Ctrl+D / Ctrl+Z),
 * so the program can close down politely instead of looping forever.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("Input stream closed.");
    }
}
