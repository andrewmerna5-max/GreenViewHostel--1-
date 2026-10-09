package ug.ac.vu.greenview.visitors;

import java.time.LocalDate;
import java.time.LocalTime;

import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;

/**
 * The Visitors submenu.
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public class VisitorMenu extends ConsoleMenu {

    private final VisitorService service;

    public VisitorMenu(VisitorService service) {
        this.service = service;
    }

    @Override
    protected String title() {
        return "VISITORS MANAGEMENT";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Register a new visitor",
            "Check a visitor in (record the visit)",
            "Check a visitor out",
            "View all visitors and visits",
            "Search visitors",
            "Search visits",
            "Update visitor details",
            "Update a visit (date / check-in time / purpose)",
            "Remove a visit record",
            "Remove a visitor",
            "Report: visitors currently inside",
            "Report: visit history",
            "Report: visits on a date",
            "Report: visits to a tenant"
        };
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: register(); break;
            case 2: checkIn(); break;
            case 3: checkOut(); break;
            case 4: printList("All visitors and visits", service.getAllRecords()); break;   // mixed Visitor and Visit objects
            case 5: printList("Matching visitors", service.searchVisitors(
                    InputHelper.readRequired("Search visitors (ID, name or phone): ", 40))); break;
            case 6: printList("Matching visits", service.searchVisits(
                    InputHelper.readRequired("Search visits (ID, names, purpose or date): ", 40))); break;
            case 7: updateVisitor(); break;
            case 8: updateVisit(); break;
            case 9: removeVisit(); break;
            case 10: removeVisitor(); break;
            case 11: System.out.println(service.generateInsideReport()); break;
            case 12: System.out.println(service.generateHistoryReport()); break;
            case 13: System.out.println(service.generateDateReport(InputHelper.readDate("Date (YYYY-MM-DD): "))); break;
            case 14: System.out.println(service.generateTenantReport(InputHelper.readRequired("Tenant ID: ", 20))); break;
            default: break;
        }
    }

    private void register() throws HostelException {
        System.out.println("Use FICTIONAL names and phone numbers only.");
        String name = InputHelper.readName("Visitor's full name: ");
        String phone = InputHelper.readOptionalPhone("Phone (optional, Enter to skip): ");
        Visitor v = service.registerVisitor(name, phone);
        System.out.println("Visitor registered: " + v.describe());
    }

    private void checkIn() throws HostelException {
        Visitor visitor = service.requireVisitor(InputHelper.readRequired("Visitor ID: ", 20));
        String tenantId = InputHelper.readRequired("Tenant being visited (Tenant ID): ", 20);
        String purpose = InputHelper.readRequired("Purpose of visit: ", Visit.MAX_PURPOSE);
        LocalDate date = InputHelper.readDateOrToday("Visit date (YYYY-MM-DD, Enter for today): ");
        LocalTime time = InputHelper.readTimeOrNow("Check-in time (HH:mm, Enter for now): ");
        Visit visit = service.checkIn(visitor.getId(), tenantId, date, time, purpose);
        System.out.println("Checked in: " + visit.describe());
    }

    private void checkOut() throws HostelException {
        Visit visit = service.requireVisit(InputHelper.readRequired("Visit ID: ", 20));
        System.out.println(visit.describe());
        LocalTime time = InputHelper.readTimeOrNow("Check-out time (HH:mm, Enter for now): ");
        Visit done = service.checkOut(visit.getId(), time);
        System.out.println("Checked out: " + done.describe());
    }

    private void updateVisitor() throws HostelException {
        Visitor v = service.requireVisitor(InputHelper.readRequired("Visitor ID to update: ", 20));
        System.out.println("Current: " + v.describe());
        System.out.println("(Press Enter to keep a value.)");
        String name = InputHelper.readNameOrKeep("New name [" + v.getFullName() + "]: ", v.getFullName());
        String phone = InputHelper.readPhoneOrKeep("New phone [" + (v.getPhone().isEmpty() ? "none" : v.getPhone())
                + "] (- to clear): ", v.getPhone());
        Visitor updated = service.updateVisitor(v.getId(), name, phone);
        System.out.println("Visitor updated: " + updated.describe());
    }

    private void updateVisit() throws HostelException {
        Visit visit = service.requireVisit(InputHelper.readRequired("Visit ID to update: ", 20));
        System.out.println("Current: " + visit.describe());
        LocalDate date = InputHelper.readDateOrToday("Correct visit date (YYYY-MM-DD, Enter for today): ");
        LocalTime in = InputHelper.readTime("Correct check-in time (HH:mm): ");
        String purpose = InputHelper.readRequired("Purpose of visit: ", Visit.MAX_PURPOSE);
        Visit updated = service.updateVisit(visit.getId(), date, in, purpose);
        System.out.println("Visit updated: " + updated.describe());
    }

    private void removeVisit() throws HostelException {
        Visit visit = service.requireVisit(InputHelper.readRequired("Visit ID to remove: ", 20));
        System.out.println(visit.describe());
        if (!InputHelper.readYesNo("Permanently remove this visit record?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        service.removeVisit(visit.getId());
        System.out.println("Visit " + visit.getId() + " removed.");
    }

    private void removeVisitor() throws HostelException {
        Visitor v = service.requireVisitor(InputHelper.readRequired("Visitor ID to remove: ", 20));
        System.out.println(v.describe());
        System.out.println("This also deletes the visitor's visit history.");
        if (!InputHelper.readYesNo("Permanently remove this visitor?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        service.removeVisitor(v.getId());
        System.out.println("Visitor " + v.getId() + " removed.");
    }
}
