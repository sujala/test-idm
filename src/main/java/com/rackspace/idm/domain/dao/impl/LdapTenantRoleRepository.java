package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LDAPComponent
public class LdapTenantRoleRepository extends LdapGenericRepository<TenantRole> implements TenantRoleDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
    public void addTenantRoleToUser(BaseUser user, TenantRole tenantRole) {
        addOrUpdateTenantRole(user.getUniqueId(), tenantRole);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user) {
        return getObjects(searchFilterGetTenantRoles(), user.getUniqueId());
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user, String applicationId) {
        return getObjects(searchFilterGetTenantRolesByApplication(applicationId), user.getUniqueId());
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        return getObjects(searchFilterGetTenantRolesByTenantId(tenantId));
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId) {
        return getObjects(searchFilterGetTenantRolesByTenantIdAndRoleId(tenantId, roleId));
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        return getObjects(searchFilterGetTenantRolesByRoleId(role.getId()));
    }

    @Override
    public void updateTenantRole(TenantRole tenantRole, String tenantId) {
        updateObject(tenantRole);
    }

    @Override
    public void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole) {
        deleteOrUpdateTenantRole(tenantRole, user.getUniqueId());
    }

    @Override
    public void deleteTenantRole(TenantRole tenantRole) {
        deleteTenantRole(tenantRole, null);
    }

    @Override
    public void deleteTenantRole(TenantRole tenantRole, String tenantId) {
        deleteObject(tenantRole);
    }

    @Override
    public List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult) {
        List<String> userIds = new ArrayList<String>();

        List<TenantRole> roles = null;
        try {
            roles = getUnpagedUnsortedObjects(searchFilterGetTenantRolesByRoleId(roleId), getBaseDn(), SearchScope.SUB, maxResult);
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug("Aborting loading users with role. Result size of {} will exceed limit of {}", userIds.size(), maxResult);
                throw new BadRequestException("Result size exceeded. Results limited to " + maxResult + " users.");
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }
        //get the userIds
        for (TenantRole tenantRole : roles) {
            try {
                userIds.add(getUserIdFromUniqueId(tenantRole.getUniqueId()));
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

    protected String getUserIdFromUniqueId(String uniqueId) throws LDAPException {
        DN dn = new DN(uniqueId);
        DN userDN = getBaseDnForSearch(dn);
        if (userDN != null) {
            List<RDN> userRDNs = new ArrayList<RDN>(Arrays.asList(userDN.getRDNs()));
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
    public TenantRole getTenantRoleForUser(BaseUser user, String roleId) {
        return getTenantRole(user.getUniqueId(), roleId);
    }

    @Override
    public void updateTenantRole(TenantRole tenantRole) {
        updateTenantRole(tenantRole, null);
    }

    @Override
    public Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles) {
        return getObjects(orFilter(clientRoles), user.getUniqueId());
    }

    @Override
    public String getUserIdForParent(TenantRole tenantRole) {
        try{
            DN dn = new DN(tenantRole.getUniqueId());
            RDN rdn = dn.getParent().getParent().getRDN();
            if(rdn.hasAttribute("rsId")){
                String id = rdn.getAttributeValues()[0];
                if(!StringUtils.isBlank(id)){
                    return id;
                }
            }
        }catch (Exception ignored){
        }
        return null;
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
