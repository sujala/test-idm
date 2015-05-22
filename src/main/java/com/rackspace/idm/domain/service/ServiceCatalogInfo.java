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

@Getter
public class ServiceCatalogInfo {

    private List<OpenstackEndpoint> userEndpoints;
    private List<TenantRole> userTenantRoles;
    private List<Tenant> userTenants;

    public ServiceCatalogInfo() {
        this(null, null, null);
    }

    public ServiceCatalogInfo( List<TenantRole> userTenantRoles, List<Tenant> userTenants, List<OpenstackEndpoint> userEndpoints) {
        this.userTenantRoles = userTenantRoles == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<TenantRole>(userTenantRoles));
        this.userEndpoints = userEndpoints == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<OpenstackEndpoint>(userEndpoints));
        this.userTenants = userTenants == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(new ArrayList<Tenant>(userTenants));
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
