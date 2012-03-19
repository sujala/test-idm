package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
            throw new IllegalArgumentException(
                "Null or Empty tenantId parameter.");
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
            throw new IllegalArgumentException(
                "Null or Empty tenantId parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, tenantId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        Tenant tenant = null;

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
    public Tenant getTenantByName(String name) {
        getLogger().debug("Doing search for name " + name);
        if (StringUtils.isBlank(name)) {
            getLogger().error("Null or Empty name parameter");
            throw new IllegalArgumentException("Null or Empty name parameter.");
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, name)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        Tenant tenant = null;

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
            if (modifications.size() > 0) {
                persister.modify(tenant, conn, null, true);
            }
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
            TENANT_BASE_DN, SearchScope.ONE, searchFilter, ATTR_ID,
            ATTR_TENANT_SEARCH_ATTRIBUTES);

        List<Tenant> tenants = new ArrayList<Tenant>();
        for (SearchResultEntry entry : entries) {
            tenants.add(getTenant(entry));
        }
        return tenants;
    }

    private Tenant getSingleTenant(Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(TENANT_BASE_DN,
            SearchScope.ONE, searchFilter, ATTR_TENANT_SEARCH_ATTRIBUTES);
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

    @Override
    public void addTenantRoleToParent(String parentUniqueId, TenantRole role) {
        if (StringUtils.isBlank(parentUniqueId)) {
            String errmsg = "ParentUniqueId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        if (role == null) {
            String errmsg = "Null instance of TenantRole was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        getLogger().info("Adding TenantRole: {}", role);
        Audit audit = Audit.log(role).add();
        LDAPConnection conn = null;
        try {
            conn = getAppConnPool().getConnection();

            SearchResultEntry entry = getContainer(conn, parentUniqueId, CONTAINER_ROLES);
            if (entry == null) {
                addContainer(conn, parentUniqueId, CONTAINER_ROLES);
                entry = getContainer(conn, parentUniqueId, CONTAINER_ROLES);
            }

            final LDAPPersister<TenantRole> persister = LDAPPersister.getInstance(TenantRole.class);
            persister.add(role, conn, entry.getDN());
            audit.succeed();
            getLogger().info("Added TenantRole: {}", role);
        } catch (final LDAPException e) {
            getLogger().error("Error adding tenant role object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public void deleteTenantRole(TenantRole role) {
        if (role == null || StringUtils.isBlank(role.getUniqueId())) {
            String errMsg = "Null tenant role was passed";
            getLogger().warn(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        getLogger().debug("Deleting TenantRole: {}", role);
        final String dn = role.getUniqueId();
        final Audit audit = Audit.log(role).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted TenantRole: {}", role);
    }

    @Override
    public TenantRole getTenantRoleForParentById(String parentUniqueId, String id) {
        if (StringUtils.isBlank(parentUniqueId)) {
            String errmsg = "ParentUniqueId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            throw new IllegalArgumentException("Null or Empty id parameter.");
        }
        getLogger().debug("Doing search for TenantRole " + id);

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ROLE_RS_ID, id)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .build();

        TenantRole role = null;

        try {
            role = getSingleTenantRole(parentUniqueId, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting role object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Tenant Role - {}", role);

        return role;
    }

    @Override
    public List<TenantRole> getTenantRolesByParent(String parentUniqueId) {
        if (StringUtils.isBlank(parentUniqueId)) {
            String errmsg = "ParentUniqueId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        getLogger().debug("Getting tenantRoles");
        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(
            ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(parentUniqueId, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user) {
        getLogger().debug("Getting tenantRoles");
        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE).build();

        //String dn = new LdapDnBuilder(user.getUniqueId()).addAttribute(ATTR_NAME, CONTAINER_ROLES).build();
        String dn = new LdapDnBuilder(user.getUniqueId()).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(dn, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user,
        FilterParam[] filters) {
        return getTenantRolesForClient(user.getUniqueId(), filters);
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(
        Application application, FilterParam[] filters) {
        return getTenantRolesForClient(application.getUniqueId(), filters);
    }

    @Override
    public List<TenantRole> getTenantRolesByParentAndClientId(
        String parentUniqueId, String clientId) {
        if (StringUtils.isBlank(parentUniqueId)) {
            String errmsg = "ParentUniqueId cannot be blank";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }

        if (StringUtils.isBlank(clientId)) {
            getLogger().error("Null or Empty clientId parameter");
            throw new IllegalArgumentException(
                "Null or Empty clientId parameter.");
        }

        getLogger().debug("Getting tenantRoles by clientId");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_CLIENT_ID, clientId).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(parentUniqueId, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    private List<TenantRole> getTenantRolesForClient(
        String uniqueParentClientId, FilterParam[] filters) {
        getLogger().debug("Getting tenantRoles");

        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS,
            OBJECTCLASS_TENANT_ROLE);

        if (filters != null) {
            for (FilterParam filter : filters) {
                // can only filter on tenantId and applicationId for now
                if (filter.getParam() == FilterParamName.APPLICATION_ID) {
                    searchBuilder.addEqualAttribute(ATTR_CLIENT_ID,
                        filter.getStrValue());
                } else if (filter.getParam() == FilterParamName.TENANT_ID) {
                    searchBuilder.addEqualAttribute(ATTR_TENANT_RS_ID,
                        filter.getStrValue());
                }
            }
        }

        String dn = new LdapDnBuilder(uniqueParentClientId).build();

        Filter searchFilter = searchBuilder.build();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(dn, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    private List<TenantRole> getMultipleTenantRoles(String parentUniqueId,
        Filter searchFilter) throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(
            parentUniqueId, SearchScope.SUB, searchFilter, ATTR_ID);

        List<TenantRole> roles = new ArrayList<TenantRole>();
        for (SearchResultEntry entry : entries) {
            roles.add(getTenantRole(entry));
        }
        return roles;
    }

    private TenantRole getSingleTenantRole(String parentUniqueId,
        Filter searchFilter) throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(parentUniqueId,
            SearchScope.SUB, searchFilter);
        TenantRole role = getTenantRole(entry);
        return role;
    }

    private TenantRole getTenantRole(SearchResultEntry entry)
        throws LDAPPersistException {
        if (entry == null) {
            return null;
        }
        TenantRole role = LDAPPersister.getInstance(TenantRole.class).decode(
            entry);
        return role;
    }

    @Override
    public void updateTenantRole(TenantRole role) {
        if (role == null || StringUtils.isBlank(role.getUniqueId())) {
            String errmsg = "Null instance of Tenant was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating Tenant Role: {}", role);
        LDAPConnection conn = null;
        Audit audit = Audit.log(role);
        try {
            conn = getAppConnPool().getConnection();
            final LDAPPersister<TenantRole> persister = LDAPPersister
                .getInstance(TenantRole.class);
            List<Modification> modifications = persister.getModifications(role,
                true);
            audit.modify(modifications);
            persister.modify(role, conn, null, true);
            getLogger().debug("Updated Tenant Role: {}", role);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating Tenant Role", e);
            audit.fail();
            throw new IllegalStateException(e);
        } finally {
            getAppConnPool().releaseConnection(conn);
        }
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error("Null or Empty tenantId parameter");
            throw new IllegalArgumentException(
                "Null or Empty tenantId parameter.");
        }

        getLogger().debug("Getting tenantRoles by tenantId");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    @Override
    public List<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        if (role == null) {
            getLogger().error("Null or Empty role parameter");
            throw new IllegalArgumentException("Null or Empty role parameter.");
        }

        getLogger().debug("Getting tenantRoles by client role");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_ROLE_RS_ID, role.getId()).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId,
        String roleId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error("Null or Empty tenantId parameter");
            throw new IllegalArgumentException(
                "Null or Empty tenantId parameter.");
        }

        if (StringUtils.isBlank(roleId)) {
            getLogger().error("Null or Empty roleId parameter");
            throw new IllegalArgumentException(
                "Null or Empty roleId parameter.");
        }

        getLogger().debug("Getting tenantRoles by tenantId");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId)
            .addEqualAttribute(ATTR_ROLE_RS_ID, roleId).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error("Error getting tenant object", e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Got {} Tenant Roles", roles.size());

        return roles;
    }

    @Override
    public boolean doesScopeAccessHaveTenantRole(ScopeAccess scopeAccess, ClientRole role) {
        getLogger().debug("Does Scope Access Have Tenant Role");

        String parentDn = null;
        try {
            if (scopeAccess instanceof DelegatedClientScopeAccess) {
                parentDn = scopeAccess.getUniqueId();
            } else {
                parentDn = scopeAccess.getLDAPEntry().getParentDN().getParentString();
            }
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        TenantRole exists = this.getTenantRoleForParentById(parentDn, role.getId());

        boolean hasRole = exists != null;
        getLogger().debug("Does Scope Access Have Tenant Role: {}", hasRole);
        return hasRole;
    }
}
