package ug.ac.vu.greenview.app;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.maintenance.IssueCategory;
import ug.ac.vu.greenview.maintenance.MaintenanceRequest;
import ug.ac.vu.greenview.maintenance.Priority;
import ug.ac.vu.greenview.rent.RentPayment;
import ug.ac.vu.greenview.tenants.EmergencyContact;
import ug.ac.vu.greenview.tenants.Tenant;
import ug.ac.vu.greenview.tenants.TenantType;
import ug.ac.vu.greenview.visitors.Visit;
import ug.ac.vu.greenview.visitors.Visitor;

/**
 * Loads a small set of FICTIONAL records so the system can be demonstrated
 * straight away. All names and numbers are invented. It goes through the same
 * service methods a real user would, so every rule is still checked.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class SampleData {

    private SampleData() { }

    public static boolean load(HostelSystem s) {
        if (s.rooms().getRoomCount() > 0 || s.tenants().getTenantCount() > 0) {
            System.out.println("Sample data was NOT loaded: the system already has rooms or tenants.");
            return false;
        }
        try {
            s.rooms().addRoom("A101", 2, 450_000, false);
            s.rooms().addRoom("A102", 1, 700_000, true);
            s.rooms().addRoom("B201", 4, 300_000, false);
            s.rooms().addRoom("B202", 3, 350_000, true);

            Tenant amina = s.tenants().registerTenant(TenantType.STUDENT, "Amina Nakato", "0700000001",
                    "Lakeview University", new EmergencyContact("Grace Nakato", "Mother", "0700000101"), "A101");
            Tenant brian = s.tenants().registerTenant(TenantType.WORKING, "Brian Okello", "0700000002",
                    "Sunrise Logistics Ltd", new EmergencyContact("Peter Okello", "Brother", "0700000102"), "A101");
            Tenant carol = s.tenants().registerTenant(TenantType.STUDENT, "Carol Achieng", "",
                    "Hilltop Institute", new EmergencyContact("Ruth Achieng", "Aunt", "0700000103"), "A102");
            Tenant david = s.tenants().registerTenant(TenantType.STUDENT, "David Mugisha", "0700000004",
                    "Lakeview University", new EmergencyContact("Sarah Mugisha", "Sister", "0700000104"), "B201");
            s.tenants().registerTenant(TenantType.WORKING, "Esther Namutebi", "0700000005",
                    "Greenfield Clinic", new EmergencyContact("John Namutebi", "Father", "0700000105"), "");

            YearMonth thisMonth = YearMonth.now();
            LocalDate today = LocalDate.now();
            RentPayment paid = s.rent().addPayment(amina.getId(), thisMonth, null);
            s.rent().recordPayment(paid.getId(), paid.getAmountDue(), today);
            RentPayment part = s.rent().addPayment(brian.getId(), thisMonth, null);
            s.rent().recordPayment(part.getId(), 200_000, today);
            s.rent().addPayment(carol.getId(), thisMonth, null);
            s.rent().addPayment(david.getId(), thisMonth, 320_000L);

            s.maintenance().recordComplaint(amina.getId(), IssueCategory.PLUMBING, Priority.HIGH,
                    "The tap in the room is leaking badly");
            MaintenanceRequest latch = s.maintenance().recordComplaint(brian.getId(), IssueCategory.SECURITY,
                    Priority.LOW, "The window latch is broken");
            s.maintenance().markCompleted(latch.getId(), today, "Latch replaced by the caretaker");
            MaintenanceRequest bulb = s.maintenance().recordRoomRequest("B201", "Samuel Kato",
                    IssueCategory.ELECTRICAL, Priority.MEDIUM, "Replace the broken ceiling light holder");
            s.maintenance().startRepair(bulb.getId());

            Visitor joan = s.visitors().registerVisitor("Joan Namuli", "0700000201");
            Visitor moses = s.visitors().registerVisitor("Moses Ssali", "");
            LocalDate yesterday = today.minusDays(1);
            Visit past = s.visitors().checkIn(joan.getId(), amina.getId(), yesterday, LocalTime.of(14, 0), "Brought food");
            s.visitors().checkOut(past.getId(), LocalTime.of(16, 30));
            s.visitors().checkIn(moses.getId(), brian.getId(), today,
                    LocalTime.now().truncatedTo(ChronoUnit.MINUTES), "Family visit");

            System.out.println("Sample data loaded: 4 rooms, 5 tenants, 4 rent records, 3 maintenance records, 2 visitors, 2 visits.");
            return true;
        } catch (HostelException e) {
            System.out.println("Sample data stopped part-way: " + e.getMessage());
            return false;
        }
    }
}
