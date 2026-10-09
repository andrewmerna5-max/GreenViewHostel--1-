package ug.ac.vu.greenview.tenants;

import ug.ac.vu.greenview.core.HostelException;

/**
 * Thrown when tenant data or a tenant operation breaks a rule
 * (bad name, duplicate tenant, tenant not found...).
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public class InvalidTenantException extends HostelException {

    private static final long serialVersionUID = 1L;

    public InvalidTenantException(String message) {
        super(message);
    }
}
