package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    Iterable<Tenant> getTenants();
    PaginatorContext<Tenant> getTenantsPaged(int offset, int limit);
    void updateTenant(Tenant tenant);
    void updateTenantAsIs(Tenant tenant);


    /**
     * Returns a list of tenants associated with a baseUrl
     * @return
     */
    Iterable<Tenant> getTenantsByBaseUrlId(String baseUrlId);

    /**
     * Returns a list of tenants associated with a domainId
     * @return
     */
    Iterable<Tenant> getTenantsByDomainId(String domainId);

    /**
     * Returns the total number of tenants
     *
     * @return
     */
    int getTenantCount();

    int countTenantsByTenantType(String tenantType);

    int countTenantsWithTypeInDomain(String tenantType, String domainId);
}
