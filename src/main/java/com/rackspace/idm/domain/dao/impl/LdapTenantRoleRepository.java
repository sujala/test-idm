package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.ClientConflictException;
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

    public String getSortAttribute() {
        return ATTR_ROLE_RS_ID;
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
    public List<String> getIdsForUsersWithTenantRole(String roleId) {
        List<String> userIds = new ArrayList<String>();
        List<TenantRole> tenantRoles = getObjects(searchFilterGetTenantRolesByRoleId(roleId));

        for (TenantRole tenantRole : tenantRoles) {
            try {
                userIds.add(getUserIdFromDN(tenantRole.getLDAPEntry().getParsedDN()));
            } catch (LDAPException e) {
                throw new IllegalStateException();
            }
        }

        return userIds;
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
