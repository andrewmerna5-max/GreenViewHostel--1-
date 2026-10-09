package ug.ac.vu.greenview.maintenance;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.NaturalOrder;
import ug.ac.vu.greenview.core.RemovalListener;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.rooms.Room;
import ug.ac.vu.greenview.rooms.RoomService;
import ug.ac.vu.greenview.storage.RecordStore;
import ug.ac.vu.greenview.tenants.Tenant;
import ug.ac.vu.greenview.tenants.TenantService;

/**
 * Manages all maintenance complaints and requests in an ArrayList: record, find,
 * search, update status, edit, remove and report. Loads from file when created
 * and saves after every change.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public final class MaintenanceService implements Reportable {

    /** Pending report order: most urgent first, then oldest first. */
    private static final Comparator<MaintenanceRequest> URGENT_FIRST =
            Comparator.comparing(MaintenanceRequest::getPriority, Comparator.reverseOrder())
                      .thenComparing(MaintenanceRequest::getDateReported)
                      .thenComparing(MaintenanceRequest::getId, NaturalOrder.INSTANCE);

    /** Completed report order: most recently finished first. */
    private static final Comparator<MaintenanceRequest> RECENT_FIRST =
            Comparator.comparing(MaintenanceRequest::getDateCompleted, Comparator.reverseOrder())
                      .thenComparing(MaintenanceRequest::getId, NaturalOrder.INSTANCE);

    private final ArrayList<MaintenanceRequest> requests = new ArrayList<>();
    private final RecordStore<MaintenanceRequest> store;
    private final RoomService rooms;
    private final TenantService tenants;

    public MaintenanceService(Path dataDir, RoomService rooms, TenantService tenants) {
        this.rooms = rooms;
        this.tenants = tenants;
        this.store = new RecordStore<>(dataDir.resolve("maintenance.txt"), fields -> parse(fields, rooms, tenants));
        requests.addAll(store.load());
    }

    /** Rebuilds one request, re-linking it to the real Room (and Tenant) objects. */
    private static MaintenanceRequest parse(String[] f, RoomService rooms, TenantService tenants) throws Exception {
        if (f.length < 12) {
            throw new IllegalArgumentException("expected 12 fields but found " + f.length);
        }
        Room room = rooms.findById(f[2]);
        if (room == null) {
            throw new IllegalArgumentException("room " + f[2] + " no longer exists");
        }
        IssueCategory category = IssueCategory.valueOf(f[5].trim());
        Priority priority = Priority.valueOf(f[6].trim());
        LocalDate reported = LocalDate.parse(f[9].trim());
        MaintenanceRequest request;
        if ("COMPLAINT".equals(f[1])) {
            Tenant tenant = f[3].trim().isEmpty() ? null : tenants.findTenant(f[3]);
            request = new TenantComplaint(f[0], room, tenant, f[4], category, priority, f[7], reported);
        } else if ("ROOMREQ".equals(f[1])) {
            request = new RoomMaintenanceRequest(f[0], room, f[4], category, priority, f[7], reported);
        } else {
            throw new IllegalArgumentException("unknown request type '" + f[1] + "'");
        }
        RepairStatus status = RepairStatus.valueOf(f[8].trim());
        if (status == RepairStatus.IN_PROGRESS) {
            request.startRepair();
        } else if (status == RepairStatus.COMPLETED) {
            request.markCompleted(LocalDate.parse(f[10].trim()), f[11]);
        }
        return request;
    }

    @Override
    public String getModuleName() {
        return "Maintenance";
    }

    private void save() {
        store.save(requests);
    }

    // ----------------------------------------------------------------- record

    /** A tenant reports a problem in their own room. */
    public TenantComplaint recordComplaint(String tenantId, IssueCategory category, Priority priority,
                                           String description) throws HostelException {
        Tenant tenant = tenants.requireTenant(tenantId);
        Room room = tenant.getRoom();
        if (room == null) {
            throw new InvalidMaintenanceException("Tenant " + tenant.getId()
                    + " has no room. Use a room maintenance request instead.");
        }
        MaintenanceRequest.validateDescription(description);
        TenantComplaint complaint = new TenantComplaint(IdGenerator.next('M'), room, tenant,
                tenant.getFullName(), category, priority, description, LocalDate.now());
        requests.add(complaint);
        save();
        return complaint;
    }

    /** Staff report a problem in any room. */
    public RoomMaintenanceRequest recordRoomRequest(String roomKey, String staffName, IssueCategory category,
                                                    Priority priority, String description) throws HostelException {
        Room room = rooms.requireRoom(roomKey);
        MaintenanceRequest.validateDescription(description);
        RoomMaintenanceRequest request = new RoomMaintenanceRequest(IdGenerator.next('M'), room, staffName,
                category, priority, description, LocalDate.now());
        requests.add(request);
        save();
        return request;
    }

    // ------------------------------------------------------------------- find

    public MaintenanceRequest findById(String id) {
        if (id == null) {
            return null;
        }
        for (MaintenanceRequest r : requests) {
            if (r.getId().equalsIgnoreCase(id.trim())) {
                return r;
            }
        }
        return null;
    }

    public MaintenanceRequest requireRequest(String id) throws InvalidMaintenanceException {
        MaintenanceRequest r = findById(id);
        if (r == null) {
            throw new InvalidMaintenanceException("No maintenance record found with ID: " + (id == null ? "" : id.trim()));
        }
        return r;
    }

    /** Matches the ID, room number, who raised it, category, status or words in the description. */
    public List<MaintenanceRequest> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<MaintenanceRequest> result = new ArrayList<>();
        if (k.isEmpty()) {
            return result;
        }
        for (MaintenanceRequest r : requests) {
            if (r.getId().toLowerCase(Locale.ROOT).contains(k)
                    || r.getRoom().getRoomNumber().toLowerCase(Locale.ROOT).contains(k)
                    || r.getRaisedBy().toLowerCase(Locale.ROOT).contains(k)
                    || r.getCategory().toString().toLowerCase(Locale.ROOT).contains(k)
                    || r.getStatus().toString().toLowerCase(Locale.ROOT).contains(k)
                    || r.getPriority().toString().toLowerCase(Locale.ROOT).contains(k)
                    || r.getDescription().toLowerCase(Locale.ROOT).contains(k)) {
                result.add(r);
            }
        }
        result.sort(URGENT_FIRST);
        return result;
    }

    public List<MaintenanceRequest> getAll() {
        List<MaintenanceRequest> all = new ArrayList<>(requests);
        all.sort(Comparator.comparing(MaintenanceRequest::getId, NaturalOrder.INSTANCE));
        return all;
    }

    public int getCount() {
        return requests.size();
    }

    // ----------------------------------------------------------------- update

    public MaintenanceRequest startRepair(String id) throws HostelException {
        MaintenanceRequest r = requireRequest(id);
        r.startRepair();
        save();
        return r;
    }

    public MaintenanceRequest markCompleted(String id, LocalDate date, String notes) throws HostelException {
        MaintenanceRequest r = requireRequest(id);
        r.markCompleted(date, notes);
        save();
        return r;
    }

    public MaintenanceRequest updateRequest(String id, IssueCategory category, Priority priority,
                                            String description) throws HostelException {
        MaintenanceRequest r = requireRequest(id);
        r.updateDetails(category, priority, description);
        save();
        return r;
    }

    // ----------------------------------------------------------------- remove

    public MaintenanceRequest removeRequest(String id) throws HostelException {
        MaintenanceRequest r = requireRequest(id);
        requests.remove(r);
        save();
        return r;
    }

    // ---------------------------------------------- keeping modules consistent

    /** Registered with the Rooms module: no removal while repairs are open; cleans up afterwards. */
    public RemovalListener<Room> roomListener() {
        return new RemovalListener<Room>() {
            @Override
            public void canRemove(Room room) throws HostelException {
                for (MaintenanceRequest r : requests) {
                    if (r.getRoom() == room && r.isOpen()) {
                        throw new InvalidMaintenanceException("Cannot remove room " + room.getRoomNumber()
                                + ": maintenance record " + r.getId() + " is still " + r.getStatus() + ".");
                    }
                }
            }

            @Override
            public void onRemoved(Room room) {
                if (requests.removeIf(r -> r.getRoom() == room)) {
                    save();
                }
            }
        };
    }

    /** Registered with the Tenants module: keeps the complaint history, detaches the tenant. */
    public RemovalListener<Tenant> tenantListener() {
        return new RemovalListener<Tenant>() {
            @Override
            public void onRemoved(Tenant tenant) {
                boolean changed = false;
                for (MaintenanceRequest r : requests) {
                    if (r instanceof TenantComplaint && ((TenantComplaint) r).getTenant() == tenant) {
                        ((TenantComplaint) r).detachTenant();
                        changed = true;
                    }
                }
                if (changed) {
                    save();
                }
            }
        };
    }

    // ---------------------------------------------------------------- reports

    /** FILTERED + SORTED: open requests, most urgent first. Complaints and room requests are mixed. */
    public String generatePendingReport() {
        List<MaintenanceRequest> open = new ArrayList<>();
        for (MaintenanceRequest r : requests) {
            if (r.isOpen()) {
                open.add(r);
            }
        }
        open.sort(URGENT_FIRST);
        StringBuilder sb = new StringBuilder("=== PENDING MAINTENANCE REPORT (pending and in progress, most urgent first) ===\n");
        for (MaintenanceRequest r : open) {
            sb.append(r.describe()).append('\n');       // TenantComplaint / RoomMaintenanceRequest - polymorphic call
        }
        if (open.isEmpty()) {
            sb.append("No pending maintenance.\n");
        }
        sb.append("Open requests: ").append(open.size());
        return sb.toString();
    }

    /** FILTERED + SORTED: finished repairs, newest first. */
    public String generateCompletedReport() {
        List<MaintenanceRequest> done = new ArrayList<>();
        for (MaintenanceRequest r : requests) {
            if (r.isCompleted()) {
                done.add(r);
            }
        }
        done.sort(RECENT_FIRST);
        StringBuilder sb = new StringBuilder("=== COMPLETED MAINTENANCE REPORT (newest first) ===\n");
        for (MaintenanceRequest r : done) {
            sb.append(r.describe()).append('\n');
        }
        if (done.isEmpty()) {
            sb.append("No completed maintenance.\n");
        }
        sb.append("Completed requests: ").append(done.size());
        return sb.toString();
    }

    @Override
    public String generateReport() {
        return generatePendingReport() + "\n\n" + generateCompletedReport();
    }
}
