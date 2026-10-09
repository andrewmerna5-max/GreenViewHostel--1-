package ug.ac.vu.greenview.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import ug.ac.vu.greenview.app.HostelSystem;
import ug.ac.vu.greenview.app.MainMenu;
import ug.ac.vu.greenview.app.SampleData;
import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.InputClosedException;
import ug.ac.vu.greenview.core.InputHelper;
import ug.ac.vu.greenview.core.NaturalOrder;
import ug.ac.vu.greenview.maintenance.IssueCategory;
import ug.ac.vu.greenview.maintenance.MaintenanceRequest;
import ug.ac.vu.greenview.maintenance.Priority;
import ug.ac.vu.greenview.rent.PaymentStatus;
import ug.ac.vu.greenview.rent.RentPayment;
import ug.ac.vu.greenview.rooms.Room;
import ug.ac.vu.greenview.storage.FileStorage;
import ug.ac.vu.greenview.tenants.EmergencyContact;
import ug.ac.vu.greenview.tenants.Tenant;
import ug.ac.vu.greenview.tenants.TenantType;
import ug.ac.vu.greenview.visitors.Visit;
import ug.ac.vu.greenview.visitors.Visitor;
import ug.ac.vu.greenview.visitors.VisitorService;

/**
 * The system-wide automated test suite. Runs every test case, prints the expected
 * and actual result of each one, and ends with a pass/fail total. It needs no
 * extra libraries: just run this class.
 *
 * @author Bijeneza Ndege Fidele (Member 7 - File Storage and Testing)
 */
public final class SystemTests {

    private interface Action {
        void run() throws Exception;
    }

    private static PrintStream realOut;
    private static final List<String[]> RESULTS = new ArrayList<>();
    private static int passed;
    private static int failed;

    private static final String[] NAMES = {"Aaron Test", "Bella Test", "Carl Test", "Dina Test", "Evan Test", "Faith Test"};

    private SystemTests() { }

    // ===================================================================== helpers

    private static void test(String id, String module, String description, String expected, Callable<String> body) {
        String actual;
        try {
            actual = body.call();
        } catch (Throwable e) {
            actual = "CRASH: " + e;
        }
        boolean ok = expected.equals(actual);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        RESULTS.add(new String[] {id, module, description, expected, actual, ok ? "PASS" : "FAIL"});
    }

    /** Returns "rejected: ExceptionName" if the action throws, otherwise "accepted". */
    private static String outcome(Action action) {
        try {
            action.run();
            return "accepted";
        } catch (Exception e) {
            return "rejected: " + e.getClass().getSimpleName();
        }
    }

    private static Path newDir() throws Exception {
        return Files.createTempDirectory("greenview-test");
    }

    private static HostelSystem start(Path dir) {
        IdGenerator.reset();
        return new HostelSystem(dir);
    }

    private static EmergencyContact kin() throws Exception {
        return new EmergencyContact("Kin Person", "Mother", "0700000900");
    }

    private static Tenant addTenant(HostelSystem s, int n, String roomKey) throws Exception {
        return s.tenants().registerTenant(TenantType.STUDENT, NAMES[n], "", "Test College", kin(), roomKey);
    }

    private static String quietly(Callable<String> body) throws Exception {
        return body.call();
    }

    private static String runInput(String text, Callable<String> body) throws Exception {
        InputHelper.setInput(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        return body.call();
    }

    // ======================================================================== main

    public static void main(String[] args) throws Exception {
        realOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));   // hide program chatter during tests

        roomTests();
        tenantTests();
        rentTests();
        maintenanceTests();
        visitorTests();
        storageTests();
        coreTests();
        stressTests();

