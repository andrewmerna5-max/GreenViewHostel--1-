package ug.ac.vu.greenview.maintenance;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;

/**
 * The Maintenance submenu.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public class MaintenanceMenu extends ConsoleMenu {

    private final MaintenanceService service;

    public MaintenanceMenu(MaintenanceService service) {
        this.service = service;
    }

    @Override
    protected String title() {
        return "MAINTENANCE MANAGEMENT";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Record a tenant complaint",
            "Record a room maintenance request",
            "View all maintenance records",
            "Search maintenance records",
            "Start a repair (mark In progress)",
            "Mark a request as completed",
            "Edit a request (category / priority / description)",
            "Remove a maintenance record",
            "Pending maintenance report",
            "Completed maintenance report"
        };
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: recordComplaint(); break;
            case 2: recordRoomRequest(); break;
            case 3: printList("All maintenance records", service.getAll()); break;
            case 4: search(); break;
            case 5: startRepair(); break;
            case 6: complete(); break;
            case 7: edit(); break;
            case 8: remove(); break;
            case 9: System.out.println(service.generatePendingReport()); break;
            case 10: System.out.println(service.generateCompletedReport()); break;
            default: break;
        }
    }

    private static String readDescription() {
        return InputHelper.readValid("Describe the problem (" + MaintenanceRequest.MIN_DESCRIPTION + "-"
                + MaintenanceRequest.MAX_DESCRIPTION + " characters): ", MaintenanceRequest::validateDescription);
    }

    private void recordComplaint() throws HostelException {
        String tenantId = InputHelper.readRequired("Tenant ID of the complaining tenant: ", 20);
        IssueCategory category = InputHelper.readEnum("Type of problem:", IssueCategory.class);
        Priority priority = InputHelper.readEnum("Priority:", Priority.class);
        String description = readDescription();
        TenantComplaint c = service.recordComplaint(tenantId, category, priority, description);
        System.out.println("Complaint recorded: " + c.describe());
    }

    private void recordRoomRequest() throws HostelException {
        String roomKey = InputHelper.readRequired("Room number or ID: ", 20);
        String staff = InputHelper.readName("Staff member raising the request: ");
        IssueCategory category = InputHelper.readEnum("Type of problem:", IssueCategory.class);
        Priority priority = InputHelper.readEnum("Priority:", Priority.class);
        String description = readDescription();
        RoomMaintenanceRequest r = service.recordRoomRequest(roomKey, staff, category, priority, description);
        System.out.println("Request recorded: " + r.describe());
    }

    private void search() {
        String keyword = InputHelper.readRequired("Search (ID, room, name, category, status, priority or words): ", 40);
        printList("Search results", service.search(keyword));
    }

    private void startRepair() throws HostelException {
        MaintenanceRequest r = service.startRepair(InputHelper.readRequired("Maintenance record ID: ", 20));
        System.out.println("Repair started: " + r.describe());
    }

    private void complete() throws HostelException {
        MaintenanceRequest r = service.requireRequest(InputHelper.readRequired("Maintenance record ID: ", 20));
        System.out.println(r.describe());
        LocalDate date = InputHelper.readDateOrToday("Date completed (YYYY-MM-DD, Enter for today): ");
        String notes = InputHelper.readOptional("Notes about the repair (optional): ", MaintenanceRequest.MAX_NOTES);
        MaintenanceRequest done = service.markCompleted(r.getId(), date, notes);
        System.out.println("Marked completed: " + done.describe());
    }

    private void edit() throws HostelException {
        MaintenanceRequest r = service.requireRequest(InputHelper.readRequired("Maintenance record ID: ", 20));
        System.out.println("Current: " + r.describe());
        if (r.isCompleted()) {
            throw new InvalidMaintenanceException("A completed request cannot be edited.");
        }
        IssueCategory category = InputHelper.readEnum("New type of problem:", IssueCategory.class);
        Priority priority = InputHelper.readEnum("New priority:", Priority.class);
        String description = readDescription();
        MaintenanceRequest updated = service.updateRequest(r.getId(), category, priority, description);
        System.out.println("Updated: " + updated.describe());
    }

    private void remove() throws HostelException {
        MaintenanceRequest r = service.requireRequest(InputHelper.readRequired("Maintenance record ID to remove: ", 20));
        System.out.println(r.describe());
        if (!InputHelper.readYesNo("Permanently remove this record?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        service.removeRequest(r.getId());
        System.out.println("Record " + r.getId() + " removed.");
    }
}
