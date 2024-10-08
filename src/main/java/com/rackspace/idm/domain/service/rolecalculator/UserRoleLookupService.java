package com.rackspace.idm.domain.service.rolecalculator;

import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.*;

import java.util.List;
import java.util.Map;

public interface UserRoleLookupService {
    String SYSTEM_SOURCE_IDENTITY = "IDENTITY";

    /**
     * Retrieve the user for which roles will be queried.
     * @return
     */
    EndUser getUser();

    /**
     * Retrieve the domain for the user
     * @return
     */
    Domain getUserDomain();

    /**
     * Retrieve the tenantIds for the domain
     * @return
     */
    String[] getTenantIds();

    /**
     * Retrieves the set of tenants within the domains RCN. If the domain doesn't have an RCN, just returns the tenants
     * within the domain.
     *
     * All tenant's must be updated with "inferred" tenant types as necessary.
     *
     * Returns an empty list if no tenants exist within domain/RCN
     *
     * @return
     */
    List<Tenant> calculateRcnTenants();

    /**
     * Retrieves the ImmutableClientRole for a client role with the specified role id
     * @param id
     * @return
     */
    ImmutableClientRole getImmutableClientRole(String id);

    /**
     * Retrieves the tenant roles the user is explicitly assigned
     *
     * @return
     */
    List<TenantRole> getUserSourcedRoles();

    /**
     * Retrieves the set of tenant roles the user receives from each group membership.
     * @return
     */
    Map<String, List<TenantRole>> getGroupSourcedRoles();

    /**
     * Retrieves the set of tenant roles the user receives automatically by the system
     * @return
     */
    Map<String, List<TenantRole>> getSystemSourcedRoles();

    /**
     * Retrieves the set of other source roles the user receives
     */
    Map<TenantRole, RoleAssignmentSource> getOtherSourcedRoles();
}
