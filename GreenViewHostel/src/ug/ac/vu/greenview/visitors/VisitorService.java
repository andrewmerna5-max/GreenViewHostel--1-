package ug.ac.vu.greenview.visitors;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.IdGenerator;
import ug.ac.vu.greenview.core.NaturalOrder;
import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.RemovalListener;
import ug.ac.vu.greenview.core.Reportable;
import ug.ac.vu.greenview.storage.RecordStore;
import ug.ac.vu.greenview.tenants.Tenant;
import ug.ac.vu.greenview.tenants.TenantService;

/**
 * Manages hostel visitors and their visits in ArrayLists: register, find, update,
 * remove, check in, check out and report. Visits are linked to a real Visitor and
 * a real Tenant object. Loads from file when created and saves after every change.
 *
 * @author Khalid Abdalla (Member 5 - Visitors Management)
 */
public final class VisitorService implements Reportable {

    /** The most visitors one tenant may have inside the hostel at the same time. */
    public static final int MAX_VISITORS_AT_ONCE = 3;

    private static final Comparator<Visitor> VISITORS_BY_NAME =
            Comparator.comparing(Visitor::getFullName, String.CASE_INSENSITIVE_ORDER)
                      .thenComparing(Visitor::getId, NaturalOrder.INSTANCE);

    /** Oldest first: used for "who is inside right now". */
    private static final Comparator<Visit> OLDEST_FIRST =
            Comparator.comparing(Visit::getDate)
                      .thenComparing(Visit::getCheckIn)
                      .thenComparing(Visit::getId, NaturalOrder.INSTANCE);

    /** Newest first: used for the history report. */
    private static final Comparator<Visit> NEWEST_FIRST =
            Comparator.comparing(Visit::getDate, Comparator.reverseOrder())
                      .thenComparing(Visit::getCheckIn, Comparator.reverseOrder())
                      .thenComparing(Visit::getId, NaturalOrder.INSTANCE);

    private final ArrayList<Visitor> visitors = new ArrayList<>();
    private final ArrayList<Visit> visits = new ArrayList<>();
    private final RecordStore<Visitor> visitorStore;
    private final RecordStore<Visit> visitStore;
    private final TenantService tenants;

    public VisitorService(Path dataDir, TenantService tenants) {
        this.tenants = tenants;
        this.visitorStore = new RecordStore<>(dataDir.resolve("visitors.txt"), Visitor::fromFields);
        this.visitStore = new RecordStore<>(dataDir.resolve("visits.txt"), this::parseVisit);
        visitors.addAll(visitorStore.load());
        visits.addAll(visitStore.load());
    }

    /** Rebuilds one visit, re-linking it to the real Visitor and Tenant objects. */
    private Visit parseVisit(String[] f) throws Exception {
        if (f.length < 7) {
            throw new IllegalArgumentException("expected 7 fields but found " + f.length);
        }
        Visitor visitor = findVisitor(f[1]);
        if (visitor == null) {
            throw new IllegalArgumentException("visitor " + f[1] + " no longer exists");
        }
        Tenant tenant = tenants.findTenant(f[2]);
        if (tenant == null) {
            throw new IllegalArgumentException("tenant " + f[2] + " no longer exists");
        }
        Visit visit = new Visit(f[0], visitor, tenant, LocalDate.parse(f[3].trim()),
                LocalTime.parse(f[4].trim(), Visit.TIME_FORMAT), f[6]);
        if (!f[5].trim().isEmpty()) {
            visit.checkOutAt(LocalTime.parse(f[5].trim(), Visit.TIME_FORMAT));
        }
        return visit;
    }

    @Override
    public String getModuleName() {
        return "Visitors";
    }

    private void saveVisitors() {
        visitorStore.save(visitors);
    }

    private void saveVisits() {
        visitStore.save(visits);
    }

    // --------------------------------------------------------------- visitors

    public Visitor registerVisitor(String name, String phone) throws HostelException {
        String cleanName = Visitor.validateName(name);
        String cleanPhone = Visitor.validatePhone(phone);
        checkDuplicate(null, cleanName, cleanPhone);
        Visitor visitor = new Visitor(IdGenerator.next('V'), cleanName, cleanPhone, LocalDate.now());
        visitors.add(visitor);
        saveVisitors();
        return visitor;
    }

    private void checkDuplicate(Visitor ignore, String name, String phone) throws InvalidVisitorException {
        for (Visitor v : visitors) {
            if (v != ignore && v.getFullName().equalsIgnoreCase(name) && v.getPhone().equals(phone)) {
                throw new InvalidVisitorException("This visitor is already registered as " + v.getId()
                        + ". Add a phone number if this is a different person with the same name.");
            }
        }
    }

