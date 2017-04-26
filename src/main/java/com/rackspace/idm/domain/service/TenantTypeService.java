package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantType;

public interface TenantTypeService {
    void deleteTenantType(TenantType tenantType);

    void createTenantType(TenantType tenantType);

    PaginatorContext<TenantType> listTenantTypes(Integer marker, Integer limit);

    TenantType checkAndGetTenantType(String tenantTypeId);
}
