package ug.ac.vu.greenview.maintenance;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Validator;
import ug.ac.vu.greenview.rooms.Room;

/**
 * A maintenance request raised by hostel staff about a room (for example after
 * an inspection).
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public class RoomMaintenanceRequest extends MaintenanceRequest {

    private final String requestedBy;

    public RoomMaintenanceRequest(String id, Room room, String requestedBy, IssueCategory category,
                                  Priority priority, String description, LocalDate dateReported)
            throws InvalidMaintenanceException {
        super(id, room, category, priority, description, dateReported);
        if (!Validator.isValidName(requestedBy)) {
            throw new InvalidMaintenanceException("Staff name must be 2 to 60 letters.");
        }
        this.requestedBy = Validator.clean(requestedBy);
    }

    @Override
    public String getRequestType() { return "Room request"; }

    @Override
    public String getTypeCode() { return "ROOMREQ"; }

    @Override
    public String getRaisedBy() { return requestedBy; }

    @Override
    public String getRaisedByTenantId() { return ""; }
}
