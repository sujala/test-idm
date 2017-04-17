package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.TenantTypeDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantType;
import com.unboundid.ldap.sdk.Filter;

@LDAPComponent
public class LdapTenantTypeRepository extends LdapGenericRepository<TenantType> implements TenantTypeDao {

    public String getBaseDn() {
        return TENANT_TYPE_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_TENANT_TYPE;
    }

    @Override
    public TenantType getTenantType(String name) {
        return getObject(searchFilterGetTenantTypeByName(name));
    }

    @Override
    public void deleteTenantType(TenantType tenantType) {
        deleteObject(tenantType);
    }

    @Override
    public void addTenantType(TenantType tenantType) {
        addObject(tenantType);
    }

    @Override
    public PaginatorContext<TenantType> listTenantTypes(Integer marker, Integer limit) {
        return getObjectsPaged(searchFilterGetTenantType(), marker, limit);
    }

    @Override
    public int countObjects() {
        return countObjects(searchFilterGetTenantType());
    }

    @Override
    public String getSortAttribute() {
        return ATTR_NAME;
    }

    private Filter searchFilterGetTenantTypeByName(String name) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_TYPE).build();
    }

    private Filter searchFilterGetTenantType() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_TYPE)
                .build();
    }
}
