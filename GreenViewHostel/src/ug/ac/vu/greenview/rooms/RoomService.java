package ug.ac.vu.greenview.rooms;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.NaturalOrder;
import ug.ac.vu.greenview.core.RemovalListener;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.storage.RecordStore;

/**
 * Manages all rooms and room allocations in an ArrayList: add, find, update,
 * remove, allocate, release, availability and reports. Loads from file when
 * created and saves after every change.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public final class RoomService implements Reportable {

    /** Rooms sorted by room number, so A2 comes before A10. */
    public static final Comparator<Room> BY_NUMBER =
            Comparator.comparing(Room::getRoomNumber, NaturalOrder.INSTANCE);

    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<RoomAllocation> allocations = new ArrayList<>();
    private final RecordStore<Room> roomStore;
    private final RecordStore<RoomAllocation> allocationStore;
    private final List<RemovalListener<Room>> listeners = new ArrayList<>();

    public RoomService(Path dataDir) {
        roomStore = new RecordStore<>(dataDir.resolve("rooms.txt"), Room::fromFields);
        allocationStore = new RecordStore<>(dataDir.resolve("allocations.txt"), RoomAllocation::fromFields);
        load();
    }

    private void load() {
        for (Room r : roomStore.load()) {
            if (findByNumber(r.getRoomNumber()) != null) {
                System.out.println("WARNING: skipped room " + r.getId() + " - room number "
                        + r.getRoomNumber() + " is already used.");
                continue;
            }
            rooms.add(r);
        }
        for (RoomAllocation a : allocationStore.load()) {
            Room room = findById(a.getRoomId());
            if (room == null) {
                System.out.println("WARNING: skipped allocation " + a.getId() + " - its room no longer exists.");
                continue;
            }
            if (a.isActive()) {
                if (getActiveAllocation(a.getTenantId()) != null) {
                    System.out.println("WARNING: skipped allocation " + a.getId() + " - tenant already has a room.");
                    continue;
                }
                try {
                    room.addOccupant(a.getTenantId());
                } catch (HostelException e) {
                    System.out.println("WARNING: skipped allocation " + a.getId() + " - " + e.getMessage());
                    continue;
                }
            }
            allocations.add(a);
        }
    }

    @Override
    public String getModuleName() {
        return "Rooms";
    }

    public void addRemovalListener(RemovalListener<Room> listener) {
        listeners.add(listener);
    }

    private void saveRooms() {
        roomStore.save(rooms);
    }

    private void saveAllocations() {
        allocationStore.save(allocations);
    }

    // ------------------------------------------------------------------ add

    public Room addRoom(String number, int capacity, long monthlyRent, boolean ensuite) throws HostelException {
        String roomNumber = Room.validateNumber(number);
        Room.validateCapacity(capacity);
        Room.validateRent(monthlyRent);
        if (findByNumber(roomNumber) != null) {
            throw new InvalidRoomException("Room number " + roomNumber + " already exists.");
        }
        String id = IdGenerator.next('R');
        Room room = ensuite ? new EnsuiteRoom(id, roomNumber, capacity, monthlyRent)
                            : new Room(id, roomNumber, capacity, monthlyRent);
        rooms.add(room);
        saveRooms();
        return room;
    }

    // ----------------------------------------------------------------- find

    public Room findById(String id) {
        if (id == null) {
            return null;
        }
        for (Room r : rooms) {
            if (r.getId().equalsIgnoreCase(id.trim())) {
                return r;
            }
        }
        return null;
    }

    public Room findByNumber(String number) {
        if (number == null) {
            return null;
        }
        for (Room r : rooms) {
            if (r.getRoomNumber().equalsIgnoreCase(number.trim())) {
                return r;
            }
        }
        return null;
    }

    /** Finds a room by its ID (G01-R001) or by its room number (A101). Null if none. */
    public Room findRoom(String key) {
        Room r = findById(key);
        return r != null ? r : findByNumber(key);
    }

    public Room requireRoom(String key) throws InvalidRoomException {
        Room r = findRoom(key);
        if (r == null) {
            throw new InvalidRoomException("No room found with ID or number: " + (key == null ? "" : key.trim()));
        }
        return r;
    }

    public List<Room> getAllRooms() {
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(BY_NUMBER);
        return sorted;
    }

    public List<RoomAllocation> getAllAllocations() {
        return new ArrayList<>(allocations);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    // --------------------------------------------------------------- update

    public Room updateRoom(String key, String newNumber, int newCapacity, long newRent) throws HostelException {
        Room room = requireRoom(key);
        String number = Room.validateNumber(newNumber);
        Room other = findByNumber(number);
        if (other != null && other != room) {
            throw new InvalidRoomException("Room number " + number + " is already used by " + other.getId() + ".");
        }
        room.updateDetails(number, newCapacity, newRent);
        saveRooms();
        return room;
    }

    // --------------------------------------------------------------- remove

    public Room removeRoom(String key) throws HostelException {
        Room room = requireRoom(key);
        if (room.isOccupied()) {
            throw new InvalidRoomException("Room " + room.getRoomNumber() + " still has "
                    + room.getOccupantCount() + " tenant(s). Vacate them first.");
        }
        for (RemovalListener<Room> l : listeners) {
            l.canRemove(room);
        }
        rooms.remove(room);
        allocations.removeIf(a -> a.getRoomId().equals(room.getId()));
        saveRooms();
        saveAllocations();
        for (RemovalListener<Room> l : listeners) {
            l.onRemoved(room);
        }
        return room;
    }

    // ----------------------------------------------------------- allocation

    /**
     * Puts a tenant in a room. If the tenant already has a different room they
     * are moved. Everything is checked before anything is changed.
     */
    public RoomAllocation allocate(String roomKey, String tenantId, LocalDate date) throws HostelException {
        Room target = requireRoom(roomKey);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRoomException("Tenant ID cannot be empty.");
        }
        String tenant = tenantId.trim();
        RoomAllocation current = getActiveAllocation(tenant);
        if (current != null && current.getRoomId().equals(target.getId())) {
            throw new InvalidRoomException("Tenant " + tenant + " is already in room " + target.getRoomNumber() + ".");
        }
        if (target.isFull()) {
            throw new RoomFullException("Room " + target.getRoomNumber() + " is full ("
                    + target.getCapacity() + " of " + target.getCapacity() + " beds taken).");
        }
        LocalDate when = date == null ? LocalDate.now() : date;
        RoomAllocation allocation = new RoomAllocation(IdGenerator.next('A'), target.getId(), tenant, when);
        if (current != null) {
            endAllocation(current, when);
        }
        target.addOccupant(tenant);
        allocations.add(allocation);
        saveAllocations();
        return allocation;
    }

    /** Ends the tenant's current stay. Throws if they are not in a room. */
    public RoomAllocation release(String tenantId, LocalDate date) throws HostelException {
        RoomAllocation current = getActiveAllocation(tenantId);
        if (current == null) {
            throw new InvalidRoomException("Tenant " + (tenantId == null ? "" : tenantId.trim()) + " is not in any room.");
        }
        endAllocation(current, date == null ? LocalDate.now() : date);
        saveAllocations();
        return current;
    }

    private void endAllocation(RoomAllocation allocation, LocalDate date) throws HostelException {
        LocalDate end = date.isBefore(allocation.getDateAllocated()) ? allocation.getDateAllocated() : date;
        allocation.release(end);
        Room room = findById(allocation.getRoomId());
        if (room != null) {
            room.removeOccupant(allocation.getTenantId());
        }
    }

    public RoomAllocation getActiveAllocation(String tenantId) {
        if (tenantId == null) {
            return null;
        }
        for (RoomAllocation a : allocations) {
            if (a.isActive() && a.getTenantId().equalsIgnoreCase(tenantId.trim())) {
                return a;
            }
        }
        return null;
    }

    public Room getRoomOfTenant(String tenantId) {
        RoomAllocation a = getActiveAllocation(tenantId);
        return a == null ? null : findById(a.getRoomId());
    }

    /** Clean-up after loading: frees beds held by tenants that no longer exist. */
    public void releaseOrphans(Set<String> existingTenantIds) {
        boolean changed = false;
        for (RoomAllocation a : new ArrayList<>(allocations)) {
            if (a.isActive() && !containsIgnoreCase(existingTenantIds, a.getTenantId())) {
                try {
                    endAllocation(a, LocalDate.now());
                    changed = true;
                    System.out.println("WARNING: released room for unknown tenant " + a.getTenantId() + ".");
                } catch (HostelException e) {
                    System.out.println("WARNING: " + e.getMessage());
                }
            }
        }
        if (changed) {
            saveAllocations();
        }
    }

    private static boolean containsIgnoreCase(Set<String> ids, String id) {
        for (String s : ids) {
            if (s.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------- reports

    /** FILTERED + SORTED: rooms with a free bed, sorted by room number. */
    public List<Room> getAvailableRooms() {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.hasFreeBed()) {
                result.add(r);
            }
        }
        result.sort(BY_NUMBER);
        return result;
    }

    /** FILTERED + SORTED: rooms with at least one tenant, most crowded first. */
    public List<Room> getOccupiedRooms() {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isOccupied()) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingInt(Room::getOccupantCount).reversed().thenComparing(BY_NUMBER));
        return result;
    }

    public String generateAvailableReport() {
        List<Room> available = getAvailableRooms();
        StringBuilder sb = new StringBuilder("=== AVAILABLE ROOMS REPORT ===\n");
        int freeBeds = 0;
        for (Room r : available) {
            sb.append(r.describe()).append('\n');   // Room or EnsuiteRoom - polymorphic call
            freeBeds += r.getFreeBeds();
        }
        if (available.isEmpty()) {
            sb.append("No rooms with free beds.\n");
        }
        sb.append("Rooms: ").append(available.size()).append(" | Free beds: ").append(freeBeds);
        return sb.toString();
    }

    public String generateOccupiedReport() {
        List<Room> occupied = getOccupiedRooms();
        StringBuilder sb = new StringBuilder("=== OCCUPIED ROOMS REPORT ===\n");
        int people = 0;
        for (Room r : occupied) {
            sb.append(r.describe()).append("\n    Tenants: ")
              .append(String.join(", ", r.getOccupantIds())).append('\n');
            people += r.getOccupantCount();
        }
        if (occupied.isEmpty()) {
            sb.append("No occupied rooms.\n");
        }
        sb.append("Rooms: ").append(occupied.size()).append(" | Tenants housed: ").append(people);
        return sb.toString();
    }

    @Override
    public String generateReport() {
        int beds = 0;
        int taken = 0;
        for (Room r : rooms) {
            beds += r.getCapacity();
            taken += r.getOccupantCount();
        }
        String occupancy = beds == 0 ? "0" : String.format("%.1f", 100.0 * taken / beds);
        return "Rooms: " + rooms.size() + " | Beds: " + beds + " | Taken: " + taken
                + " | Occupancy: " + occupancy + "% | Rent range: "
                + (rooms.isEmpty() ? "n/a" : rentRange()) + "\n"
                + generateAvailableReport() + "\n\n" + generateOccupiedReport();
    }

    private String rentRange() {
        long min = Long.MAX_VALUE;
        long max = 0;
        for (Room r : rooms) {
            min = Math.min(min, r.getMonthlyRent());
            max = Math.max(max, r.getMonthlyRent());
        }
        return AppConfig.money(min) + " to " + AppConfig.money(max);
    }
}
