package ug.ac.vu.greenview.tenants;

import java.util.List;

import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;
import ug.ac.vu.greenview.rooms.RoomService;

/**
 * The Tenants submenu.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public class TenantMenu extends ConsoleMenu {

    private final TenantService tenants;
    private final RoomService rooms;

    public TenantMenu(TenantService tenants, RoomService rooms) {
        this.tenants = tenants;
        this.rooms = rooms;
    }

    @Override
    protected String title() {
        return "TENANT MANAGEMENT";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Register a new tenant",
            "View all tenants",
            "Search for tenants",
            "Update tenant information",
            "Link tenant to a room (allocate / change room)",
            "Vacate tenant from their room",
            "Remove a tenant record",
            "Report: all tenants sorted by name",
            "Report: tenants by room",
            "Report: tenants without a room"
        };
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: register(); break;
            case 2: printList("All tenants", tenants.getAllTenants()); break;
            case 3: search(); break;
            case 4: update(); break;
            case 5: linkRoom(); break;
            case 6: vacate(); break;
            case 7: remove(); break;
            case 8: System.out.println(tenants.generateAllTenantsReport()); break;
            case 9: System.out.println(tenants.generateByRoomReport()); break;
            case 10: System.out.println(tenants.generateNoRoomReport()); break;
            default: break;
        }
    }

    private void register() throws HostelException {
        System.out.println("Use FICTIONAL names and phone numbers only.");
        TenantType type = InputHelper.readEnum("Tenant type:", TenantType.class);
        String name = InputHelper.readName("Full name: ");
        String phone = InputHelper.readOptionalPhone("Phone (optional, Enter to skip): ");
        String label = type == TenantType.STUDENT ? "Institution (e.g. Victoria University): "
                                                  : "Employer: ";
        String affiliation = InputHelper.readRequired(label, 60);
        System.out.println("Emergency contact:");
        String contactName = InputHelper.readName("  Name: ");
        String relationship = InputHelper.readRequired("  Relationship (e.g. Mother): ", 30);
        String contactPhone = InputHelper.readPhone("  Phone: ");
        EmergencyContact contact = new EmergencyContact(contactName, relationship, contactPhone);

        printList("Rooms with free beds", rooms.getAvailableRooms());
        String roomKey = "";
        while (true) {
            roomKey = InputHelper.readOptional("Room number or ID to allocate (Enter for none): ", 20);
            if (roomKey.isEmpty()) {
                break;
            }
            try {
                tenants.checkRoomAvailable(roomKey);
                break;
            } catch (HostelException e) {
                System.out.println("  Invalid: " + e.getMessage());
            }
        }
        Tenant tenant = tenants.registerTenant(type, name, phone, affiliation, contact, roomKey);
        System.out.println("Tenant registered: " + tenant.describe());
    }

    private void search() {
        String keyword = InputHelper.readRequired("Search (ID, name, institution/employer, phone or room): ", 40);
        List<Tenant> found = tenants.search(keyword);
        printList("Search results", found);
    }

    private void update() throws HostelException {
        Tenant tenant = tenants.requireTenant(InputHelper.readRequired("Tenant ID to update: ", 20));
        System.out.println("Current: " + tenant.describe());
        System.out.println("Emergency contact: " + tenant.getEmergencyContact());
        System.out.println("(Press Enter to keep a value.)");
        String name = InputHelper.readNameOrKeep("New full name [" + tenant.getFullName() + "]: ", tenant.getFullName());
        String phone = InputHelper.readPhoneOrKeep("New phone [" + (tenant.getPhone().isEmpty() ? "none" : tenant.getPhone())
                + "] (- to clear): ", tenant.getPhone());
        String affiliation = InputHelper.readTextOrKeep("New " + tenant.getAffiliationLabel().toLowerCase()
                + " [" + tenant.getAffiliation() + "]: ", 60, tenant.getAffiliation());
        EmergencyContact contact = tenant.getEmergencyContact();
        if (InputHelper.readYesNo("Change the emergency contact?")) {
            contact = new EmergencyContact(
                    InputHelper.readName("  Name: "),
                    InputHelper.readRequired("  Relationship: ", 30),
                    InputHelper.readPhone("  Phone: "));
        }
        Tenant updated = tenants.updateTenant(tenant.getId(), name, phone, affiliation, contact);
        System.out.println("Tenant updated: " + updated.describe());
    }

    private void linkRoom() throws HostelException {
        Tenant tenant = tenants.requireTenant(InputHelper.readRequired("Tenant ID: ", 20));
        System.out.println("Current: " + tenant.describe());
        printList("Rooms with free beds", rooms.getAvailableRooms());
        String roomKey = InputHelper.readRequired("Room number or ID: ", 20);
        tenants.assignRoom(tenant.getId(), roomKey);
        System.out.println("Done: " + tenant.describe());
    }

    private void vacate() throws HostelException {
        Tenant tenant = tenants.requireTenant(InputHelper.readRequired("Tenant ID: ", 20));
        tenants.vacateRoom(tenant.getId());
        System.out.println("Done: " + tenant.describe());
    }

    private void remove() throws HostelException {
        Tenant tenant = tenants.requireTenant(InputHelper.readRequired("Tenant ID to remove: ", 20));
        System.out.println(tenant.describe());
        System.out.println("Removing a tenant frees their bed and also deletes their rent and visit records.");
        if (!InputHelper.readYesNo("Permanently remove this tenant?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        tenants.removeTenant(tenant.getId());
        System.out.println("Tenant " + tenant.getId() + " removed.");
    }
}
