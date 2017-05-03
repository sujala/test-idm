package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantType;

public interface TenantTypeDao {
    TenantType getTenantType(String name);

    void deleteTenantType(TenantType tenantType);

    void addTenantType(TenantType tenantType);

    PaginatorContext<TenantType> listTenantTypes(Integer marker, Integer limit);

    int countObjects();
}
