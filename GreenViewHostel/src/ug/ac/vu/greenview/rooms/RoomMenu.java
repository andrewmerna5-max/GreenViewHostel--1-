package ug.ac.vu.greenview.rooms;

import java.util.List;

import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;

/**
 * The Rooms submenu.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public class RoomMenu extends ConsoleMenu {

    private final RoomService rooms;
    private final TenantLink tenants;

    public RoomMenu(RoomService rooms, TenantLink tenants) {
        this.rooms = rooms;
        this.tenants = tenants;
    }

    @Override
    protected String title() {
        return "ROOMS MANAGEMENT";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Register a new room",
            "View all rooms",
            "Check room availability",
            "Allocate a room to a tenant",
            "Vacate a tenant from their room",
            "Update room details",
            "Remove a room record",
            "Available rooms report",
            "Occupied rooms report"
        };
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: registerRoom(); break;
            case 2: viewAll(); break;
            case 3: checkAvailability(); break;
            case 4: allocate(); break;
            case 5: vacate(); break;
            case 6: update(); break;
            case 7: remove(); break;
            case 8: System.out.println(rooms.generateAvailableReport()); break;
            case 9: System.out.println(rooms.generateOccupiedReport()); break;
            default: break;
        }
    }

    private void registerRoom() throws HostelException {
        String number = InputHelper.readValid("Room number (e.g. A101): ", Room::validateNumber);
        int capacity = InputHelper.readInt("Capacity - number of beds (" + Room.MIN_CAPACITY + "-"
                + Room.MAX_CAPACITY + "): ", Room.MIN_CAPACITY, Room.MAX_CAPACITY);
        long rent = InputHelper.readLong("Monthly rent per tenant (UGX): ", 1, Room.MAX_RENT);
        boolean ensuite = InputHelper.readYesNo("Does the room have its own bathroom (ensuite)?");
        Room room = rooms.addRoom(number, capacity, rent, ensuite);
        System.out.println("Room registered: " + room.describe());
    }

    private void viewAll() {
        // Room and EnsuiteRoom objects are mixed in one list; each describes itself.
        List<Room> all = rooms.getAllRooms();
        printList("All rooms", all);
    }

    private void checkAvailability() throws HostelException {
        Room room = rooms.requireRoom(InputHelper.readRequired("Room number or ID: ", 20));
        System.out.println(room.describe());
        if (room.isOccupied()) {
            System.out.println("Tenants in this room:");
            for (String tenantId : room.getOccupantIds()) {
                String name = tenants.tenantName(tenantId);
                System.out.println("  - " + tenantId + (name == null ? "" : " (" + name + ")"));
            }
        }
        System.out.println(room.hasFreeBed()
                ? "Available: " + room.getFreeBeds() + " free bed(s)."
                : "This room is FULL.");
    }

    private void allocate() throws HostelException {
        printList("Rooms with free beds", rooms.getAvailableRooms());
        String tenantId = InputHelper.readRequired("Tenant ID to allocate: ", 20);
        if (tenants.tenantName(tenantId) == null) {
            throw new InvalidRoomException("No tenant found with ID: " + tenantId);
        }
        String roomKey = InputHelper.readRequired("Room number or ID: ", 20);
        tenants.assignRoom(tenantId, roomKey);
        System.out.println("Tenant " + tenantId.toUpperCase() + " is now in room "
                + rooms.requireRoom(roomKey).getRoomNumber() + ".");
    }

    private void vacate() throws HostelException {
        String tenantId = InputHelper.readRequired("Tenant ID to vacate: ", 20);
        tenants.vacateRoom(tenantId);
        System.out.println("Tenant " + tenantId.toUpperCase() + " has been moved out of their room.");
    }

    private void update() throws HostelException {
        Room room = rooms.requireRoom(InputHelper.readRequired("Room number or ID to update: ", 20));
        System.out.println("Current: " + room.describe());
        System.out.println("(Press Enter to keep a value.)");
        String number = room.getRoomNumber();
        String typed = InputHelper.readOptional("New room number [" + number + "]: ", 10);
        if (!typed.isEmpty()) {
            number = Room.validateNumber(typed);
        }
        int capacity = room.getCapacity();
        String capText = InputHelper.readOptional("New capacity [" + capacity + "]: ", 5);
        if (!capText.isEmpty()) {
            try {
                capacity = Integer.parseInt(capText);
            } catch (NumberFormatException e) {
                throw new InvalidRoomException("Capacity must be a whole number.");
            }
        }
        long rent = room.getMonthlyRent();
        Long newRent = InputHelper.readOptionalLong("New monthly rent [" + rent + "]: ", 1, Room.MAX_RENT);
        if (newRent != null) {
            rent = newRent;
        }
        Room updated = rooms.updateRoom(room.getId(), number, capacity, rent);
        System.out.println("Room updated: " + updated.describe());
    }

    private void remove() throws HostelException {
        Room room = rooms.requireRoom(InputHelper.readRequired("Room number or ID to remove: ", 20));
        System.out.println(room.describe());
        if (!InputHelper.readYesNo("Permanently remove this room record?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        rooms.removeRoom(room.getId());
        System.out.println("Room " + room.getRoomNumber() + " removed.");
    }
}
