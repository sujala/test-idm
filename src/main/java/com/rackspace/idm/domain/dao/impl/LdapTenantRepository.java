package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Tenant;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

@Component
public class LdapTenantRepository extends LdapGenericRepository<Tenant> implements TenantDao {

    public String getBaseDn() {
        return TENANT_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_TENANT;
    }

    @Override
    public void addTenant(Tenant tenant) {
        addObject(tenant);
    }

    @Override
    public void deleteTenant(String tenantId) {
        deleteObject(searchFilterGetTenantById(tenantId));
    }

    @Override
    public Tenant getTenant(String tenantId) {
        return getObject(searchFilterGetTenantById(tenantId));
    }

    @Override
    public Tenant getTenantByName(String name) {
        return getObject(searchFilterGetTenantByName(name));
    }

    @Override
    public Iterable<Tenant> getTenants() {
        return getObjects(searchFilterGetTenants());
    }

    @Override
    public PaginatorContext<Tenant> getTenantsPaged(int offset, int limit) {
        return getObjectsPaged(searchFilterGetTenants(), offset, limit);
    }

    @Override
    public void updateTenant(Tenant tenant) {
        updateObject(tenant);
    }

    private Filter searchFilterGetTenantById(String tenantId) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_ID, tenantId)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_TENANT).build();
    }

    private Filter searchFilterGetTenantByName(String name) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_NAME, name)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_TENANT).build();
    }

    private Filter searchFilterGetTenants() {
         return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_TENANT).build();
    }
}
