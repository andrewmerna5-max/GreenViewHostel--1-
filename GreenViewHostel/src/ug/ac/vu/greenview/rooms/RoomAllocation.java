package ug.ac.vu.greenview.rooms;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Record;

/**
 * One stay of one tenant in one room, from the day they were allocated until
 * the day they left (or still active). Keeps the history of who lived where.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class RoomAllocation extends Record {

    private final String roomId;
    private final String tenantId;
    private final LocalDate dateAllocated;
    private LocalDate dateReleased;   // null while the tenant is still in the room

    public RoomAllocation(String id, String roomId, String tenantId, LocalDate dateAllocated)
            throws InvalidRoomException {
        super(id);
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new InvalidRoomException("Room ID cannot be empty.");
        }
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRoomException("Tenant ID cannot be empty.");
        }
        if (dateAllocated == null) {
            throw new InvalidRoomException("Allocation date is required.");
        }
        if (dateAllocated.isAfter(LocalDate.now())) {
            throw new InvalidRoomException("Allocation date cannot be in the future.");
        }
        this.roomId = roomId.trim();
        this.tenantId = tenantId.trim();
        this.dateAllocated = dateAllocated;
    }

    public String getRoomId() { return roomId; }
    public String getTenantId() { return tenantId; }
    public LocalDate getDateAllocated() { return dateAllocated; }
    public LocalDate getDateReleased() { return dateReleased; }
    public boolean isActive() { return dateReleased == null; }

    public void release(LocalDate date) throws InvalidRoomException {
        if (!isActive()) {
            throw new InvalidRoomException("This allocation has already ended.");
        }
        if (date == null || date.isBefore(dateAllocated)) {
            throw new InvalidRoomException("Release date cannot be before the allocation date.");
        }
        this.dateReleased = date;
    }

    @Override
    public String describe() {
        return String.format("%s | Room: %s | Tenant: %s | From: %s | To: %s",
                getId(), roomId, tenantId, dateAllocated, dateReleased == null ? "still in room" : dateReleased);
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), roomId, tenantId, dateAllocated.toString(),
                dateReleased == null ? "" : dateReleased.toString()};
    }

    public static RoomAllocation fromFields(String[] f) throws Exception {
        if (f.length < 5) {
            throw new IllegalArgumentException("expected 5 fields but found " + f.length);
        }
        RoomAllocation a = new RoomAllocation(f[0], f[1], f[2], LocalDate.parse(f[3].trim()));
        if (!f[4].trim().isEmpty()) {
            a.release(LocalDate.parse(f[4].trim()));
        }
        return a;
    }
}
