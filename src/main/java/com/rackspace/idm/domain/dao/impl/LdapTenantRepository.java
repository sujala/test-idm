package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.*;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapTenantRepository extends LdapRepository implements TenantDao {

    @Autowired
    DefaultPaginator<String> userIdPaginator;

    public static final String NULL_OR_EMPTY_TENANT_ID_PARAMETER = "Null or Empty tenantId parameter";
    public static final String ERROR_GETTING_TENANT_OBJECT = "Error getting tenant object";
    public static final String PARENT_UNIQUE_ID_CANNOT_BE_BLANK = "ParentUniqueId cannot be blank";
    public static final String GOT_TENANT_ROLES = "Got {} Tenant Roles";

    @Override
    public void addTenant(Tenant tenant) {
        if (tenant == null) {
            String errmsg = "Null instance of Tenant was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().info("Adding Tenant: {}", tenant);
        Audit audit = Audit.log(tenant).add();
        try {
            final LDAPPersister<Tenant> persister = LDAPPersister.getInstance(Tenant.class);
            persister.add(tenant, getAppInterface(), TENANT_BASE_DN);
            audit.succeed();
            getLogger().info("Added Tenant: {}", tenant);
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                String errMsg = String.format("Tenant %s already exists", tenant.getTenantId());
                getLogger().warn(errMsg);
                throw new DuplicateException(errMsg, e);
            }
            getLogger().error("Error adding tenant object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void deleteTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error(NULL_OR_EMPTY_TENANT_ID_PARAMETER);
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
            getLogger().error(NULL_OR_EMPTY_TENANT_ID_PARAMETER);
            getLogger().info("Invalid tenantId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, tenantId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        Tenant tenant = null;

        try {
            tenant = getSingleTenant(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
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
            getLogger().info("Invalid name parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_NAME, name)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT).build();

        Tenant tenant = null;

        try {
            tenant = getSingleTenant(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
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
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
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
        Audit audit = Audit.log(tenant);
        try {
            final LDAPPersister<Tenant> persister = LDAPPersister.getInstance(Tenant.class);
            List<Modification> modifications = persister.getModifications(tenant, true);
            audit.modify(modifications);
            if (modifications.size() > 0) {
                persister.modify(tenant, getAppInterface(), null, true);
            }
            getLogger().debug("Updated Tenant: {}", tenant);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating tenant", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    List<Tenant> getMultipleTenants(Filter searchFilter)
        throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(
                TENANT_BASE_DN, SearchScope.ONE, ATTR_ID, searchFilter,
                ATTR_TENANT_SEARCH_ATTRIBUTES);

        List<Tenant> tenants = new ArrayList<Tenant>();
        for (SearchResultEntry entry : entries) {
            tenants.add(getTenant(entry));
        }
        return tenants;
    }

    Tenant getSingleTenant(Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(TENANT_BASE_DN,
                SearchScope.ONE, searchFilter, ATTR_TENANT_SEARCH_ATTRIBUTES);
        return getTenant(entry);
    }

    Tenant getTenant(SearchResultEntry entry)
        throws LDAPPersistException {
        if (entry == null) {
            return null;
        }
        Tenant tenant = null;
        tenant = LDAPPersister.getInstance(Tenant.class).decode(entry);
        return tenant;
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

    private TenantRole getTenantRoleForParentById(String parentUniqueId, String id) {
        if (StringUtils.isBlank(parentUniqueId)) {
            String errmsg = PARENT_UNIQUE_ID_CANNOT_BE_BLANK;
            getLogger().error(errmsg);
            getLogger().info("Invalid parentUniqueId paramater.");
            return null;
        }
        if (StringUtils.isBlank(id)) {
            getLogger().error("Null or Empty id parameter");
            getLogger().info("Invalid id parameter.");
            return null;
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
    public List<TenantRole> getTenantRolesForUser(User user) {
        return getTenantRoles(user.getUniqueId(), searchFilterGetTenantRoles());
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId) {
        return getTenantRoles(user.getUniqueId(), searchFilterGetTenantRolesByApplicationId(applicationId));
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId) {
        return getTenantRoles(user.getUniqueId(), searchFilterGetTenantRolesByApplicationIdAndTenantId(applicationId, tenantId));
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application) {
        return getTenantRoles(application.getUniqueId(), searchFilterGetTenantRoles());
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application, String applicationId) {
        return getTenantRoles(application.getUniqueId(),
                searchFilterGetTenantRolesByApplicationId(applicationId));
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId) {
        return getTenantRoles(application.getUniqueId(), searchFilterGetTenantRolesByApplicationIdAndTenantId(applicationId, tenantId));
    }

    private List<TenantRole> getTenantRoles(String uniqueParentClientId, Filter searchFilter) {
        getLogger().debug("Getting tenantRoles");

        String dn = new LdapDnBuilder(uniqueParentClientId).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(dn, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug(GOT_TENANT_ROLES, roles.size());

        return roles;
    }

    List<TenantRole> getMultipleTenantRoles(String parentUniqueId,
        Filter searchFilter) throws LDAPPersistException {
        List<SearchResultEntry> entries = this.getMultipleEntries(parentUniqueId, SearchScope.SUB, searchFilter, "*"); //TODO: Dont use wildcard

        List<TenantRole> roles = new ArrayList<TenantRole>();
        for (SearchResultEntry entry : entries) {
            roles.add(getTenantRole(entry));
        }
        return roles;
    }

    TenantRole getSingleTenantRole(String parentUniqueId,
        Filter searchFilter) throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(parentUniqueId, SearchScope.SUB, searchFilter);
        return getTenantRole(entry);
    }

    TenantRole getTenantRole(SearchResultEntry entry)
        throws LDAPPersistException {
        if (entry == null) {
            return null;
        }
        return LDAPPersister.getInstance(TenantRole.class).decode(entry);
    }

    @Override
    public void updateTenantRole(TenantRole role) {
        if (role == null || StringUtils.isBlank(role.getUniqueId())) {
            String errmsg = "Null instance of Tenant was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating Tenant Role: {}", role);
        Audit audit = Audit.log(role);
        try {
            final LDAPPersister<TenantRole> persister = LDAPPersister.getInstance(TenantRole.class);
            List<Modification> modifications = persister.getModifications(role, true);
            audit.modify(modifications);
            persister.modify(role, getAppInterface(), null, true);
            getLogger().debug("Updated Tenant Role: {}", role);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating Tenant Role", e);
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error(NULL_OR_EMPTY_TENANT_ID_PARAMETER);
            getLogger().info("Invalid tenantId parameter.");
            return new ArrayList<TenantRole>();
        }

        getLogger().debug("Getting tenantRoles by tenantId");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug(GOT_TENANT_ROLES, roles.size());

        return roles;
    }

    @Override
    public List<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        if (role == null) {
            getLogger().error("Null or Empty role parameter");
            getLogger().info("Invalid role parameter.");
            return new ArrayList<TenantRole>();
        }

        getLogger().debug("Getting tenantRoles by client role");
        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
            .addEqualAttribute(ATTR_ROLE_RS_ID, role.getId()).build();

        List<TenantRole> roles = new ArrayList<TenantRole>();
        try {
            roles = getMultipleTenantRoles(BASE_DN, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug(GOT_TENANT_ROLES, roles.size());

        return roles;
    }

    @Override
    public PaginatorContext<String> getMultipleTenantRoles(String roleId, int offset, int limit) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE);
        searchBuilder.addEqualAttribute(ATTR_ROLE_RS_ID, roleId);
        Filter searchFilter = searchBuilder.build();

        SearchRequest searchRequest = new SearchRequest(USERS_BASE_DN, SearchScope.SUB, searchFilter, "*");
        PaginatorContext<String> context = userIdPaginator.createSearchRequest(ATTR_ID, searchRequest, offset, limit);

        SearchResult searchResult = this.getMultipleEntries(searchRequest);

        if (searchResult == null) {
            return context;
        }

        userIdPaginator.createPage(searchResult, context);
        List<String> userIds = new ArrayList<String>();
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            try {
                userIds.add(getUserIdFromDN(entry.getParsedDN()));
            } catch (LDAPException e) {
                throw new IllegalStateException(e);
            } catch (Exception e) {
                // noop
            }
        }

        context.setValueList(userIds);

        return context;
    }

    protected String getUserIdFromDN(DN dn) {
        DN userDN = getBaseDnForSearch(dn);
        if (userDN != null) {
            List<RDN> userRDNs= new ArrayList<RDN>(Arrays.asList(userDN.getRDNs()));
            for (RDN rdn : userRDNs) {
                if (rdn.hasAttribute("rsId")) {
                    String rdnString = rdn.toString();
                    return rdnString.substring(rdnString.indexOf('=') + 1);
                }
            }
        }
        return "";
    }

    protected DN getBaseDnForSearch(DN dn) {
        DN parentDN = dn.getParent();
        List<RDN> rdns = new ArrayList<RDN>(Arrays.asList(dn.getRDNs()));
        List<RDN> parentRDNs = new ArrayList<RDN>(Arrays.asList(parentDN.getRDNs()));
        List<RDN> remainder = new ArrayList<RDN>(rdns);
        remainder.removeAll(parentRDNs);
        RDN rdn = remainder.get(0);
        if (rdn.hasAttribute("rsId") || rdn.hasAttribute("rackerId") || rdn.hasAttribute("clientId")) {
            return dn;
        } else if (parentDN.getParent() == null) {
            return null;
        } else {
            return getBaseDnForSearch(parentDN);
        }
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId,
        String roleId) {
        if (StringUtils.isBlank(tenantId)) {
            getLogger().error(NULL_OR_EMPTY_TENANT_ID_PARAMETER);
            getLogger().info("Invalid tenantId parameter.");
            return new ArrayList<TenantRole>();
        }

        if (StringUtils.isBlank(roleId)) {
            getLogger().error("Null or Empty roleId parameter");
            getLogger().info("Invalid roleId parameter.");
            return new ArrayList<TenantRole>();
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
            getLogger().error(ERROR_GETTING_TENANT_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug(GOT_TENANT_ROLES, roles.size());

        return roles;
    }

    @Override // Todo: Remove this function and just use the below User call
    public boolean doesScopeAccessHaveTenantRole(ScopeAccess scopeAccess, ClientRole role) {
        getLogger().debug("Does Scope Access Have Tenant Role");

        DN searchDn = null;
        try {
            if (scopeAccess instanceof DelegatedClientScopeAccess) {
                searchDn = getBaseDnForSearch(new DN(scopeAccess.getUniqueId()));
            } else {
                searchDn = getBaseDnForSearch(scopeAccess.getLDAPEntry().getParentDN());
            }
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        if (searchDn == null) {
            throw new BadRequestException("token was not tied to a user");
        }
        TenantRole exists = this.getTenantRoleForParentById(searchDn.toString(), role.getId());

        boolean hasRole = exists != null;
        getLogger().debug("Does Scope Access Have Tenant Role: {}", hasRole);
        return hasRole;
    }

    @Override
    public boolean doesUserHaveTenantRole(String uniqueId, ClientRole role) {
        getLogger().debug("Does User Have Tenant Role");

        DN searchDn;
        try {
            searchDn = new DN(uniqueId);
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        if (searchDn == null) {
            throw new BadRequestException("User is invalid");
        }
        TenantRole exists = this.getTenantRoleForParentById(searchDn.toString(), role.getId());

        boolean hasRole = exists != null;
        getLogger().debug("Does User Have Tenant Role: {}", hasRole);
        return hasRole;
    }

    private Filter searchFilterGetTenantRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS , LdapRepository.OBJECTCLASS_TENANT_ROLE).build();
    }

    private Filter searchFilterGetTenantRolesByApplicationId(String applicationId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }

    private Filter searchFilterGetTenantRolesByApplicationIdAndTenantId(String applicationId, String tenantId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId).build();
    }
}
