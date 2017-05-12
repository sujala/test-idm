package com.rackspace.idm.domain.service;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This Class represents the service catalog for a user as well as the metadata for the service catalog.
 * This includes the tenants, roles used to create the service catalog, the user's Identity user type. The
 * IdentityUserTypeEnum is being included here because certain features of identity treat the service catalog
 * differently for a user based on the user's type (ex. Terminator).
 */
@Getter
public class ServiceCatalogInfo {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<OpenstackEndpoint> userEndpoints;
    private List<TenantRole> userTenantRoles;
    private List<Tenant> userTenants;
    private IdentityUserTypeEnum userTypeEnum;

    /**
     * Creates an object with empty endpoints, tenant roles, and tenants and null user type.
     */
    public ServiceCatalogInfo() {
        this(null);
    }

    /**
     * Creates an object with empty endpoints, tenant roles, and tenants with the specified user type.
     *
     * @param userTypeEnum
     */
    public ServiceCatalogInfo(IdentityUserTypeEnum userTypeEnum) {
        this(null, null, null, userTypeEnum);
    }

    public ServiceCatalogInfo( List<TenantRole> userTenantRoles, List<Tenant> userTenants, List<OpenstackEndpoint> userEndpoints, IdentityUserTypeEnum userTypeEnum) {
        this.userTenantRoles = userTenantRoles == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<TenantRole>(userTenantRoles));
        this.userEndpoints = userEndpoints == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<OpenstackEndpoint>(userEndpoints));
        this.userTenants = userTenants == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<Tenant>(userTenants));
        this.userTypeEnum = userTypeEnum;
    }

    /**
     * Returns true if at least one tenant exists in catalog and all tenants are disabled
     * @return
     */
    public boolean allTenantsDisabled() {
        return !userTenants.isEmpty() && CollectionUtils.find(userTenants, new TenantEnabledPredicate()) == null;
    }

    public Tenant findUserTenantById(String id) {
        if (id != null) {
            for (Tenant userTenant : userTenants) {
                if (id.equalsIgnoreCase(userTenant.getTenantId())) {
                    return userTenant;
                }
            }
        }
        return null;
    }

    public Tenant findUserTenantByName(String name) {
        if (name != null) {
            for (Tenant userTenant : userTenants) {
                if (name.equalsIgnoreCase(userTenant.getName())) {
                    return userTenant;
                }
            }
        }
        return null;
    }

    public ServiceCatalogInfo filterEndpointsByTenant(Tenant tenant) {
        List<OpenstackEndpoint> tenantEndpoints = new ArrayList<>();

        for (OpenstackEndpoint endpoint : this.userEndpoints) {
            if (tenant.getTenantId().equalsIgnoreCase(endpoint.getTenantId())) {
                tenantEndpoints.add(endpoint);
            }
        }

        return new ServiceCatalogInfo(userTenantRoles, userTenants, tenantEndpoints, userTypeEnum);
    }

    /**
     * This service finds the a tenant role assigned to the user by name
     *
     * @return
     */
    public TenantRole findAssignedRoleByName(String roleName) {
        for (TenantRole tenantRole : getUserTenantRoles()) {
            if (tenantRole.getName().equalsIgnoreCase(roleName)) {
                return tenantRole;
            }
        }
        return null;
    }

    /**
     * Determine whether the service catalog was generated using RCN role logic. This can be determined simply by
     * looking at the identity user classification role and whether it has
     * tenants associated with it. If it does, then the logic was applied. If not, then it wasn't.
     *
     * This depends on the user having an Identity user classification role - which is a requirement for all users. Identity
     * does not guarantee anything w/ respect to the API for users without such a role. As such, this service will always
     * return false in this case.
     *
     * @return
     */
    public boolean wereRcnRolesAppliedForCatalog() {
        if (userTypeEnum == null) {
            logger.warn("Service Catalog info was generated for a user w/o specifying the user type");
            return false;
        }

        TenantRole userTypeRole = findAssignedRoleByName(userTypeEnum.getRoleName());
        if (userTypeRole == null) {
            logger.warn("Service Catalog info was generated for a user w/o containing a tenant role for the user type");
            return false;
        }

        return CollectionUtils.isNotEmpty(userTypeRole.getTenantIds());
    }

    /**
     * A predicate that will return true if the given tenant is enabled
     */
    private static class TenantEnabledPredicate implements Predicate<Tenant> {
        @Override
        public boolean evaluate(Tenant tenant) {
            return tenant != null && tenant.getEnabled();
        }
    }
}
