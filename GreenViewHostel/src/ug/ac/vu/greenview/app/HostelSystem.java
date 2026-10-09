package ug.ac.vu.greenview.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.maintenance.MaintenanceService;
import ug.ac.vu.greenview.rent.RentPaymentManager;
import ug.ac.vu.greenview.rooms.RoomService;
import ug.ac.vu.greenview.storage.CounterStore;
import ug.ac.vu.greenview.tenants.TenantService;
import ug.ac.vu.greenview.visitors.VisitorService;

/**
 * The integration point: creates the five module services in the right order,
 * connects them to each other, and wires up the listeners that keep the modules
 * consistent when records are removed.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class HostelSystem {

    private final RoomService rooms;
    private final TenantService tenants;
    private final RentPaymentManager rent;
    private final MaintenanceService maintenance;
    private final VisitorService visitors;

    public HostelSystem(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException | RuntimeException e) {
            System.out.println("WARNING: could not create the data folder (" + e.getMessage()
                    + "). Changes may not be saved.");
        }
        Path counterFile = dataDir.resolve("counters.txt");
        CounterStore.load(counterFile);
        IdGenerator.setChangeListener(() -> CounterStore.save(counterFile));

        // Order matters: later modules point at earlier ones.
        rooms = new RoomService(dataDir);
        tenants = new TenantService(dataDir, rooms);
        rent = new RentPaymentManager(dataDir, tenants, rooms);
        maintenance = new MaintenanceService(dataDir, rooms, tenants);
        visitors = new VisitorService(dataDir, tenants);

        // When a room is removed, rent and maintenance get a say and tidy up.
        rooms.addRemovalListener(rent.roomListener());
        rooms.addRemovalListener(maintenance.roomListener());
        // When a tenant is removed, rent, maintenance and visitors get a say and tidy up.
        tenants.addRemovalListener(rent.tenantListener());
        tenants.addRemovalListener(maintenance.tenantListener());
        tenants.addRemovalListener(visitors.tenantListener());
    }

    public RoomService rooms() { return rooms; }
    public TenantService tenants() { return tenants; }
    public RentPaymentManager rent() { return rent; }
    public MaintenanceService maintenance() { return maintenance; }
    public VisitorService visitors() { return visitors; }

    /** Every module service that can make a report (they all share the Reportable interface). */
    public List<Reportable> reportables() {
        return Arrays.<Reportable>asList(rooms, tenants, rent, maintenance, visitors);
    }

    /** Every record in the whole system, of every class, in one list. */
    public List<Record> allRecords() {
        List<Record> all = new ArrayList<>();
        all.addAll(rooms.getAllRooms());
        all.addAll(rooms.getAllAllocations());
        all.addAll(tenants.getAllTenants());
        all.addAll(rent.getAll());
        all.addAll(maintenance.getAll());
        all.addAll(visitors.getAllRecords());
        return all;
    }
}
