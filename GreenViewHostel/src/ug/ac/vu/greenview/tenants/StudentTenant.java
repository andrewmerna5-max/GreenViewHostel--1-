package ug.ac.vu.greenview.tenants;

import java.time.LocalDate;

/**
 * A tenant who studies at a university or college.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public class StudentTenant extends Tenant {

    public StudentTenant(String id, String fullName, String phone, String institution,
                         EmergencyContact contact, LocalDate dateRegistered) throws InvalidTenantException {
        super(id, fullName, phone, institution, contact, dateRegistered);
    }

    @Override
    public TenantType getType() {
        return TenantType.STUDENT;
    }

    @Override
    public String getAffiliationLabel() {
        return "Institution";
    }
}
