package ug.ac.vu.greenview.maintenance;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Validator;
import ug.ac.vu.greenview.rooms.Room;
import ug.ac.vu.greenview.tenants.Tenant;

/**
 * A maintenance complaint raised by a tenant. It keeps an object reference to
 * the tenant, plus a copy of the tenant's name so the history is still readable
 * if the tenant is later removed.
 *
 * @author Yasir Basheer Mohammed (Member 4 - Maintenance Management)
 */
public class TenantComplaint extends MaintenanceRequest {

    private Tenant tenant;            // null once the tenant has been removed
    private final String tenantName;

    public TenantComplaint(String id, Room room, Tenant tenant, String tenantName, IssueCategory category,
                           Priority priority, String description, LocalDate dateReported)
            throws InvalidMaintenanceException {
        super(id, room, category, priority, description, dateReported);
        if (!Validator.isTextOfLength(tenantName, 2, 60)) {
            throw new InvalidMaintenanceException("The complaining tenant's name is required.");
        }
        this.tenant = tenant;
        this.tenantName = Validator.clean(tenantName);
    }

    /** Called by the maintenance module when the tenant is removed. */
    void detachTenant() {
        this.tenant = null;
    }

    public Tenant getTenant() { return tenant; }

    @Override
    public String getRequestType() { return "Tenant complaint"; }

    @Override
    public String getTypeCode() { return "COMPLAINT"; }

    @Override
    public String getRaisedBy() { return tenantName; }

    @Override
    public String getRaisedByTenantId() { return tenant == null ? "" : tenant.getId(); }
}
