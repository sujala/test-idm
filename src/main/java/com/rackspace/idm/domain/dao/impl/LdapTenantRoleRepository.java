package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.sun.jersey.api.ConflictException;
import com.unboundid.ldap.sdk.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapTenantRoleRepository extends LdapGenericRepository<TenantRole> implements TenantRoleDao {

    @Autowired
    DefaultPaginator<String> stringPaginator;

    public String getBaseDn() {
        return BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_TENANT_ROLE;
    }

    @Override
    public void addTenantRoleToApplication(Application application, TenantRole tenantRole) {
        addOrUpdateTenantRole(application.getUniqueId(), tenantRole);
    }

    @Override
    public void addTenantRoleToUser(User user, TenantRole tenantRole) {
        addOrUpdateTenantRole(user.getUniqueId(), tenantRole);
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application) {
        return getObjects(searchFilterGetTenantRoles(), application.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application, String applicationId) {
        return getObjects(searchFilterGetTenantRolesByApplication(applicationId), application.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(Application application, String applicationId, String tenantId) {
        return getObjects(searchFilterGetTenantRolesByApplicationAndTenantId(applicationId, tenantId), application.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user) {
        return getObjects(searchFilterGetTenantRoles(), user.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId) {
        return getObjects(searchFilterGetTenantRolesByApplication(applicationId), user.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user, String applicationId, String tenantId) {
        return getObjects(searchFilterGetTenantRolesByApplicationAndTenantId(applicationId, tenantId), user.getUniqueId());
    }

    @Override
    public List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess) {
        return getObjects(searchFilterGetTenantRoles(), scopeAccess.getUniqueId());
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        return getObjects(searchFilterGetTenantRolesByTenantId(tenantId));
    }

    @Override
    public List<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId) {
        return getObjects(searchFilterGetTenantRolesByTenantIdAndRoleId(tenantId, roleId));
    }

    @Override
    public List<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        return getObjects(searchFilterGetTenantRolesByRoleId(role.getId()));
    }

    @Override
    public void updateTenantRole(TenantRole tenantRole) {
        updateObject(tenantRole);
    }

    @Override
    public void deleteTenantRoleForUser(User user, TenantRole tenantRole) {
        deleteOrUpdateTenantRole(tenantRole, user.getUniqueId());
    }

    @Override
    public void deleteTenantRoleForApplication(Application application, TenantRole tenantRole) {
        deleteOrUpdateTenantRole(tenantRole, application.getUniqueId());
    }

    @Override
    public void deleteTenantRole(TenantRole tenantRole) {
        deleteObject(tenantRole);
    }

    @Override
    public PaginatorContext<String> getIdsForUsersWithTenantRole(String roleId, int offset, int limit) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder();
        searchBuilder.addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE);
        searchBuilder.addEqualAttribute(ATTR_ROLE_RS_ID, roleId);
        Filter searchFilter = searchBuilder.build();

        SearchRequest searchRequest = new SearchRequest(USERS_BASE_DN, SearchScope.SUB, searchFilter, "*");
        PaginatorContext<String> context = stringPaginator.createSearchRequest(ATTR_ID, searchRequest, offset, limit);

        SearchResult searchResult = this.getMultipleEntries(searchRequest);

        stringPaginator.createPage(searchResult, context);
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

    private void addOrUpdateTenantRole(String uniqueId, TenantRole tenantRole) {
        TenantRole currentTenantRole = getObject(searchFilterGetTenantRolesByRoleId(tenantRole.getRoleRsId()), uniqueId, SearchScope.SUB);
        if (currentTenantRole != null) {
            for (String tenantId : tenantRole.getTenantIds()) {
                if (currentTenantRole.getTenantIds().contains(tenantId)) {
                    throw new ClientConflictException();
                }
            }
            currentTenantRole.getTenantIds().addAll(tenantRole.getTenantIds());
            updateObject(currentTenantRole);
        } else {
            addObject(getTenantRoleDn(uniqueId), tenantRole);
        }
    }

    private void deleteOrUpdateTenantRole(TenantRole tenantRole, String uniqueId) {
        TenantRole currentTenantRole = getObject(searchFilterGetTenantRolesByRoleId(tenantRole.getRoleRsId()), uniqueId, SearchScope.SUB);
        if (currentTenantRole != null) {
            currentTenantRole.getTenantIds().removeAll(tenantRole.getTenantIds());
            if (currentTenantRole.getTenantIds().size() == 0) {
                deleteObject(currentTenantRole);
            } else {
                updateObject(currentTenantRole);
            }
        }
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
        TenantRole exists = getTenantRole(searchDn.toString(), role.getId());

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
        TenantRole exists = getTenantRole(searchDn.toString(), role.getId());

        boolean hasRole = exists != null;
        getLogger().debug("Does User Have Tenant Role: {}", hasRole);
        return hasRole;
    }

    @Override
    public TenantRole getTenantRoleForApplication(Application application, String roleId) {
        return getObject(searchFilterGetTenantRoleByRoleId(roleId), application.getUniqueId(), SearchScope.SUB);
    }

    @Override
    public TenantRole getTenantRoleForUser(User user, String roleId) {
        return getTenantRole(user.getUniqueId(), roleId);
    }

    @Override
    public TenantRole getTenantRoleForUser(User user, List<ClientRole> clientRoles) {
        return getTenantRole(user.getUniqueId(), orFilter(clientRoles));
    }

    @Override
    public TenantRole getTenantRoleForScopeAccess(ScopeAccess scopeAccess, String roleId) {
        return getTenantRole(scopeAccess.getUniqueId(), roleId);
    }

    private TenantRole getTenantRole(String dn, Filter filter) {
        return getObject(filter, dn, SearchScope.SUB);
    }

    private TenantRole getTenantRole(String dn, String roleId) {
        return getObject(searchFilterGetTenantRoleByRoleId(roleId), dn, SearchScope.SUB);
    }

    private String getTenantRoleDn(String dn) {
        return addLdapContainer(dn, LdapRepository.CONTAINER_ROLES);
    }

    private Filter searchFilterGetTenantRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE).build();
    }

    private Filter searchFilterGetTenantRolesByApplication(String applicationId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }

    private Filter searchFilterGetTenantRolesByApplicationAndTenantId(String applicationId, String tenantId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }

    private Filter searchFilterGetTenantRoleByRoleId(String roleId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_ROLE_RS_ID, roleId).build();
    }

    private Filter searchFilterGetTenantRolesByTenantId(String tenantId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId).build();
    }

    private Filter searchFilterGetTenantRolesByTenantIdAndRoleId(String tenantId, String roleId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_TENANT_RS_ID, tenantId)
                .addEqualAttribute(ATTR_ROLE_RS_ID, roleId).build();
    }

    private Filter searchFilterGetTenantRolesByRoleId(String roleId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                .addEqualAttribute(ATTR_ROLE_RS_ID, roleId).build();
    }

    private Filter orFilter(List<ClientRole> clientRoles) {
        List<Filter> orComponents = new ArrayList<Filter>();
        for (ClientRole role : clientRoles) {
            orComponents.add(Filter.createEqualityFilter(ATTR_ROLE_RS_ID, role.getId()));
        }

         return new LdapSearchBuilder()
                 .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TENANT_ROLE)
                 .addOrAttributes(orComponents).build();
    }
}
