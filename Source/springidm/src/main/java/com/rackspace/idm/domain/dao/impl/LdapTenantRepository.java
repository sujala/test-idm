package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;

public class LdapTenantRepository extends LdapRepository implements TenantDao {

    public LdapTenantRepository(LdapConnectionPools connPools,
        Configuration config) {
        super(connPools, config);
    }

    @Override
    public void addTenant(Tenant tenant) {
        if (tenant == null) {
            String errmsg = "Null instance of Tenant was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().info("Adding Tenant: {}", tenant);
        Audit audit = Audit.log(tenant).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister<Tenant> persister = LDAPPersister
                .getInstance(Tenant.class);
            persister.add(tenant, conn, TENANT_BASE_DN);
            audit.succeed();
            getLogger().info("Added Tenant: {}", tenant);
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                String errMsg = String.format("Tenant %s already exists",
                    tenant.getTenantId());
                getLogger().warn(errMsg);
                throw new DuplicateException(errMsg);
            }
            getLogger().error("Error adding tenant object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public void deleteTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error("Null or Empty tenantId parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }
        Tenant tenant = getTenant(tenantId);
        if (tenant == null) {
            String errMsg = String.format("Tenant %s not found", tenantId);
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        getLogger().debug("Deleting Tenant: {}", tenant);
        final String dn = tenant.getUniqueId();
        final Audit audit = Audit.log(tenant).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted Tenant: {}", tenant);
    }

    @Override
    public Tenant getTenant(String tenantId) {
        getLogger().debug("Doing search for tenantId " + tenantId);
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error("Null or Empty tenantId parameter");
            throw new IllegalArgumentException("Null or Empty inum parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_TENANT_ID, tenantId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        Tenant tenant = null;
        ;
        try {
            tenant = getSingleTenant(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Tenant - {}", tenant);

        return tenant;
    }

    @Override
    public List<Tenant> getTenants() {
        getLogger().debug("Getting tenants");
        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        List<Tenant> tenants = new ArrayList<Tenant>();
        try {
            tenants = getMultipleTenants(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} tenants", tenants.size());

        return tenants;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        if (tenant == null || StringUtils.isBlank(tenant.getUniqueId())) {
            String errmsg = "Null instance of Tenant was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating Tenant: {}", tenant);
        LDAPConnection conn = null;
        Audit audit = Audit.log(tenant);
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister<Tenant> persister = LDAPPersister
                .getInstance(Tenant.class);
            List<Modification> modifications = persister.getModifications(
                tenant, true);
            audit.modify(modifications);
            persister.modify(tenant, conn, null, true);
            getLogger().debug("Updated Tenant: {}", tenant);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating tenant", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    private List<Tenant> getMultipleTenants(Filter searchFilter)
        throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(
            TENANT_BASE_DN, SearchScope.SUB, searchFilter, ATTR_TENANT_ID);

        List<Tenant> tenants = new ArrayList<Tenant>();
        for (SearchResultEntry entry : entries) {
            tenants.add(getTenant(entry));
        }
        return tenants;
    }

    private Tenant getSingleTenant(Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(TENANT_BASE_DN,
            SearchScope.SUB, searchFilter);
        Tenant tenant = getTenant(entry);
        return tenant;
    }

    private Tenant getTenant(SearchResultEntry entry)
        throws LDAPPersistException {
        if (entry == null) {
            return null;
        }
        Tenant tenant = null;
        tenant = LDAPPersister.getInstance(Tenant.class).decode(entry);
        return tenant;
    }
}
