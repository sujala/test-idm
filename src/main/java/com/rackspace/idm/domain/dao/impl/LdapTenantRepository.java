package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.unboundid.ldap.sdk.Filter;

@LDAPComponent
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

    @Override
    public void updateTenantAsIs(Tenant tenant) {
        updateObjectAsIs(tenant);
    }

    @Override
    public Iterable<Tenant> getTenantsByBaseUrlId(String baseUrlId) {
        return getObjects(searchFilterGetTenantByBaseUrlId(baseUrlId));
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public int getTenantCount() {
        return countObjects(searchFilterGetTenants());
    }

    private Filter searchFilterGetTenantById(String tenantId) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, tenantId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();
    }

    private Filter searchFilterGetTenantByName(String name) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();
    }

    private Filter searchFilterGetTenantByBaseUrlId(String baseUrlId) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_BASEURL_ID, baseUrlId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();
    }

    private Filter searchFilterGetTenants() {
         return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();
    }
}