        System.setOut(realOut);
        System.out.println("GREEN VIEW HOSTEL - SYSTEM TEST RESULTS");
        System.out.println("=======================================");
        for (String[] r : RESULTS) {
            System.out.println(r[0] + " | " + r[1] + " | " + r[2]);
            System.out.println("      expected: " + r[3]);
            System.out.println("      actual:   " + r[4] + "   [" + r[5] + "]");
        }
        System.out.println("=======================================");
        System.out.println("Total: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ============================================================= Rooms (Member 1)

    private static void roomTests() {
        test("ROOM-01", "Rooms", "Register a valid room", "G01-R001 A101 cap2", () -> {
            HostelSystem s = start(newDir());
            Room r = s.rooms().addRoom("a101", 2, 450_000, false);
            return r.getId() + " " + r.getRoomNumber() + " cap" + r.getCapacity();
        });
        test("ROOM-02", "Rooms", "Duplicate room number is rejected", "rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A101", 2, 450_000, false);
            return outcome(() -> s.rooms().addRoom("a101", 3, 100_000, true));
        });
        test("ROOM-03", "Rooms", "Capacity 0 and capacity 9 are rejected", "rejected: InvalidRoomException/rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.rooms().addRoom("B1", 0, 100_000, false)) + "/"
                    + outcome(() -> s.rooms().addRoom("B2", 9, 100_000, false));
        });
        test("ROOM-04", "Rooms", "Zero / negative rent and bad room number are rejected",
                "rejected: InvalidRoomException/rejected: InvalidRoomException/rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.rooms().addRoom("B1", 2, 0, false)) + "/"
                    + outcome(() -> s.rooms().addRoom("B2", 2, -5, false)) + "/"
                    + outcome(() -> s.rooms().addRoom("room 1!", 2, 1000, false));
        });
        test("ROOM-05", "Rooms", "Allocating a tenant to a full room throws RoomFullException", "rejected: RoomFullException", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 1, 100_000, false);
            Tenant a = addTenant(s, 0, "A1");
            Tenant b = addTenant(s, 1, "");
            return outcome(() -> s.tenants().assignRoom(b.getId(), "A1")) + (a.hasRoom() ? "" : " BAD");
        });
        test("ROOM-06", "Rooms", "A room with tenants cannot be removed", "rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            addTenant(s, 0, "A1");
            return outcome(() -> s.rooms().removeRoom("A1"));
        });
        test("ROOM-07", "Rooms", "Capacity cannot drop below occupants, and nothing changes", "rejected: InvalidRoomException A1 cap2", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            addTenant(s, 0, "A1");
            addTenant(s, 1, "A1");
            String o = outcome(() -> s.rooms().updateRoom("A1", "Z9", 1, 5000));
            Room r = s.rooms().findRoom("G01-R001");
            return o + " " + r.getRoomNumber() + " cap" + r.getCapacity();
        });
        test("ROOM-08", "Rooms", "Update room details", "A2 cap3 rent500000", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            Room r = s.rooms().updateRoom("A1", "a2", 3, 500_000);
            return r.getRoomNumber() + " cap" + r.getCapacity() + " rent" + r.getMonthlyRent();
        });
        test("ROOM-09", "Rooms", "Available rooms report is filtered (full rooms hidden) and naturally sorted", "A2,A10", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A10", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, true);
            s.rooms().addRoom("A3", 1, 100_000, false);
            addTenant(s, 0, "A3");
            StringBuilder sb = new StringBuilder();
            for (Room r : s.rooms().getAvailableRooms()) {
                sb.append(sb.length() == 0 ? "" : ",").append(r.getRoomNumber());
            }
            return sb.toString();
        });
        test("ROOM-10", "Rooms", "Moving a tenant frees the old bed and fills the new one", "old0 new1", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, false);
            Tenant t = addTenant(s, 0, "A1");
            s.tenants().assignRoom(t.getId(), "A2");
            return "old" + s.rooms().findRoom("A1").getOccupantCount() + " new" + s.rooms().findRoom("A2").getOccupantCount();
        });
        test("ROOM-11", "Rooms", "Putting a tenant in the room they are already in is rejected", "rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            Tenant t = addTenant(s, 0, "A1");
            return outcome(() -> s.tenants().assignRoom(t.getId(), "A1"));
        });
        test("ROOM-12", "Rooms", "Rooms and occupancy survive a restart (Room and EnsuiteRoom classes)", "A1:Room:1 A2:EnsuiteRoom:0", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, true);
            addTenant(s, 0, "A1");
            HostelSystem s2 = start(dir);
            Room a = s2.rooms().findRoom("A1");
            Room b = s2.rooms().findRoom("A2");
            return a.getRoomNumber() + ":" + a.getClass().getSimpleName() + ":" + a.getOccupantCount() + " "
                    + b.getRoomNumber() + ":" + b.getClass().getSimpleName() + ":" + b.getOccupantCount();
        });
    }

    // ========================================================== Tenants (Member 2)

    private static void tenantTests() {
        test("TEN-01", "Tenants", "Register a valid tenant linked to a room", "G01-T001 room A1", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            Tenant t = addTenant(s, 0, "A1");
            return t.getId() + " room " + t.getRoom().getRoomNumber();
        });
        test("TEN-02", "Tenants", "Invalid names are rejected (digits, empty, 1 letter)",
                "rejected: InvalidTenantException/rejected: InvalidTenantException/rejected: InvalidTenantException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.tenants().registerTenant(TenantType.STUDENT, "Amina123", "", "College", kin(), ""))
                    + "/" + outcome(() -> s.tenants().registerTenant(TenantType.STUDENT, "", "", "College", kin(), ""))
                    + "/" + outcome(() -> s.tenants().registerTenant(TenantType.STUDENT, "A", "", "College", kin(), ""));
        });
        test("TEN-03", "Tenants", "Invalid phone numbers are rejected", "rejected: InvalidTenantException/rejected: InvalidTenantException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.tenants().registerTenant(TenantType.STUDENT, "Good Name", "12ab", "College", kin(), ""))
                    + "/" + outcome(() -> s.tenants().registerTenant(TenantType.STUDENT, "Good Name", "123", "College", kin(), ""));
        });
        test("TEN-04", "Tenants", "Duplicate tenant is rejected", "rejected: InvalidTenantException", () -> {
            HostelSystem s = start(newDir());
            addTenant(s, 0, "");
            return outcome(() -> addTenant(s, 0, ""));
        });
        test("TEN-05", "Tenants", "Registering into a full room fails and registers nobody", "rejected: RoomFullException count=1", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 1, 100_000, false);
            addTenant(s, 0, "A1");
            String o = outcome(() -> addTenant(s, 1, "A1"));
            return o + " count=" + s.tenants().getTenantCount();
        });
        test("TEN-06", "Tenants", "Registering into a room that does not exist fails", "rejected: InvalidRoomException count=0", () -> {
            HostelSystem s = start(newDir());
            String o = outcome(() -> addTenant(s, 0, "ZZ99"));
            return o + " count=" + s.tenants().getTenantCount();
        });
        test("TEN-07", "Tenants", "Search finds by name part, by room number, and nothing for junk", "1/1/0", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            addTenant(s, 0, "A1");
            addTenant(s, 1, "");
            return s.tenants().search("aaron").size() + "/" + s.tenants().search("a1").size() + "/" + s.tenants().search("zzzz").size();
        });
        test("TEN-08", "Tenants", "Update tenant details; bad value changes nothing", "Aaron Test | Better Name", () -> {
            HostelSystem s = start(newDir());
            Tenant t = addTenant(s, 0, "");
            String before = t.getFullName();
            String bad = outcome(() -> s.tenants().updateTenant(t.getId(), "Bad1", "", "College", kin()));
            s.tenants().updateTenant(t.getId(), "Better Name", "", "College", kin());
            return (bad.startsWith("rejected") ? before : "BAD") + " | " + t.getFullName();
        });
        test("TEN-09", "Tenants", "Removing a tenant frees their bed", "bed free=2 count=0", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            Tenant t = addTenant(s, 0, "A1");
            s.tenants().removeTenant(t.getId());
            return "bed free=" + s.rooms().findRoom("A1").getFreeBeds() + " count=" + s.tenants().getTenantCount();
        });
        test("TEN-10", "Tenants", "Vacating a tenant who has no room is rejected", "rejected: InvalidRoomException", () -> {
            HostelSystem s = start(newDir());
            Tenant t = addTenant(s, 0, "");
            return outcome(() -> s.tenants().vacateRoom(t.getId()));
        });
        test("TEN-11", "Tenants", "Removing an unknown tenant is rejected", "rejected: InvalidTenantException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.tenants().removeTenant("G01-T999"));
        });
        test("TEN-12", "Tenants", "Tenants survive a restart with room link and correct subclass", "StudentTenant room A1 | WorkingTenant no room", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            addTenant(s, 0, "A1");
            s.tenants().registerTenant(TenantType.WORKING, "Worker Test", "0700000123", "Some Firm", kin(), "");
            HostelSystem s2 = start(dir);
            Tenant a = s2.tenants().findTenant("G01-T001");
            Tenant b = s2.tenants().findTenant("G01-T002");
            return a.getClass().getSimpleName() + " room " + a.getRoom().getRoomNumber() + " | "
                    + b.getClass().getSimpleName() + (b.hasRoom() ? " room" : " no room");
        });
    }

    // ===================================================== Rent payments (Member 3)

    private static HostelSystem rentFixture(Path dir) throws Exception {
        HostelSystem s = start(dir);
        s.rooms().addRoom("A1", 2, 400_000, false);
        addTenant(s, 0, "A1");
        return s;
    }

    private static void rentTests() {
        test("RENT-01", "Rent", "Rent record uses the room's rent by default and is linked to tenant and room",
                "G01-P001 due400000 UNPAID A1 Aaron Test", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), null);
            return p.getId() + " due" + p.getAmountDue() + " " + p.getStatus() + " " + p.getRoom().getRoomNumber()
                    + " " + p.getTenant().getFullName();
        });
        test("RENT-02", "Rent", "Status moves UNPAID -> PARTIAL -> PAID", "UNPAID/PARTIAL/PAID", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 300_000L);
            String a = p.getStatus().toString();
            s.rent().recordPayment(p.getId(), 100_000, LocalDate.now());
            String b = s.rent().checkStatus(p.getId()).toString();
            s.rent().recordPayment(p.getId(), 200_000, LocalDate.now());
            return a + "/" + b + "/" + s.rent().checkStatus(p.getId());
        });
        test("RENT-03", "Rent", "Overpayment is rejected and nothing is recorded", "rejected: InvalidPaymentException paid=0", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 100_000L);
            String o = outcome(() -> s.rent().recordPayment(p.getId(), 100_001, LocalDate.now()));
            return o + " paid=" + p.getAmountPaid();
        });
        test("RENT-04", "Rent", "Zero, negative and future-dated payments are rejected",
                "rejected: InvalidPaymentException/rejected: InvalidPaymentException/rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 100_000L);
            return outcome(() -> s.rent().recordPayment(p.getId(), 0, LocalDate.now())) + "/"
                    + outcome(() -> s.rent().recordPayment(p.getId(), -50, LocalDate.now())) + "/"
                    + outcome(() -> s.rent().recordPayment(p.getId(), 10, LocalDate.now().plusDays(1)));
        });
        test("RENT-05", "Rent", "Duplicate rent record for same tenant, room and month is rejected", "rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), null);
            return outcome(() -> s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), null));
        });
        test("RENT-06", "Rent", "Tenant without a room cannot be charged rent", "rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            Tenant t = addTenant(s, 1, "");
            return outcome(() -> s.rent().addPayment(t.getId(), YearMonth.of(2026, 10), null));
        });
        test("RENT-07", "Rent", "Unknown tenant and absurd month are rejected", "rejected: InvalidTenantException/rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            return outcome(() -> s.rent().addPayment("G01-T404", YearMonth.of(2026, 10), null)) + "/"
                    + outcome(() -> s.rent().addPayment("G01-T001", YearMonth.of(1999, 1), null));
        });
        test("RENT-08", "Rent", "Amount due above the limit or zero is rejected", "rejected: InvalidPaymentException/rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            return outcome(() -> s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 0L)) + "/"
                    + outcome(() -> s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 999_999_999_999L));
        });
        test("RENT-09", "Rent", "Amount due cannot be set below what is already paid", "rejected: InvalidPaymentException due=200000", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 200_000L);
            s.rent().recordPayment(p.getId(), 150_000, LocalDate.now());
            String o = outcome(() -> s.rent().updateAmountDue(p.getId(), 100_000));
            return o + " due=" + p.getAmountDue();
        });
        test("RENT-10", "Rent", "Correct a payment, then delete a payment", "paid=120000 paid=0", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 200_000L);
            s.rent().recordPayment(p.getId(), 150_000, LocalDate.now());
            s.rent().correctPayment(p.getId(), 1, 120_000, LocalDate.now());
            String a = "paid=" + p.getAmountPaid();
            s.rent().deletePaymentEntry(p.getId(), 1);
            return a + " paid=" + p.getAmountPaid();
        });
        test("RENT-11", "Rent", "Correcting a payment above the amount due is rejected; bad payment number is rejected",
                "rejected: InvalidPaymentException/rejected: InvalidPaymentException", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 200_000L);
            s.rent().recordPayment(p.getId(), 100_000, LocalDate.now());
            return outcome(() -> s.rent().correctPayment(p.getId(), 1, 300_000, LocalDate.now())) + "/"
                    + outcome(() -> s.rent().deletePaymentEntry(p.getId(), 5));
        });
        test("RENT-12", "Rent", "Paid and unpaid reports split the records correctly", "paid=1 unpaid=2", () -> {
            HostelSystem s = rentFixture(newDir());
            Tenant b = addTenant(s, 1, "A1");
            RentPayment p1 = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 100_000L);
            s.rent().recordPayment(p1.getId(), 100_000, LocalDate.now());
            s.rent().addPayment("G01-T001", YearMonth.of(2026, 11), 100_000L);
            s.rent().addPayment(b.getId(), YearMonth.of(2026, 10), 100_000L);
            int paid = s.rent().generatePaidReport().split("G01-P0").length - 1;
            int unpaid = s.rent().generateUnpaidReport().split("G01-P0").length - 1;
            return "paid=" + paid + " unpaid=" + unpaid;
        });
        test("RENT-13", "Rent", "Tenant who owes rent cannot be removed; after paying they can, and their rent records go",
                "rejected: InvalidPaymentException then removed rentRecords=0", () -> {
            HostelSystem s = rentFixture(newDir());
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 100_000L);
            String first = outcome(() -> s.tenants().removeTenant("G01-T001"));
            s.rent().recordPayment(p.getId(), 100_000, LocalDate.now());
            s.tenants().removeTenant("G01-T001");
            return first + " then removed rentRecords=" + s.rent().getCount();
        });
        test("RENT-14", "Rent", "Rent records (multiple payments) survive a restart and re-link to tenant and room",
                "PARTIAL paid=150000 Aaron Test A1", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            RentPayment p = s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), 200_000L);
            s.rent().recordPayment(p.getId(), 100_000, LocalDate.now().minusDays(3));
            s.rent().recordPayment(p.getId(), 50_000, LocalDate.now());
            HostelSystem s2 = start(dir);
            RentPayment q = s2.rent().findById("G01-P001");
            return q.getStatus() + " paid=" + q.getAmountPaid() + " " + q.getTenant().getFullName() + " " + q.getRoom().getRoomNumber();
        });
        test("RENT-15", "Rent", "Removing a rent record", "removed count=0", () -> {
            HostelSystem s = rentFixture(newDir());
            s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), null);
            s.rent().removePayment("G01-P001");
            return "removed count=" + s.rent().getCount();
        });
    }

    // ================================================== Maintenance (Member 4)

    private static void maintenanceTests() {
        test("MAINT-01", "Maintenance", "Record a tenant complaint (room taken from the tenant) and a staff room request",
                "G01-M001 A1 Tenant complaint | G01-M002 A1 Room request", () -> {
            HostelSystem s = rentFixture(newDir());
            MaintenanceRequest c = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            MaintenanceRequest r = s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.ELECTRICAL, Priority.LOW, "Replace the old bulb");
            return c.getId() + " " + c.getRoom().getRoomNumber() + " " + c.getRequestType() + " | "
                    + r.getId() + " " + r.getRoom().getRoomNumber() + " " + r.getRequestType();
        });
        test("MAINT-02", "Maintenance", "Too-short and over-long descriptions are rejected", "rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException", () -> {
            HostelSystem s = rentFixture(newDir());
            String longText = "x".repeat(201);
            return outcome(() -> s.maintenance().recordComplaint("G01-T001", IssueCategory.OTHER, Priority.LOW, "abc")) + "/"
                    + outcome(() -> s.maintenance().recordComplaint("G01-T001", IssueCategory.OTHER, Priority.LOW, longText));
        });
        test("MAINT-03", "Maintenance", "Complaint from a tenant without a room, and request for unknown room, are rejected",
                "rejected: InvalidMaintenanceException/rejected: InvalidRoomException", () -> {
            HostelSystem s = rentFixture(newDir());
            Tenant t = addTenant(s, 1, "");
            return outcome(() -> s.maintenance().recordComplaint(t.getId(), IssueCategory.OTHER, Priority.LOW, "Something is broken"))
                    + "/" + outcome(() -> s.maintenance().recordRoomRequest("NOPE", "Staff Person", IssueCategory.OTHER, Priority.LOW, "Something is broken"));
        });
        test("MAINT-04", "Maintenance", "Status flow Pending -> In progress -> Completed; start twice is rejected",
                "Pending/In progress/rejected: InvalidMaintenanceException/Completed", () -> {
            HostelSystem s = rentFixture(newDir());
            MaintenanceRequest m = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            String a = m.getStatus().toString();
            s.maintenance().startRepair(m.getId());
            String b = m.getStatus().toString();
            String c = outcome(() -> s.maintenance().startRepair(m.getId()));
            s.maintenance().markCompleted(m.getId(), LocalDate.now(), "Fixed");
            return a + "/" + b + "/" + c + "/" + m.getStatus();
        });
        test("MAINT-05", "Maintenance", "Completion date in the future or before the report date is rejected",
                "rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException", () -> {
            HostelSystem s = rentFixture(newDir());
            MaintenanceRequest m = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            return outcome(() -> s.maintenance().markCompleted(m.getId(), LocalDate.now().plusDays(1), "")) + "/"
                    + outcome(() -> s.maintenance().markCompleted(m.getId(), LocalDate.now().minusDays(1), ""));
        });
        test("MAINT-06", "Maintenance", "A completed request cannot be completed again or edited",
                "rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException", () -> {
            HostelSystem s = rentFixture(newDir());
            MaintenanceRequest m = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            s.maintenance().markCompleted(m.getId(), LocalDate.now(), "Done");
            return outcome(() -> s.maintenance().markCompleted(m.getId(), LocalDate.now(), "Again")) + "/"
                    + outcome(() -> s.maintenance().updateRequest(m.getId(), IssueCategory.OTHER, Priority.LOW, "Changed my mind"));
        });
        test("MAINT-07", "Maintenance", "Edit an open request", "Electrical Urgent", () -> {
            HostelSystem s = rentFixture(newDir());
            MaintenanceRequest m = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            s.maintenance().updateRequest(m.getId(), IssueCategory.ELECTRICAL, Priority.URGENT, "Sparks from the socket");
            return m.getCategory() + " " + m.getPriority();
        });
        test("MAINT-08", "Maintenance", "Pending report is filtered (no completed) and sorted most-urgent first", "M003,M001 completed excluded", () -> {
            HostelSystem s = rentFixture(newDir());
            s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.LOW, "Paint the wall soon");
            MaintenanceRequest done = s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.URGENT, "Door will not lock");
            s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.URGENT, "Fire alarm is dead");
            s.maintenance().markCompleted(done.getId(), LocalDate.now(), "");
            String report = s.maintenance().generatePendingReport();
            int first = report.indexOf("G01-M003");
            int second = report.indexOf("G01-M001");
            boolean order = first > 0 && second > first && !report.contains("G01-M002");
            return order ? "M003,M001 completed excluded" : "WRONG ORDER: " + report;
        });
        test("MAINT-09", "Maintenance", "Search finds by word, room and status", "1/2/1", () -> {
            HostelSystem s = rentFixture(newDir());
            s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            MaintenanceRequest b = s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.LOW, "Paint the wall soon");
            s.maintenance().startRepair(b.getId());
            return s.maintenance().search("leaking").size() + "/" + s.maintenance().search("a1").size() + "/" + s.maintenance().search("in progress").size();
        });
        test("MAINT-10", "Maintenance", "Room with an open request cannot be removed; after completion it can (history goes with it)",
                "rejected: InvalidMaintenanceException then removed records=0", () -> {
            HostelSystem s = start(newDir());
            s.rooms().addRoom("A1", 2, 100_000, false);
            MaintenanceRequest m = s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.LOW, "Paint the wall soon");
            String first = outcome(() -> s.rooms().removeRoom("A1"));
            s.maintenance().markCompleted(m.getId(), LocalDate.now(), "");
            s.rooms().removeRoom("A1");
            return first + " then removed records=" + s.maintenance().getCount();
        });
        test("MAINT-11", "Maintenance", "Removing a tenant keeps their complaint history with the name",
                "kept=1 by Aaron Test tenantLink=none", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            s.tenants().removeTenant("G01-T001");
            HostelSystem s2 = start(dir);
            MaintenanceRequest m = s2.maintenance().findById("G01-M001");
            return "kept=" + s2.maintenance().getCount() + " by " + m.getRaisedBy()
                    + " tenantLink=" + (m.getRaisedByTenantId().isEmpty() ? "none" : "STILL LINKED");
        });
        test("MAINT-12", "Maintenance", "Maintenance records survive a restart with status, notes and subclass",
                "TenantComplaint Completed Fixed | RoomMaintenanceRequest In progress", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            MaintenanceRequest a = s.maintenance().recordComplaint("G01-T001", IssueCategory.PLUMBING, Priority.HIGH, "Leaking tap in room");
            s.maintenance().markCompleted(a.getId(), LocalDate.now(), "Fixed");
            MaintenanceRequest b = s.maintenance().recordRoomRequest("A1", "Staff Person", IssueCategory.OTHER, Priority.LOW, "Paint the wall soon");
            s.maintenance().startRepair(b.getId());
            HostelSystem s2 = start(dir);
            MaintenanceRequest x = s2.maintenance().findById("G01-M001");
            MaintenanceRequest y = s2.maintenance().findById("G01-M002");
            return x.getClass().getSimpleName() + " " + x.getStatus() + " " + x.getResolutionNotes() + " | "
                    + y.getClass().getSimpleName() + " " + y.getStatus();
        });
        test("MAINT-13", "Maintenance", "Text with the file separator and a backslash in it survives a restart", "Pipe | and \\ slash ok", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            s.maintenance().recordComplaint("G01-T001", IssueCategory.OTHER, Priority.LOW, "Pipe | and \\ slash ok");
            return start(dir).maintenance().findById("G01-M001").getDescription();
        });
    }

    // ==================================================== Visitors (Member 5)

    private static final LocalDate TODAY = LocalDate.now();

    private static void visitorTests() {
        test("VIS-01", "Visitors", "Register a visitor, check in and check out", "G01-V001 G01-L001 inside=true then false", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            Visit visit = s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Family visit");
            String a = visit.getId() + " inside=" + visit.isInside();
            s.visitors().checkOut(visit.getId(), LocalTime.of(12, 0));
            return v.getId() + " " + a + " then " + visit.isInside();
        });
        test("VIS-02", "Visitors", "Invalid visitor names and phone are rejected",
                "rejected: InvalidVisitorException/rejected: InvalidVisitorException/rejected: InvalidVisitorException", () -> {
            HostelSystem s = start(newDir());
            return outcome(() -> s.visitors().registerVisitor("R2D2", "")) + "/"
                    + outcome(() -> s.visitors().registerVisitor("", "")) + "/"
                    + outcome(() -> s.visitors().registerVisitor("Good Name", "abc"));
        });
        test("VIS-03", "Visitors", "Duplicate visitor (same name and phone) is rejected", "rejected: InvalidVisitorException", () -> {
            HostelSystem s = start(newDir());
            s.visitors().registerVisitor("Visitor Person", "0700000555");
            return outcome(() -> s.visitors().registerVisitor("visitor person", "0700000555"));
        });
        test("VIS-04", "Visitors", "Check-in with a future date or future time is rejected",
                "rejected: InvalidVisitorException/rejected: InvalidVisitorException", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            String a = outcome(() -> s.visitors().checkIn(v.getId(), "G01-T001", TODAY.plusDays(1), LocalTime.of(10, 0), "Visit"));
            LocalTime later = LocalTime.now().plusMinutes(30);
            String b = LocalTime.now().isBefore(LocalTime.of(23, 0))
                    ? outcome(() -> s.visitors().checkIn(v.getId(), "G01-T001", TODAY, later, "Visit"))
                    : "rejected: InvalidVisitorException";
            return a + "/" + b;
        });
        test("VIS-05", "Visitors", "Check-out before check-in time is rejected and visitor stays inside",
                "rejected: InvalidVisitorException inside=true", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            Visit visit = s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            String o = outcome(() -> s.visitors().checkOut(visit.getId(), LocalTime.of(9, 0)));
            return o + " inside=" + visit.isInside();
        });
        test("VIS-06", "Visitors", "Checking out twice is rejected", "rejected: InvalidVisitorException", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            Visit visit = s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            s.visitors().checkOut(visit.getId(), LocalTime.of(11, 0));
            return outcome(() -> s.visitors().checkOut(visit.getId(), LocalTime.of(12, 0)));
        });
        test("VIS-07", "Visitors", "Same visitor cannot be inside twice", "rejected: InvalidVisitorException", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            return outcome(() -> s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 30), "Visit"));
        });
        test("VIS-08", "Visitors", "A 4th visitor for one tenant throws VisitorLimitExceededException", "rejected: VisitorLimitExceededException", () -> {
            HostelSystem s = rentFixture(newDir());
            String[] names = {"Guest One", "Guest Two", "Guest Three", "Guest Four"};
            List<Visitor> list = new ArrayList<>();
            for (String n : names) {
                list.add(s.visitors().registerVisitor(n, ""));
            }
            for (int i = 0; i < VisitorService.MAX_VISITORS_AT_ONCE; i++) {
                s.visitors().checkIn(list.get(i).getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, i), "Visit");
            }
            return outcome(() -> s.visitors().checkIn(list.get(3).getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(11, 0), "Visit"));
        });
        test("VIS-09", "Visitors", "Visit to an unknown tenant is rejected", "rejected: InvalidTenantException", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            return outcome(() -> s.visitors().checkIn(v.getId(), "G01-T999", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit"));
        });
        test("VIS-10", "Visitors", "Tenant cannot be removed while a visitor is inside; after check-out removal deletes their visits",
                "rejected: InvalidVisitorException then visits=0", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            Visit visit = s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            String first = outcome(() -> s.tenants().removeTenant("G01-T001"));
            s.visitors().checkOut(visit.getId(), LocalTime.of(11, 0));
            s.tenants().removeTenant("G01-T001");
            return first + " then visits=" + s.visitors().getVisitCount();
        });
        test("VIS-11", "Visitors", "Visitor who is inside cannot be removed", "rejected: InvalidVisitorException", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            return outcome(() -> s.visitors().removeVisitor(v.getId()));
        });
        test("VIS-12", "Visitors", "Update a visit: bad time rejected, good change applied",
                "rejected: InvalidVisitorException 09:15 Delivery", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor v = s.visitors().registerVisitor("Visitor Person", "");
            Visit visit = s.visitors().checkIn(v.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            s.visitors().checkOut(visit.getId(), LocalTime.of(11, 0));
            String bad = outcome(() -> s.visitors().updateVisit(visit.getId(), TODAY.minusDays(1), LocalTime.of(11, 30), "Visit"));
            s.visitors().updateVisit(visit.getId(), TODAY.minusDays(1), LocalTime.of(9, 15), "Delivery");
            return bad + " " + visit.getCheckIn() + " " + visit.getPurpose();
        });
        test("VIS-13", "Visitors", "Reports: inside is filtered, date report is filtered, history is newest first",
                "inside=1 onDate=1 newestFirst=true", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor a = s.visitors().registerVisitor("Guest One", "");
            Visitor b = s.visitors().registerVisitor("Guest Two", "");
            Visit old = s.visitors().checkIn(a.getId(), "G01-T001", TODAY.minusDays(5), LocalTime.of(10, 0), "Visit");
            s.visitors().checkOut(old.getId(), LocalTime.of(11, 0));
            s.visitors().checkIn(b.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            int inside = s.visitors().generateInsideReport().split("G01-L0").length - 1;
            int onDate = s.visitors().generateDateReport(TODAY.minusDays(5)).split("G01-L0").length - 1;
            String h = s.visitors().generateHistoryReport();
            boolean newest = h.indexOf("G01-L002") < h.indexOf("G01-L001");
            return "inside=" + inside + " onDate=" + onDate + " newestFirst=" + newest;
        });
        test("VIS-14", "Visitors", "Visitors and visits survive a restart and re-link", "Guest One -> Aaron Test out=11:00", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            Visitor a = s.visitors().registerVisitor("Guest One", "");
            Visit v = s.visitors().checkIn(a.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            s.visitors().checkOut(v.getId(), LocalTime.of(11, 0));
            Visit q = start(dir).visitors().findVisit("G01-L001");
            return q.getVisitor().getFullName() + " -> " + q.getTenant().getFullName() + " out=" + q.getCheckOut();
        });
        test("VIS-15", "Visitors", "Mixed Visitor and Visit objects are listed together", "2 visitors + 1 visit = 3 records", () -> {
            HostelSystem s = rentFixture(newDir());
            Visitor a = s.visitors().registerVisitor("Guest One", "");
            s.visitors().registerVisitor("Guest Two", "");
            s.visitors().checkIn(a.getId(), "G01-T001", TODAY.minusDays(1), LocalTime.of(10, 0), "Visit");
            return "2 visitors + 1 visit = " + s.visitors().getAllRecords().size() + " records";
        });
    }

    // ============================================== File storage (Member 7)

    private static void storageTests() {
        test("STORE-01", "Storage", "First run with no data folder or files starts empty without crashing", "rooms=0 tenants=0", () -> {
            Path dir = newDir().resolve("does-not-exist-yet");
            HostelSystem s = start(dir);
            return "rooms=" + s.rooms().getRoomCount() + " tenants=" + s.tenants().getTenantCount();
        });
        test("STORE-02", "Storage", "Data folder is created and files are written after every change", "true true", () -> {
            Path dir = newDir().resolve("new-folder");
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            return Files.exists(dir.resolve("rooms.txt")) + " " + Files.exists(dir.resolve("counters.txt"));
        });
        test("STORE-03", "Storage", "One damaged line is skipped, good lines still load, a backup is kept", "rooms=2 backup=true", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, false);
            List<String> lines = new ArrayList<>(Files.readAllLines(dir.resolve("rooms.txt")));
            lines.add(1, "this is garbage|||");
            lines.add("G01-R050|STANDARD|A5|notanumber|100");
            Files.write(dir.resolve("rooms.txt"), lines);
            HostelSystem s2 = start(dir);
            boolean backup = false;
            try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                backup = files.anyMatch(p -> p.getFileName().toString().contains(".damaged-"));
            }
            return "rooms=" + s2.rooms().getRoomCount() + " backup=" + backup;
        });
        test("STORE-04", "Storage", "A completely binary / unreadable file does not crash the program", "tenants=0 then app works", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            Files.write(dir.resolve("tenants.txt"), new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0x80, (byte) 0xC3, 0x28});
            HostelSystem s2 = start(dir);
            addTenant(s2, 0, "A1");
            return "tenants=" + (s2.tenants().getTenantCount() - 1) + " then app works";
        });
        test("STORE-05", "Storage", "An empty file and a file with only blank/comment lines load as empty", "rooms=0", () -> {
            Path dir = newDir();
            Files.write(dir.resolve("rooms.txt"), Arrays.asList("", "   ", "# comment"));
            Files.write(dir.resolve("tenants.txt"), new byte[0]);
            return "rooms=" + start(dir).rooms().getRoomCount();
        });
        test("STORE-06", "Storage", "Deleted record's ID is never reused after a restart", "G01-R003", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, false);
            s.rooms().removeRoom("A2");
            HostelSystem s2 = start(dir);
            return s2.rooms().addRoom("A3", 2, 100_000, false).getId();
        });
        test("STORE-07", "Storage", "ID counter survives even if counters.txt is deleted (rebuilt from records)", "G01-R003", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            s.rooms().addRoom("A2", 2, 100_000, false);
            Files.delete(dir.resolve("counters.txt"));
            return start(dir).rooms().addRoom("A3", 2, 100_000, false).getId();
        });
        test("STORE-08", "Storage", "Damaged counters.txt is ignored safely", "G01-R002", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            Files.write(dir.resolve("counters.txt"), Arrays.asList("R=abc", "junk", "=", "RR=5"));
            return start(dir).rooms().addRoom("A2", 2, 100_000, false).getId();
        });
        test("STORE-09", "Storage", "Records pointing at a deleted tenant are dropped and the bed is freed on load",
                "tenants=0 rent=0 freeBeds=2", () -> {
            Path dir = newDir();
            HostelSystem s = rentFixture(dir);
            s.rent().addPayment("G01-T001", YearMonth.of(2026, 10), null);
            Files.write(dir.resolve("tenants.txt"), new ArrayList<String>());   // someone emptied the tenants file
            HostelSystem s2 = start(dir);
            return "tenants=" + s2.tenants().getTenantCount() + " rent=" + s2.rent().getCount()
                    + " freeBeds=" + s2.rooms().findRoom("A1").getFreeBeds();
        });
        test("STORE-10", "Storage", "Allocation pointing at a missing room is dropped on load", "rooms=0 allocations=0", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 2, 100_000, false);
            addTenant(s, 0, "A1");
            Files.write(dir.resolve("rooms.txt"), new ArrayList<String>());
            HostelSystem s2 = start(dir);
            return "rooms=" + s2.rooms().getRoomCount() + " allocations=" + s2.rooms().getAllAllocations().size();
        });
        test("STORE-11", "Storage", "Over-booked data file (more tenants than beds) is repaired on load", "occupants=1", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            s.rooms().addRoom("A1", 1, 100_000, false);
            addTenant(s, 0, "A1");
            List<String> lines = new ArrayList<>(Files.readAllLines(dir.resolve("allocations.txt")));
            lines.add("G01-A099|G01-R001|G01-T777|2026-01-01|");
            Files.write(dir.resolve("allocations.txt"), lines);
            return "occupants=" + start(dir).rooms().findRoom("A1").getOccupantCount();
        });
        test("STORE-12", "Storage", "join/split round trip with separator, backslash and line breaks", "ok", () -> {
            String[] fields = {"a|b", "c\\d", "line1\nline2", "", "end\\", "|"};
            String[] back = FileStorage.split(FileStorage.join(fields));
            return Arrays.equals(fields, back) ? "ok" : Arrays.toString(back);
        });
        test("STORE-13", "Storage", "Duplicate IDs in a file: the second copy is skipped", "rooms=1", () -> {
            Path dir = newDir();
            Files.write(dir.resolve("rooms.txt"), Arrays.asList("G01-R001|STANDARD|A1|2|100000", "G01-R001|STANDARD|A2|2|100000"));
            return "rooms=" + start(dir).rooms().getRoomCount();
        });
        test("STORE-14", "Storage", "Saving to an impossible location warns but does not crash", "returned false", () -> {
            Path dir = newDir();
            Path blocker = dir.resolve("blocker");
            Files.write(blocker, new byte[] {1});
            boolean ok = FileStorage.writeLines(blocker.resolve("inside.txt"), Arrays.asList("x"));
            return ok ? "returned true" : "returned false";
        });
        test("STORE-15", "Storage", "A full set of data written by one run is read back identically by the next", "24 24", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            SampleData.load(s);
            int before = s.allRecords().size();
            return before + " " + start(dir).allRecords().size();
        });
    }

    // ========================================== Shared core (Member 6) and input

    private static void coreTests() {
        test("CORE-01", "Core", "InputHelper.readInt ignores garbage until a valid number", "3", () ->
                runInput("abc\n\n-5\n99\n3.5\n3\n", () -> String.valueOf(InputHelper.readInt("n: ", 1, 5))));
        test("CORE-02", "Core", "InputHelper.readLong accepts 450,000 and rejects NaN / Infinity / text", "450000", () ->
                runInput("NaN\nInfinity\nabc\n-1\n450,000\n", () -> String.valueOf(InputHelper.readLong("n: ", 1, 1_000_000))));
        test("CORE-03", "Core", "InputHelper.readDate rejects 2026-02-30 and bad formats", "2026-02-28", () ->
                runInput("2026-02-30\n28/02/2026\n2026-2-28\n2026-02-28\n", () -> InputHelper.readDate("d: ").toString()));
        test("CORE-04", "Core", "InputHelper.readTime rejects 25:00, 9:5 and text", "09:05", () ->
                runInput("25:00\n9:5\nnoon\n09:05\n", () -> InputHelper.readTime("t: ").toString()));
        test("CORE-05", "Core", "InputHelper.readName rejects digits and symbols", "Mary Anne", () ->
                runInput("M4ry\n<script>\n \nMary   Anne\n", () -> InputHelper.readName("n: ")));
        test("CORE-06", "Core", "End of input throws InputClosedException (no endless loop)", "rejected: InputClosedException", () ->
                runInput("", () -> outcome(() -> InputHelper.readRequired("x: "))));
        test("CORE-07", "Core", "readYesNo accepts y/yes/n/no only", "true/false", () ->
                runInput("maybe\nYES\nn\n", () -> InputHelper.readYesNo("q") + "/" + InputHelper.readYesNo("q")));
        test("CORE-08", "Core", "NaturalOrder sorts A2 < A10 and G01-T9 < G01-T10", "true true true", () ->
                (NaturalOrder.INSTANCE.compare("A2", "A10") < 0) + " " + (NaturalOrder.INSTANCE.compare("G01-T9", "G01-T10") < 0)
                        + " " + (NaturalOrder.INSTANCE.compare("a1", "A1") == 0));
        test("CORE-09", "Core", "Every record ID starts with the group code and the right letter", "R,A,T,P,M,V,L all G01-", () -> {
            HostelSystem s = start(newDir());
            SampleData.load(s);
            boolean ok = s.rooms().getAllRooms().get(0).getId().startsWith("G01-R")
                    && s.rooms().getAllAllocations().get(0).getId().startsWith("G01-A")
                    && s.tenants().getAllTenants().get(0).getId().startsWith("G01-T")
                    && s.rent().getAll().get(0).getId().startsWith("G01-P")
                    && s.maintenance().getAll().get(0).getId().startsWith("G01-M")
                    && s.visitors().getAllVisitors().get(0).getId().startsWith("G01-V")
                    && s.visitors().getAllVisits().get(0).getId().startsWith("G01-L");
            return ok ? "R,A,T,P,M,V,L all G01-" : "BAD IDs";
        });
        test("CORE-10", "Core", "Sample data loads once and refuses to load into a non-empty system", "true/false", () -> {
            HostelSystem s = start(newDir());
            return SampleData.load(s) + "/" + SampleData.load(s);
        });
        test("CORE-11", "Core", "Every module is Reportable and produces a report", "5 modules, all reports non-empty", () -> {
            HostelSystem s = start(newDir());
            SampleData.load(s);
            int n = 0;
            for (ug.ac.vu.greenview.core.Reportable r : s.reportables()) {
                if (!r.generateReport().isEmpty() && !r.getModuleName().isEmpty()) {
                    n++;
                }
            }
            return n == 5 ? "5 modules, all reports non-empty" : "only " + n;
        });
        test("CORE-12", "Core", "Menu scenario typed like a real user: room -> tenant -> rent -> payment (all via menus)",
                "tenant=G01-T001 room=A1 paid=100000 status=PARTIAL", () -> {
            Path dir = newDir();
            HostelSystem s = start(dir);
            String script = String.join("\n",
                    "1", "1", "A1", "2", "400000", "n", "0",                     // rooms: register room A1
                    "2", "1", "1", "Mary Fictional", "", "Test College",         // tenants: register tenant
                    "Anna Fictional", "Mother", "0700000999", "A1", "0",         // emergency contact, room, back
                    "3", "1", "G01-T001", "2026-10", "", "2", "G01-P001", "100,000", "", "0",   // rent
                    "0") + "\n";
            InputHelper.setInput(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
            new MainMenu(s).run();
            Tenant t = s.tenants().findTenant("G01-T001");
            RentPayment p = s.rent().findById("G01-P001");
            PaymentStatus st = p.getStatus();
            return "tenant=" + t.getId() + " room=" + t.getRoom().getRoomNumber() + " paid=" + p.getAmountPaid() + " status=" + st;
        });
    }

    // ============================================ Stress: random typing into menus

    private static void stressTests() {
        final String[] pool = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "99", "-1",
                "", " ", "abc", "y", "n", "yes", "G01-T001", "G01-T002", "G01-R001", "G01-R002", "A101", "B201", "G01-P001",
                "G01-M001", "G01-V001", "G01-V002", "G01-L001", "G01-L002", "2026-10", "2026-10-08", "2026-02-30", "09:30",
                "25:61", "450000", "450,000", "0", "1", "2", "99999999999999999999", "NaN", "Infinity", "Test Name", "O'Brien-Smith",
                "0700000333", "|", "\\", "x".repeat(300), "<b>html</b>", "'; DROP TABLE", "Leaking tap everywhere", "-"};
        test("STRESS-01", "Stress", "40 runs of 600 random inputs typed into the whole menu system: no crash, no 'went wrong' message, files still load cleanly",
                "40 runs clean", () -> {
            int clean = 0;
            for (int seed = 1; seed <= 40; seed++) {
                Path dir = newDir();
                HostelSystem s = start(dir);
                if (seed % 2 == 0) {
                    SampleData.load(s);
                }
                Random rnd = new Random(seed);
                StringBuilder input = new StringBuilder();
                for (int i = 0; i < 600; i++) {
                    input.append(pool[rnd.nextInt(pool.length)]).append('\n');
                }
                ByteArrayOutputStream captured = new ByteArrayOutputStream();
                PrintStream saved = System.out;
                System.setOut(new PrintStream(captured, true, "UTF-8"));
                String endedBy = "ran out of lines without closing";
                try {
                    InputHelper.setInput(new ByteArrayInputStream(input.toString().getBytes(StandardCharsets.UTF_8)));
                    new MainMenu(s).run();
                    endedBy = "exit";
                } catch (InputClosedException e) {
                    endedBy = "exit";
                } catch (Throwable e) {
                    endedBy = "CRASH " + e;
                } finally {
                    System.setOut(saved);
                }
                String text = captured.toString("UTF-8");
                if (!endedBy.equals("exit") || text.contains("went wrong") || text.contains("WARNING")) {
                    return "seed " + seed + ": " + endedBy + (text.contains("went wrong") ? " [went wrong]" : "")
                            + (text.contains("WARNING") ? " [WARNING in run]" : "");
                }
                // restart from the files this run produced: must load with no warnings
                ByteArrayOutputStream reload = new ByteArrayOutputStream();
                System.setOut(new PrintStream(reload, true, "UTF-8"));
                try {
                    start(dir);
                } finally {
                    System.setOut(saved);
                }
                if (reload.toString("UTF-8").contains("WARNING")) {
                    return "seed " + seed + ": files written by the run reload with warnings: " + reload.toString("UTF-8");
                }
                clean++;
            }
            return clean + " runs clean";
        });
    }
}
