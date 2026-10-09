package ug.ac.vu.greenview.rooms;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when room data or a room operation breaks a rule.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class InvalidRoomException extends HostelException {

    private static final long serialVersionUID = 1L;

    public InvalidRoomException(String message) {
        super(message);
    }
}
