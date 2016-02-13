package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.List;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    Tenant getTenantByName(String name);
    Iterable<Tenant> getTenants();
    PaginatorContext<Tenant> getTenantsPaged(int offset, int limit);
    void updateTenant(Tenant tenant);
    void updateTenantAsIs(Tenant tenant);
}
