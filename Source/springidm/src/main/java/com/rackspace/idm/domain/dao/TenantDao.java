package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Tenant;

public interface TenantDao {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
}
