package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

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
                if (id.equals(userTenant.getTenantId())) {
                    return userTenant;
                }
            }
        }
        return null;
    }

    public Tenant findUserTenantByName(String name) {
        if (name != null) {
            for (Tenant userTenant : userTenants) {
                if (name.equals(userTenant.getName())) {
                    return userTenant;
                }
            }
        }
        return null;
    }

    public ServiceCatalogInfo filterEndpointsByTenant(Tenant tenant) {
        List<OpenstackEndpoint> tenantEndpoints = new ArrayList<>();

        for (OpenstackEndpoint endpoint : this.userEndpoints) {
            if (tenant.getTenantId().equals(endpoint.getTenantId())) {
                tenantEndpoints.add(endpoint);
            }
        }

        return new ServiceCatalogInfo(userTenantRoles, userTenants, tenantEndpoints, userTypeEnum);
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
