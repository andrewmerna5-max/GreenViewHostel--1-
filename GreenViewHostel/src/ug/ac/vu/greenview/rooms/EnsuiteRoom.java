package ug.ac.vu.greenview.rooms;

/**
 * A room with its own bathroom. It behaves like any other room but describes
 * and saves itself differently (polymorphism).
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class EnsuiteRoom extends Room {

    public EnsuiteRoom(String id, String roomNumber, int capacity, long monthlyRent) throws InvalidRoomException {
        super(id, roomNumber, capacity, monthlyRent);
    }

    @Override
    public String getCategory() {
        return "Ensuite (own bathroom)";
    }

    @Override
    public String getTypeCode() {
        return "ENSUITE";
    }
}
