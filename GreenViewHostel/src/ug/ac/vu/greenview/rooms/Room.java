package ug.ac.vu.greenview.rooms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.Record;

/**
 * A hostel room with a room number, a capacity (number of beds) and a monthly
 * rent per tenant. Who is currently in the room is kept here too, so the room
 * can refuse to be over-filled.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class Room extends Record {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 8;
    public static final long MAX_RENT = 100_000_000L;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9-]{0,9}");

    private String roomNumber;
    private int capacity;
    private long monthlyRent;
    private final List<String> occupantIds = new ArrayList<>();

    public Room(String id, String roomNumber, int capacity, long monthlyRent) throws InvalidRoomException {
        super(id);
        this.roomNumber = validateNumber(roomNumber);
        this.capacity = validateCapacity(capacity);
        this.monthlyRent = validateRent(monthlyRent);
    }

    // ------------------------------------------------------ validation rules

    /** Returns the room number in capitals, or throws if it is not allowed. */
    public static String validateNumber(String number) throws InvalidRoomException {
        String n = number == null ? "" : number.trim().toUpperCase();
        if (!NUMBER_PATTERN.matcher(n).matches()) {
            throw new InvalidRoomException("Room number must be 1 to 10 letters, digits or '-' with no spaces (example: A101).");
        }
        return n;
    }

    public static int validateCapacity(int capacity) throws InvalidRoomException {
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new InvalidRoomException("Capacity must be from " + MIN_CAPACITY + " to " + MAX_CAPACITY + " beds.");
        }
        return capacity;
    }

    public static long validateRent(long rent) throws InvalidRoomException {
        if (rent <= 0 || rent > MAX_RENT) {
            throw new InvalidRoomException("Monthly rent must be more than 0 and at most " + AppConfig.money(MAX_RENT) + ".");
        }
        return rent;
    }

    // --------------------------------------------------------------- getters

    public String getRoomNumber() { return roomNumber; }
    public int getCapacity() { return capacity; }
    public long getMonthlyRent() { return monthlyRent; }
    public int getOccupantCount() { return occupantIds.size(); }
    public int getFreeBeds() { return capacity - occupantIds.size(); }
    public boolean isFull() { return getFreeBeds() <= 0; }
    public boolean isOccupied() { return !occupantIds.isEmpty(); }
    public boolean hasFreeBed() { return getFreeBeds() > 0; }

    public List<String> getOccupantIds() {
        return Collections.unmodifiableList(occupantIds);
    }

    /** "Standard" here; subclasses give their own category. */
    public String getCategory() { return "Standard"; }

    /** Code saved in the file so the right class is rebuilt on load. */
    public String getTypeCode() { return "STANDARD"; }

    // --------------------------------------------------------------- setters

    public final void setRoomNumber(String roomNumber) throws InvalidRoomException {
        this.roomNumber = validateNumber(roomNumber);
    }

    public final void setCapacity(int capacity) throws InvalidRoomException {
        validateCapacity(capacity);
        if (capacity < occupantIds.size()) {
            throw new InvalidRoomException("Capacity cannot be less than the " + occupantIds.size()
                    + " tenant(s) already in the room.");
        }
        this.capacity = capacity;
    }

    public final void setMonthlyRent(long monthlyRent) throws InvalidRoomException {
        this.monthlyRent = validateRent(monthlyRent);
    }

    /** Checks every new value first, so a bad value changes nothing at all. */
    public final void updateDetails(String newNumber, int newCapacity, long newRent) throws InvalidRoomException {
        String number = validateNumber(newNumber);
        validateCapacity(newCapacity);
        validateRent(newRent);
        if (newCapacity < occupantIds.size()) {
            throw new InvalidRoomException("Capacity cannot be less than the " + occupantIds.size()
                    + " tenant(s) already in the room.");
        }
        this.roomNumber = number;
        this.capacity = newCapacity;
        this.monthlyRent = newRent;
    }

    // ------------------------------------------------------------- occupants

    public void addOccupant(String tenantId) throws RoomFullException, InvalidRoomException {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRoomException("Tenant ID cannot be empty.");
        }
        if (occupantIds.contains(tenantId)) {
            throw new InvalidRoomException("Tenant " + tenantId + " is already in room " + roomNumber + ".");
        }
        if (isFull()) {
            throw new RoomFullException("Room " + roomNumber + " is full (" + capacity + " of " + capacity + " beds taken).");
        }
        occupantIds.add(tenantId);
    }

    public boolean removeOccupant(String tenantId) {
        return occupantIds.remove(tenantId);
    }

    // ------------------------------------------------------- Record methods

    @Override
    public String describe() {
        return String.format("%s | Room %s | %s | Beds: %d/%d taken | Rent: %s per tenant/month | %s",
                getId(), roomNumber, getCategory(), getOccupantCount(), capacity,
                AppConfig.money(monthlyRent), isFull() ? "FULL" : "AVAILABLE (" + getFreeBeds() + " free)");
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), getTypeCode(), roomNumber,
                String.valueOf(capacity), String.valueOf(monthlyRent)};
    }

    /** Rebuilds a Room or EnsuiteRoom from one line of the rooms file. */
    public static Room fromFields(String[] f) throws Exception {
        if (f.length < 5) {
            throw new IllegalArgumentException("expected 5 fields but found " + f.length);
        }
        int capacity = Integer.parseInt(f[3].trim());
        long rent = Long.parseLong(f[4].trim());
        if ("ENSUITE".equals(f[1])) {
            return new EnsuiteRoom(f[0], f[2], capacity, rent);
        }
        if ("STANDARD".equals(f[1])) {
            return new Room(f[0], f[2], capacity, rent);
        }
        throw new IllegalArgumentException("unknown room type '" + f[1] + "'");
    }
}
