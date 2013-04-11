package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LdapTenantRoleRepository extends LdapGenericRepository<TenantRole> implements TenantRoleDao {

    @Override
    public void addTenantRoleToApplication(Application application, TenantRole tenantRole) {
        addTenantRole(application.getUniqueId(), tenantRole);
    }

    @Override
    public void addTenantRoleToUser(User user, TenantRole tenantRole) {
        addTenantRole(user.getUniqueId(), tenantRole);
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
        String parentDn = getSearchDnForScopeAccess(scopeAccess);
        return getObjects(searchFilterGetTenantRoles(), parentDn);
    }

    @Override
    public void updateTenantRole(TenantRole tenantRole) {
        updateObject(tenantRole);
    }

    @Override
    public void deleteTenantRoleForUser(User user, TenantRole tenantRole) {
        deleteTenantRole(user.getUniqueId(), tenantRole);
    }

    @Override
    public void deleteTenantRoleForApplication(Application application, TenantRole tenantRole) {
        deleteTenantRole(application.getUniqueId(), tenantRole);
    }

    @Override
    public void deleteTenantRole(TenantRole tenantRole) {
        deleteObject(tenantRole);
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
        String parentDn = getSearchDnForScopeAccess(scopeAccess);
        return getTenantRole(parentDn, roleId);
    }

    private String getSearchDnForScopeAccess(ScopeAccess scopeAccess) {
        String userDn = null;
        try {
            if (scopeAccess instanceof DelegatedClientScopeAccess) {
                userDn = getBaseDnForScopeAccess(new DN(scopeAccess.getUniqueId())).toString();
            } else {
                userDn = getBaseDnForScopeAccess(scopeAccess.getLDAPEntry().getParentDN()).toString();
            }
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        if (userDn == null) {
            throw new BadRequestException("scopeAccess is not tied to a user");
        }

        return userDn;
    }

    private DN getBaseDnForScopeAccess(DN dn) {
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
            return getBaseDnForScopeAccess(parentDN);
        }
    }

    private void addTenantRole(String parentUniqueId, TenantRole role) {
        // Adding a tenantRole has multiple paths depending on whether
        // the user already has that role on not.

        if(role == null){
            throw new IllegalArgumentException("Role cannot be null");
        }

        TenantRole existingRole = getTenantRole(parentUniqueId, role.getRoleRsId());
        if (existingRole == null) {
            // if the user does not have the role then just add the
            // tenant role normally.
            String dn = getTenantRoleDn(parentUniqueId, role);
            addObject(dn, role);
        } else if (!ArrayUtils.isEmpty(existingRole.getTenantIds())) {
            // If the new role is not global then add the new tenant
            // to the role and update the role, otherwise just update
            // the role and it will delete the existing tenants and
            // make it a global role.
            if (!ArrayUtils.isEmpty(role.getTenantIds())) {
                for (String tenantId : role.getTenantIds()) {
                    for(String existingId : existingRole.getTenantIds()){
                        if(existingId.equals(tenantId)) { //If role is existing then throw error
                            throw new  ClientConflictException("Tenant Role already exists");
                        }
                    }
                    existingRole.addTenantId(tenantId);
                }
            } else {
                existingRole.setTenantIds(null);
            }
            updateTenantRole(existingRole);
        }
    }

    private void deleteTenantRole(String parentUniqueId, TenantRole role) {
        if(role == null){
            throw new IllegalArgumentException("Role cannot be null");
        }

        TenantRole existingRole = getTenantRole(parentUniqueId, role.getRoleRsId());

        if (existingRole == null) {
            throw new NotFoundException("Tenant Role not found");
        }

        if (role.getTenantIds() == null || role.getTenantIds().length == 0) {
            deleteObject(existingRole);
        } else if (existingRole.containsTenantId(role.getTenantIds()[0])) {
            if (existingRole.getTenantIds().length == 1) {
                deleteObject(existingRole);
            } else {
                existingRole.removeTenantId(role.getTenantIds()[0]);
                updateObject(existingRole);
            }
        }
    }

    private TenantRole getTenantRole(String dn, Filter filter) {
        return getObject(filter, dn, SearchScope.SUB);
    }

    private TenantRole getTenantRole(String dn, String roleId) {
        return getObject(searchFilterGetTenantRoleByRoleId(roleId), dn, SearchScope.SUB);
    }

    private String getTenantRoleDn(String dn, TenantRole tenantRole) {
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
                .addEqualAttribute(ATTR_TENANT_ID, tenantId)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }

    private Filter searchFilterGetTenantRoleByRoleId(String roleId) {
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