    public Visitor findVisitor(String id) {
        if (id == null) {
            return null;
        }
        for (Visitor v : visitors) {
            if (v.getId().equalsIgnoreCase(id.trim())) {
                return v;
            }
        }
        return null;
    }

    public Visitor requireVisitor(String id) throws InvalidVisitorException {
        Visitor v = findVisitor(id);
        if (v == null) {
            throw new InvalidVisitorException("No visitor found with ID: " + (id == null ? "" : id.trim()));
        }
        return v;
    }

    public List<Visitor> searchVisitors(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Visitor> result = new ArrayList<>();
        if (k.isEmpty()) {
            return result;
        }
        for (Visitor v : visitors) {
            if (v.getId().toLowerCase(Locale.ROOT).contains(k)
                    || v.getFullName().toLowerCase(Locale.ROOT).contains(k)
                    || v.getPhone().contains(k)) {
                result.add(v);
            }
        }
        result.sort(VISITORS_BY_NAME);
        return result;
    }

    public List<Visitor> getAllVisitors() {
        List<Visitor> sorted = new ArrayList<>(visitors);
        sorted.sort(VISITORS_BY_NAME);
        return sorted;
    }

    public Visitor updateVisitor(String id, String name, String phone) throws HostelException {
        Visitor visitor = requireVisitor(id);
        String cleanName = Visitor.validateName(name);
        String cleanPhone = Visitor.validatePhone(phone);
        checkDuplicate(visitor, cleanName, cleanPhone);
        visitor.updateDetails(cleanName, cleanPhone);
        saveVisitors();
        return visitor;
    }

    /** A visitor who is still inside cannot be removed; their visit history is removed with them. */
    public Visitor removeVisitor(String id) throws HostelException {
        Visitor visitor = requireVisitor(id);
        for (Visit v : visits) {
            if (v.getVisitor() == visitor && v.isInside()) {
                throw new InvalidVisitorException(visitor.getFullName() + " is still inside (visit " + v.getId()
                        + "). Check them out first.");
            }
        }
        visitors.remove(visitor);
        visits.removeIf(v -> v.getVisitor() == visitor);
        saveVisitors();
        saveVisits();
        return visitor;
    }

    // ----------------------------------------------------------------- visits

    public Visit checkIn(String visitorId, String tenantId, LocalDate date, LocalTime time, String purpose)
            throws HostelException {
        Visitor visitor = requireVisitor(visitorId);
        Tenant tenant = tenants.requireTenant(tenantId);
        Visit.validatePurpose(purpose);
        for (Visit v : visits) {
            if (v.getVisitor() == visitor && v.isInside()) {
                throw new InvalidVisitorException(visitor.getFullName() + " is already inside (visit " + v.getId()
                        + "). Check them out first.");
            }
        }
        int inside = countInside(tenant);
        if (inside >= MAX_VISITORS_AT_ONCE) {
            throw new VisitorLimitExceededException(tenant.getFullName() + " already has " + inside
                    + " visitors inside. The limit is " + MAX_VISITORS_AT_ONCE + " at a time.");
        }
        Visit visit = new Visit(IdGenerator.next('L'), visitor, tenant, date, time, purpose);
        visits.add(visit);
        saveVisits();
        return visit;
    }

    private int countInside(Tenant tenant) {
        int count = 0;
        for (Visit v : visits) {
            if (v.getTenant() == tenant && v.isInside()) {
                count++;
            }
        }
        return count;
    }

    public Visit checkOut(String visitId, LocalTime time) throws HostelException {
        Visit visit = requireVisit(visitId);
        visit.checkOutAt(time);
        saveVisits();
        return visit;
    }

    public Visit updateVisit(String visitId, LocalDate date, LocalTime checkIn, String purpose) throws HostelException {
        Visit visit = requireVisit(visitId);
        visit.reschedule(date, checkIn, purpose);
        saveVisits();
        return visit;
    }

    public Visit removeVisit(String visitId) throws HostelException {
        Visit visit = requireVisit(visitId);
        visits.remove(visit);
        saveVisits();
        return visit;
    }

    public Visit findVisit(String id) {
        if (id == null) {
            return null;
        }
        for (Visit v : visits) {
            if (v.getId().equalsIgnoreCase(id.trim())) {
                return v;
            }
        }
        return null;
    }

    public Visit requireVisit(String id) throws InvalidVisitorException {
        Visit v = findVisit(id);
        if (v == null) {
            throw new InvalidVisitorException("No visit found with ID: " + (id == null ? "" : id.trim()));
        }
        return v;
    }

