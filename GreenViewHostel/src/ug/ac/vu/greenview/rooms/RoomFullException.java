package ug.ac.vu.greenview.rooms;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when someone tries to put a tenant in a room that has no free bed.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class RoomFullException extends HostelException {

    private static final long serialVersionUID = 1L;

    public RoomFullException(String message) {
        super(message);
    }
}
