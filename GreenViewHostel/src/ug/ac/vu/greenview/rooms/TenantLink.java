package ug.ac.vu.greenview.rooms;

import ug.ac.vu.greenview.core.HostelException;

/**
 * The only thing the Rooms module needs to know about tenants. The tenants
 * module provides the real behaviour (wired together in the app package), so
 * the two modules stay loosely coupled.
 *
 * @author Josephina Ayok Weiu (Member 1 - Rooms Management)
 */
public interface TenantLink {

    /** Name of the tenant with this ID, or null if there is no such tenant. */
    String tenantName(String tenantId);

    /** Puts the tenant in the room (moving them if they already have one). */
    void assignRoom(String tenantId, String roomKey) throws HostelException;

    /** Takes the tenant out of their current room. */
    void vacateRoom(String tenantId) throws HostelException;
}
