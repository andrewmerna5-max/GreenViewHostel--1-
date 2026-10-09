package ug.ac.vu.greenview.tenants;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.NaturalOrder;
import ug.ac.vu.greenview.core.RemovalListener;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.rooms.Room;
import ug.ac.vu.greenview.rooms.RoomFullException;
import ug.ac.vu.greenview.rooms.RoomService;
import ug.ac.vu.greenview.rooms.TenantLink;
import ug.ac.vu.greenview.storage.RecordStore;

/**
 * Manages all tenants in an ArrayList: register, find, search, update, remove,
 * link to rooms and report. Works together with the Rooms module through object
 * references. Loads from file when created and saves after every change.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public final class TenantService implements Reportable, TenantLink {

    public static final Comparator<Tenant> BY_NAME =
            Comparator.comparing(Tenant::getFullName, String.CASE_INSENSITIVE_ORDER)
                      .thenComparing(Tenant::getId, NaturalOrder.INSTANCE);

    private final ArrayList<Tenant> tenants = new ArrayList<>();
    private final RecordStore<Tenant> store;
    private final RoomService rooms;
    private final List<RemovalListener<Tenant>> listeners = new ArrayList<>();

    public TenantService(Path dataDir, RoomService rooms) {
        this.rooms = rooms;
        this.store = new RecordStore<>(dataDir.resolve("tenants.txt"), Tenant::fromFields);
        tenants.addAll(store.load());
        Set<String> ids = new HashSet<>();
        for (Tenant t : tenants) {
            ids.add(t.getId());
            t.setRoomReference(rooms.getRoomOfTenant(t.getId()));   // link tenant -> room object
        }
        rooms.releaseOrphans(ids);
    }

    @Override
    public String getModuleName() {
        return "Tenants";
    }

    public void addRemovalListener(RemovalListener<Tenant> listener) {
        listeners.add(listener);
    }

    private void save() {
        store.save(tenants);
    }

    // ------------------------------------------------------------- register

    /** Checks a room can take a new tenant, without changing anything. */
    public Room checkRoomAvailable(String roomKey) throws HostelException {
        Room room = rooms.requireRoom(roomKey);
        if (room.isFull()) {
            throw new RoomFullException("Room " + room.getRoomNumber() + " is full.");
        }
        return room;
    }

    /**
     * Registers a tenant. If roomKey is not blank the tenant is placed in that
     * room; if that fails the tenant is not registered at all.
     */
    public Tenant registerTenant(TenantType type, String name, String phone, String affiliation,
                                 EmergencyContact contact, String roomKey) throws HostelException {
        if (type == null) {
            throw new InvalidTenantException("Tenant type is required.");
        }
        String cleanName = Tenant.validateName(name);
        String cleanPhone = Tenant.validatePhone(phone);
        String cleanAffiliation = Tenant.validateAffiliation(affiliation);
        checkDuplicate(null, cleanName, cleanPhone, cleanAffiliation);
        Room room = (roomKey == null || roomKey.trim().isEmpty()) ? null : checkRoomAvailable(roomKey);

        String id = IdGenerator.next('T');
        LocalDate today = LocalDate.now();
        Tenant tenant = (type == TenantType.STUDENT)
                ? new StudentTenant(id, cleanName, cleanPhone, cleanAffiliation, contact, today)
                : new WorkingTenant(id, cleanName, cleanPhone, cleanAffiliation, contact, today);
        if (room != null) {
            rooms.allocate(room.getId(), tenant.getId(), today);
            tenant.setRoomReference(room);
        }
        tenants.add(tenant);
        save();
        return tenant;
    }

    private void checkDuplicate(Tenant ignore, String name, String phone, String affiliation)
            throws InvalidTenantException {
        for (Tenant t : tenants) {
            if (t == ignore) {
                continue;
            }
            boolean samePhone = !phone.isEmpty() && t.getPhone().equals(phone);
            boolean sameNameAndPlace = t.getFullName().equalsIgnoreCase(name)
                    && t.getAffiliation().equalsIgnoreCase(affiliation);
            if (samePhone || sameNameAndPlace) {
                throw new InvalidTenantException("This looks like a duplicate of tenant " + t.getId()
                        + " (" + t.getFullName() + ").");
            }
        }
    }

    // ----------------------------------------------------------------- find

    public Tenant findTenant(String id) {
        if (id == null) {
            return null;
        }
        for (Tenant t : tenants) {
            if (t.getId().equalsIgnoreCase(id.trim())) {
                return t;
            }
        }
        return null;
    }

    public Tenant requireTenant(String id) throws InvalidTenantException {
        Tenant t = findTenant(id);
        if (t == null) {
            throw new InvalidTenantException("No tenant found with ID: " + (id == null ? "" : id.trim()));
        }
        return t;
    }

    /** Finds tenants whose ID, name, institution/employer, phone or room number contains the text. */
    public List<Tenant> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Tenant> result = new ArrayList<>();
        if (k.isEmpty()) {
            return result;
        }
        for (Tenant t : tenants) {
            String room = t.hasRoom() ? t.getRoom().getRoomNumber() : "";
            if (t.getId().toLowerCase(Locale.ROOT).contains(k)
                    || t.getFullName().toLowerCase(Locale.ROOT).contains(k)
                    || t.getAffiliation().toLowerCase(Locale.ROOT).contains(k)
                    || t.getPhone().contains(k)
                    || room.toLowerCase(Locale.ROOT).contains(k)) {
                result.add(t);
            }
        }
        result.sort(BY_NAME);
        return result;
    }

    public List<Tenant> getAllTenants() {
        List<Tenant> sorted = new ArrayList<>(tenants);
        sorted.sort(BY_NAME);
        return sorted;
    }

    public int getTenantCount() {
        return tenants.size();
    }

    // --------------------------------------------------------------- update

    public Tenant updateTenant(String id, String name, String phone, String affiliation,
                               EmergencyContact contact) throws HostelException {
        Tenant tenant = requireTenant(id);
        String cleanName = Tenant.validateName(name);
        String cleanPhone = Tenant.validatePhone(phone);
        String cleanAffiliation = Tenant.validateAffiliation(affiliation);
        checkDuplicate(tenant, cleanName, cleanPhone, cleanAffiliation);
        tenant.updateDetails(cleanName, cleanPhone, cleanAffiliation, contact);
        save();
        return tenant;
    }

    // ------------------------------------------------- link tenants to rooms

    @Override
    public String tenantName(String tenantId) {
        Tenant t = findTenant(tenantId);
        return t == null ? null : t.getFullName();
    }

    @Override
    public void assignRoom(String tenantId, String roomKey) throws HostelException {
        Tenant tenant = requireTenant(tenantId);
        Room room = rooms.requireRoom(roomKey);
        rooms.allocate(room.getId(), tenant.getId(), LocalDate.now());   // moves them if needed
        tenant.setRoomReference(room);
    }

    @Override
    public void vacateRoom(String tenantId) throws HostelException {
        Tenant tenant = requireTenant(tenantId);
        rooms.release(tenant.getId(), LocalDate.now());
        tenant.setRoomReference(null);
    }

    // --------------------------------------------------------------- remove

    public Tenant removeTenant(String id) throws HostelException {
        Tenant tenant = requireTenant(id);
        for (RemovalListener<Tenant> l : listeners) {
            l.canRemove(tenant);                      // other modules may say no
        }
        if (tenant.hasRoom()) {
            rooms.release(tenant.getId(), LocalDate.now());
            tenant.setRoomReference(null);
        }
        tenants.remove(tenant);
        save();
        for (RemovalListener<Tenant> l : listeners) {
            l.onRemoved(tenant);                      // other modules clean up
        }
        return tenant;
    }

    // -------------------------------------------------------------- reports

    /** All tenants sorted by name. Student and working tenants are mixed in one loop. */
    public String generateAllTenantsReport() {
        StringBuilder sb = new StringBuilder("=== TENANT REPORT (sorted by name) ===\n");
        int students = 0;
        int workers = 0;
        for (Tenant t : getAllTenants()) {
            sb.append(t.describe()).append('\n');       // StudentTenant / WorkingTenant - polymorphic call
            if (t.getType() == TenantType.STUDENT) {
                students++;
            } else {
                workers++;
            }
        }
        if (tenants.isEmpty()) {
            sb.append("No tenants registered.\n");
        }
        sb.append("Tenants: ").append(tenants.size()).append(" | Students: ").append(students)
          .append(" | Working: ").append(workers);
        return sb.toString();
    }

    /** Sorted by room number: shows who lives where. */
    public String generateByRoomReport() {
        List<Tenant> housed = new ArrayList<>();
        for (Tenant t : tenants) {
            if (t.hasRoom()) {
                housed.add(t);
            }
        }
        housed.sort(Comparator.comparing((Tenant t) -> t.getRoom().getRoomNumber(), NaturalOrder.INSTANCE)
                              .thenComparing(BY_NAME));
        StringBuilder sb = new StringBuilder("=== TENANTS BY ROOM ===\n");
        for (Tenant t : housed) {
            sb.append("Room ").append(t.getRoom().getRoomNumber()).append(" -> ").append(t.describe()).append('\n');
        }
        if (housed.isEmpty()) {
            sb.append("No tenants have rooms.\n");
        }
        sb.append("Tenants with rooms: ").append(housed.size());
        return sb.toString();
    }

    /** Filtered: only tenants who still need a room. */
    public String generateNoRoomReport() {
        List<Tenant> waiting = new ArrayList<>();
        for (Tenant t : tenants) {
            if (!t.hasRoom()) {
                waiting.add(t);
            }
        }
        waiting.sort(BY_NAME);
        StringBuilder sb = new StringBuilder("=== TENANTS WITHOUT A ROOM ===\n");
        for (Tenant t : waiting) {
            sb.append(t.describe()).append('\n');
        }
        if (waiting.isEmpty()) {
            sb.append("Every tenant has a room.\n");
        }
        sb.append("Tenants without a room: ").append(waiting.size());
        return sb.toString();
    }

    @Override
    public String generateReport() {
        return generateAllTenantsReport() + "\n\n" + generateByRoomReport() + "\n\n" + generateNoRoomReport();
    }
}
