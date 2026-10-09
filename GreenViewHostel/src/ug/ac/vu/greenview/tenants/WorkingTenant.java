package ug.ac.vu.greenview.tenants;

import java.time.LocalDate;

/**
 * A tenant who has a job.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public class WorkingTenant extends Tenant {

    public WorkingTenant(String id, String fullName, String phone, String employer,
                         EmergencyContact contact, LocalDate dateRegistered) throws InvalidTenantException {
        super(id, fullName, phone, employer, contact, dateRegistered);
    }

    @Override
    public TenantType getType() {
        return TenantType.WORKING;
    }

    @Override
    public String getAffiliationLabel() {
        return "Employer";
    }
}
