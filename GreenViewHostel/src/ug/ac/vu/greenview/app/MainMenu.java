package ug.ac.vu.greenview.app;

import java.util.Map;
import java.util.TreeMap;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;
import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.maintenance.MaintenanceMenu;
import ug.ac.vu.greenview.rent.RentMenu;
import ug.ac.vu.greenview.rooms.RoomMenu;
import ug.ac.vu.greenview.tenants.TenantMenu;
import ug.ac.vu.greenview.visitors.VisitorMenu;

/**
 * The main menu that leads to every module's submenu.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public class MainMenu extends ConsoleMenu {

    private final HostelSystem system;
    private final RoomMenu roomMenu;
    private final TenantMenu tenantMenu;
    private final RentMenu rentMenu;
    private final MaintenanceMenu maintenanceMenu;
    private final VisitorMenu visitorMenu;

    public MainMenu(HostelSystem system) {
        this.system = system;
        this.roomMenu = new RoomMenu(system.rooms(), system.tenants());   // TenantService is the Rooms module's TenantLink
        this.tenantMenu = new TenantMenu(system.tenants(), system.rooms());
        this.rentMenu = new RentMenu(system.rent());
        this.maintenanceMenu = new MaintenanceMenu(system.maintenance());
        this.visitorMenu = new VisitorMenu(system.visitors());
    }

    @Override
    protected String title() {
        return AppConfig.HOSTEL_NAME.toUpperCase() + " - MAIN MENU";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Rooms Management",
            "Tenant Management",
            "Rent Payment Management",
            "Maintenance Management",
            "Visitors Management",
            "Whole-hostel summary report",
            "Record counts for the whole system",
            "Load sample (fictional) data for a demo"
        };
    }

    @Override
    protected String backLabel() {
        return "Exit";
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: roomMenu.run(); break;
            case 2: tenantMenu.run(); break;
            case 3: rentMenu.run(); break;
            case 4: maintenanceMenu.run(); break;
            case 5: visitorMenu.run(); break;
            case 6: printSummary(); break;
            case 7: printCounts(); break;
            case 8: loadSample(); break;
            default: break;
        }
    }

    /** Every module is a Reportable, so one loop reports on all of them. */
    private void printSummary() {
        System.out.println();
        System.out.println("################ " + AppConfig.HOSTEL_NAME.toUpperCase() + " - SUMMARY ################");
        for (Reportable module : system.reportables()) {
            System.out.println();
            System.out.println("######## " + module.getModuleName().toUpperCase() + " ########");
            System.out.println(module.generateReport());
        }
    }

    /** One loop over records of many different classes, counted by class name. */
    private void printCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Record r : system.allRecords()) {
            counts.merge(r.getClass().getSimpleName(), 1, Integer::sum);
        }
        System.out.println();
        System.out.println("--- Records stored in the system ---");
        if (counts.isEmpty()) {
            System.out.println("(nothing stored yet)");
        }
        int total = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.printf("%-26s %d%n", e.getKey(), e.getValue());
            total += e.getValue();
        }
        System.out.println("Total records: " + total);
    }

    private void loadSample() {
        if (!InputHelper.readYesNo("Load fictional demo data (only works on an empty system)?")) {
            System.out.println("Cancelled.");
            return;
        }
        SampleData.load(system);
    }
}
