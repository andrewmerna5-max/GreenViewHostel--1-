package ug.ac.vu.greenview.maintenance;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Validator;
import ug.ac.vu.greenview.rooms.Room;

/**
 * Something in a room that needs repair. Abstract: a request is either a
 * TenantComplaint (raised by a tenant) or a RoomMaintenanceRequest (raised by
 * hostel staff). Holds an object reference to the Room it is about.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public abstract class MaintenanceRequest extends Record {

    public static final int MIN_DESCRIPTION = 5;
    public static final int MAX_DESCRIPTION = 200;
    public static final int MAX_NOTES = 200;

    private final Room room;
    private IssueCategory category;
    private Priority priority;
    private String description;
    private RepairStatus status = RepairStatus.PENDING;
    private final LocalDate dateReported;
    private LocalDate dateCompleted;
    private String resolutionNotes = "";

    protected MaintenanceRequest(String id, Room room, IssueCategory category, Priority priority,
                                 String description, LocalDate dateReported) throws InvalidMaintenanceException {
        super(id);
        if (room == null) {
            throw new InvalidMaintenanceException("A room is required.");
        }
        if (dateReported == null || dateReported.isAfter(LocalDate.now())) {
            throw new InvalidMaintenanceException("Report date is required and cannot be in the future.");
        }
        this.room = room;
        this.category = requireCategory(category);
        this.priority = requirePriority(priority);
        this.description = validateDescription(description);
        this.dateReported = dateReported;
    }

    // ------------------------------------------------------ validation rules

    public static String validateDescription(String text) throws InvalidMaintenanceException {
        if (!Validator.isTextOfLength(text, MIN_DESCRIPTION, MAX_DESCRIPTION)) {
            throw new InvalidMaintenanceException("Description must be " + MIN_DESCRIPTION + " to "
                    + MAX_DESCRIPTION + " characters.");
        }
        return Validator.clean(text);
    }

    private static IssueCategory requireCategory(IssueCategory c) throws InvalidMaintenanceException {
        if (c == null) {
            throw new InvalidMaintenanceException("Issue category is required.");
        }
        return c;
    }

    private static Priority requirePriority(Priority p) throws InvalidMaintenanceException {
        if (p == null) {
            throw new InvalidMaintenanceException("Priority is required.");
        }
        return p;
    }

    // ------------------------------------------------- what subclasses decide

    /** "Tenant complaint" or "Room request". */
    public abstract String getRequestType();

    /** Code saved in the file. */
    public abstract String getTypeCode();

    /** Who raised it (a tenant's name, or a staff member's name). */
    public abstract String getRaisedBy();

    /** The tenant's ID for complaints that still have a tenant; "" otherwise. */
    public abstract String getRaisedByTenantId();

    // --------------------------------------------------------------- getters

    public Room getRoom() { return room; }
    public IssueCategory getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public String getDescription() { return description; }
    public RepairStatus getStatus() { return status; }
    public LocalDate getDateReported() { return dateReported; }
    public LocalDate getDateCompleted() { return dateCompleted; }
    public String getResolutionNotes() { return resolutionNotes; }
    public boolean isCompleted() { return status == RepairStatus.COMPLETED; }
    public boolean isOpen() { return status != RepairStatus.COMPLETED; }

    // --------------------------------------------------------------- changes

    /** Changes the details of a request that is not finished yet. */
    public final void updateDetails(IssueCategory newCategory, Priority newPriority, String newDescription)
            throws InvalidMaintenanceException {
        if (isCompleted()) {
            throw new InvalidMaintenanceException("A completed request cannot be edited.");
        }
        IssueCategory c = requireCategory(newCategory);
        Priority p = requirePriority(newPriority);
        String d = validateDescription(newDescription);
        this.category = c;
        this.priority = p;
        this.description = d;
    }

    /** PENDING -> IN_PROGRESS. */
    public final void startRepair() throws InvalidMaintenanceException {
        if (status != RepairStatus.PENDING) {
            throw new InvalidMaintenanceException("Only a PENDING request can be started (this one is "
                    + status + ").");
        }
        status = RepairStatus.IN_PROGRESS;
    }

    /** PENDING or IN_PROGRESS -> COMPLETED. */
    public final void markCompleted(LocalDate date, String notes) throws InvalidMaintenanceException {
        if (isCompleted()) {
            throw new InvalidMaintenanceException("This request is already completed.");
        }
        if (date == null) {
            throw new InvalidMaintenanceException("Completion date is required.");
        }
        if (date.isBefore(dateReported)) {
            throw new InvalidMaintenanceException("Completion date cannot be before the report date (" + dateReported + ").");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidMaintenanceException("Completion date cannot be in the future.");
        }
        String n = Validator.clean(notes);
        if (n.length() > MAX_NOTES) {
            throw new InvalidMaintenanceException("Notes can be at most " + MAX_NOTES + " characters.");
        }
        status = RepairStatus.COMPLETED;
        dateCompleted = date;
        resolutionNotes = n;
    }

    // ------------------------------------------------------- Record methods

    @Override
    public String describe() {
        return String.format("%s | %s | Room: %s | By: %s | %s | Priority: %s | Reported: %s | Status: %s%s | \"%s\"%s",
                getId(), getRequestType(), room.getRoomNumber(), getRaisedBy(), category, priority,
                dateReported, status,
                dateCompleted == null ? "" : " (" + dateCompleted + ")",
                description,
                resolutionNotes.isEmpty() ? "" : " | Notes: " + resolutionNotes);
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), getTypeCode(), room.getId(), getRaisedByTenantId(), getRaisedBy(),
                category.name(), priority.name(), description, status.name(), dateReported.toString(),
                dateCompleted == null ? "" : dateCompleted.toString(), resolutionNotes};
    }
}
