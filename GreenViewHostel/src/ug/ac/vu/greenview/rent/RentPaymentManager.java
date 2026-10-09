package ug.ac.vu.greenview.rent;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ug.ac.vu.greenview.core.AppConfig;
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
 * Manages all rent records in an ArrayList: add, find, update, remove, record
 * payments, check status and produce paid / unpaid reports. Links every payment
 * to a real Tenant and Room object. Loads from file when created and saves
 * after every change.
 *
 * @author Nabongo Mark (Member 3 - Rent Payment Management)
 */
public final class RentPaymentManager implements Reportable {

    private final ArrayList<RentPayment> payments = new ArrayList<>();
    private final RecordStore<RentPayment> store;
    private final TenantService tenants;

    public RentPaymentManager(Path dataDir, TenantService tenants, RoomService rooms) {
        this.tenants = tenants;
        this.store = new RecordStore<>(dataDir.resolve("rent.txt"), fields -> parse(fields, tenants, rooms));
        payments.addAll(store.load());
    }

    /** Rebuilds one rent record, re-linking it to the real Tenant and Room objects. */
    private static RentPayment parse(String[] f, TenantService tenants, RoomService rooms) throws Exception {
        if (f.length < 6) {
            throw new IllegalArgumentException("expected 6 fields but found " + f.length);
        }
        Tenant tenant = tenants.findTenant(f[1]);
        if (tenant == null) {
            throw new IllegalArgumentException("tenant " + f[1] + " no longer exists");
        }
        Room room = rooms.findById(f[2]);
        if (room == null) {
            throw new IllegalArgumentException("room " + f[2] + " no longer exists");
        }
        RentPayment p = new RentPayment(f[0], tenant, room, YearMonth.parse(f[3].trim()), Long.parseLong(f[4].trim()));
        if (!f[5].trim().isEmpty()) {
            for (String entry : f[5].split(";")) {
                PaymentTransaction t = PaymentTransaction.fromFileText(entry);
                p.applyPayment(t.getAmount(), t.getDate());
            }
        }
        return p;
    }

    @Override
    public String getModuleName() {
        return "Rent Payments";
    }

    private void save() {
        store.save(payments);
    }

    // -------------------------------------------------------------------- add

    /**
     * Creates a rent charge for the tenant's CURRENT room and a month. Starts as
     * UNPAID. If amountDue is null the room's monthly rent is used.
     */
    public RentPayment addPayment(String tenantId, YearMonth period, Long amountDue) throws HostelException {
        Tenant tenant = tenants.requireTenant(tenantId);
        Room room = tenant.getRoom();
        if (room == null) {
            throw new InvalidPaymentException("Tenant " + tenant.getId() + " has no room yet. Allocate a room first.");
        }
        RentPayment.validatePeriod(period);
        long due = amountDue == null ? room.getMonthlyRent() : amountDue;
        RentPayment.validateAmountDue(due);
        for (RentPayment p : payments) {
            if (p.getTenant() == tenant && p.getRoom() == room && p.getPeriod().equals(period)) {
                throw new InvalidPaymentException("A rent record already exists for tenant " + tenant.getId()
                        + ", room " + room.getRoomNumber() + ", period " + period + " (" + p.getId() + ").");
            }
        }
        RentPayment payment = new RentPayment(IdGenerator.next('P'), tenant, room, period, due);
        payments.add(payment);
        save();
        return payment;
    }

    // ----------------------------------------------------------- take payment

    /** Records money received against an existing rent record (amount + date). */
    public RentPayment recordPayment(String paymentId, long amount, LocalDate date) throws HostelException {
        RentPayment payment = requirePayment(paymentId);
        payment.applyPayment(amount, date);
        save();
        return payment;
    }

    public PaymentStatus checkStatus(String paymentId) throws HostelException {
        return requirePayment(paymentId).getStatus();
    }

    // ----------------------------------------------------------------- update

    public RentPayment updateAmountDue(String paymentId, long newDue) throws HostelException {
        RentPayment payment = requirePayment(paymentId);
        payment.changeAmountDue(newDue);
        save();
        return payment;
    }

    public RentPayment correctPayment(String paymentId, int number, long newAmount, LocalDate newDate)
            throws HostelException {
        RentPayment payment = requirePayment(paymentId);
        payment.correctTransaction(number, newAmount, newDate);
        save();
        return payment;
    }

    public RentPayment deletePaymentEntry(String paymentId, int number) throws HostelException {
        RentPayment payment = requirePayment(paymentId);
        payment.removeTransaction(number);
        save();
        return payment;
    }

    // ----------------------------------------------------------------- remove

    public RentPayment removePayment(String paymentId) throws HostelException {
        RentPayment payment = requirePayment(paymentId);
        payments.remove(payment);
        save();
        return payment;
    }

    // -------------------------------------------------------------- searching