    /** Matches the visit ID, visitor, tenant, purpose or date. Newest first. */
    public List<Visit> searchVisits(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Visit> result = new ArrayList<>();
        if (k.isEmpty()) {
            return result;
        }
        for (Visit v : visits) {
            if (v.getId().toLowerCase(Locale.ROOT).contains(k)
                    || v.getVisitor().getFullName().toLowerCase(Locale.ROOT).contains(k)
                    || v.getVisitor().getId().toLowerCase(Locale.ROOT).contains(k)
                    || v.getTenant().getFullName().toLowerCase(Locale.ROOT).contains(k)
                    || v.getTenant().getId().toLowerCase(Locale.ROOT).contains(k)
                    || v.getPurpose().toLowerCase(Locale.ROOT).contains(k)
                    || v.getDate().toString().contains(k)) {
                result.add(v);
            }
        }
        result.sort(NEWEST_FIRST);
        return result;
    }

    public List<Visit> getAllVisits() {
        List<Visit> sorted = new ArrayList<>(visits);
        sorted.sort(NEWEST_FIRST);
        return sorted;
    }

    /** Visitors and visits together, as plain Records (mixed objects). */
    public List<Record> getAllRecords() {
        List<Record> all = new ArrayList<>(getAllVisitors());
        all.addAll(getAllVisits());
        return all;
    }

    public int getVisitorCount() {
        return visitors.size();
    }

    public int getVisitCount() {
        return visits.size();
    }

    // ---------------------------------------------- keeping modules consistent

    /** Registered with the Tenants module: no removal while a visitor is inside; deletes their visits afterwards. */
    public RemovalListener<Tenant> tenantListener() {
        return new RemovalListener<Tenant>() {
            @Override
            public void canRemove(Tenant tenant) throws HostelException {
                int inside = countInside(tenant);
                if (inside > 0) {
                    throw new InvalidVisitorException("Cannot remove " + tenant.getFullName() + ": " + inside
                            + " visitor(s) are still inside. Check them out first.");
                }
            }

            @Override
            public void onRemoved(Tenant tenant) {
                if (visits.removeIf(v -> v.getTenant() == tenant)) {
                    saveVisits();
                }
            }
        };
    }

    // ---------------------------------------------------------------- reports

    /** FILTERED + SORTED: visitors who have not checked out, oldest check-in first. */
    public String generateInsideReport() {
        List<Visit> inside = new ArrayList<>();
        for (Visit v : visits) {
            if (v.isInside()) {
                inside.add(v);
            }
        }
        inside.sort(OLDEST_FIRST);
        StringBuilder sb = new StringBuilder("=== VISITORS CURRENTLY INSIDE ===\n");
        for (Visit v : inside) {
            sb.append(v.describe()).append('\n');
        }
        if (inside.isEmpty()) {
            sb.append("Nobody is inside.\n");
        }
        sb.append("Visitors inside: ").append(inside.size());
        return sb.toString();
    }

    /** SORTED: every visit, newest first. */
    public String generateHistoryReport() {
        StringBuilder sb = new StringBuilder("=== VISIT HISTORY (newest first) ===\n");
        for (Visit v : getAllVisits()) {
            sb.append(v.describe()).append('\n');
        }
        if (visits.isEmpty()) {
            sb.append("No visits recorded.\n");
        }
        sb.append("Visits: ").append(visits.size());
        return sb.toString();
    }

    /** FILTERED: visits on one date, in check-in order. */
    public String generateDateReport(LocalDate date) {
        List<Visit> onDate = new ArrayList<>();
        for (Visit v : visits) {
            if (v.getDate().equals(date)) {
                onDate.add(v);
            }
        }
        onDate.sort(OLDEST_FIRST);
        StringBuilder sb = new StringBuilder("=== VISITS ON " + date + " ===\n");
        for (Visit v : onDate) {
            sb.append(v.describe()).append('\n');
        }
        if (onDate.isEmpty()) {
            sb.append("No visits on that date.\n");
        }
        sb.append("Visits: ").append(onDate.size());
        return sb.toString();
    }

    /** FILTERED + SORTED: all visits to one tenant, newest first. */
    public String generateTenantReport(String tenantId) throws HostelException {
        Tenant tenant = tenants.requireTenant(tenantId);
        List<Visit> mine = new ArrayList<>();
        for (Visit v : visits) {
            if (v.getTenant() == tenant) {
                mine.add(v);
            }
        }
        mine.sort(NEWEST_FIRST);
        StringBuilder sb = new StringBuilder("=== VISITS TO " + tenant.getFullName() + " (" + tenant.getId() + ") ===\n");
        for (Visit v : mine) {
            sb.append(v.describe()).append('\n');
        }
        if (mine.isEmpty()) {
            sb.append("No visits recorded for this tenant.\n");
        }
        sb.append("Visits: ").append(mine.size());
        return sb.toString();
    }

    @Override
    public String generateReport() {
        return generateInsideReport() + "\n\n" + generateHistoryReport();
    }
}
