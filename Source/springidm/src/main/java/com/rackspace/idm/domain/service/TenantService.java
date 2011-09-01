package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Tenant;

public interface TenantService {

    void addTenant(Tenant tenant);
    void deleteTenant(String tenantId);
    Tenant getTenant(String tenantId);
    List<Tenant> getTenants();
    void updateTenant(Tenant tenant);
}