    public RentPayment findById(String paymentId) {
        if (paymentId == null) {
            return null;
        }
        for (RentPayment p : payments) {
            if (p.getId().equalsIgnoreCase(paymentId.trim())) {
                return p;
            }
        }
        return null;
    }

    public List<RentPayment> findByTenant(String tenantId) {
        List<RentPayment> result = new ArrayList<>();
        for (RentPayment p : payments) {
            if (tenantId != null && p.getTenant().getId().equalsIgnoreCase(tenantId.trim())) {
                result.add(p);
            }
        }
        result.sort(BY_TENANT_THEN_PERIOD);
        return result;
    }

    public List<RentPayment> findByRoom(String roomKey) {
        List<RentPayment> result = new ArrayList<>();
        for (RentPayment p : payments) {
            if (roomKey != null && (p.getRoom().getId().equalsIgnoreCase(roomKey.trim())
                    || p.getRoom().getRoomNumber().equalsIgnoreCase(roomKey.trim()))) {
                result.add(p);
            }
        }
        result.sort(BY_TENANT_THEN_PERIOD);
        return result;
    }

    public List<RentPayment> getAll() {
        List<RentPayment> all = new ArrayList<>(payments);
        all.sort(BY_TENANT_THEN_PERIOD);
        return all;
    }

    public int getCount() {
        return payments.size();
    }

    private RentPayment requirePayment(String paymentId) throws InvalidPaymentException {
        RentPayment p = findById(paymentId);
        if (p == null) {
            throw new InvalidPaymentException("No rent record found with ID: " + (paymentId == null ? "" : paymentId.trim()));
        }
        return p;
    }

    // ---------------------------------------------- keeping modules consistent

    /** Registered with the Tenants module: stops removal while rent is owed, cleans up afterwards. */
    public RemovalListener<Tenant> tenantListener() {
        return new RemovalListener<Tenant>() {
            @Override
            public void canRemove(Tenant tenant) throws HostelException {
                long owed = 0;
                for (RentPayment p : payments) {
                    if (p.getTenant() == tenant && p.getBalance() > 0) {
                        owed += p.getBalance();
                    }
                }
                if (owed > 0) {
                    throw new InvalidPaymentException("Cannot remove " + tenant.getFullName() + ": they still owe "
                            + AppConfig.money(owed) + ". Record the payment or correct the rent record first.");
                }
            }

            @Override
            public void onRemoved(Tenant tenant) {
                if (payments.removeIf(p -> p.getTenant() == tenant)) {
                    save();
                }
            }
        };
    }

    /** Registered with the Rooms module: stops removal while rent is owed for the room. */
    public RemovalListener<Room> roomListener() {
        return new RemovalListener<Room>() {
            @Override
            public void canRemove(Room room) throws HostelException {
                for (RentPayment p : payments) {
                    if (p.getRoom() == room && p.getBalance() > 0) {
                        throw new InvalidPaymentException("Cannot remove room " + room.getRoomNumber()
                                + ": rent record " + p.getId() + " for it is not fully paid.");
                    }
                }
            }

            @Override
            public void onRemoved(Room room) {
                if (payments.removeIf(p -> p.getRoom() == room)) {
                    save();
                }
            }
        };
    }

    // ---------------------------------------------------------------- reports

    /** Sorted by tenant name, then by month. */
    private static final Comparator<RentPayment> BY_TENANT_THEN_PERIOD =
            Comparator.comparing((RentPayment p) -> p.getTenant().getFullName(), String.CASE_INSENSITIVE_ORDER)
                      .thenComparing(p -> p.getTenant().getId(), NaturalOrder.INSTANCE)
                      .thenComparing(RentPayment::getPeriod);

    public String generatePaidReport() {
        StringBuilder sb = new StringBuilder("=== PAID RENT REPORT ===\n");
        long total = 0;
        int count = 0;
        for (RentPayment p : getAll()) {
            if (p.getStatus() == PaymentStatus.PAID) {
                sb.append(p.describe()).append('\n');
                total += p.getAmountPaid();
                count++;
            }
        }
        if (count == 0) {
            sb.append("No fully paid records.\n");
        }
        sb.append("Records: ").append(count).append(" | Total collected: ").append(AppConfig.money(total));
        return sb.toString();
    }

    public String generateUnpaidReport() {
        StringBuilder sb = new StringBuilder("=== UNPAID RENT REPORT (unpaid and part-paid) ===\n");
        long totalOwed = 0;
        int count = 0;
        for (RentPayment p : getAll()) {
            if (p.getStatus() != PaymentStatus.PAID) {
                sb.append(p.describe()).append('\n');
                totalOwed += p.getBalance();
                count++;
            }
        }
        if (count == 0) {
            sb.append("No outstanding rent.\n");
        }
        sb.append("Records: ").append(count).append(" | Total owed: ").append(AppConfig.money(totalOwed));
        return sb.toString();
    }

    // Required by Reportable: full report = paid + unpaid.
    @Override
    public String generateReport() {
        return generatePaidReport() + "\n\n" + generateUnpaidReport();
    }
}
