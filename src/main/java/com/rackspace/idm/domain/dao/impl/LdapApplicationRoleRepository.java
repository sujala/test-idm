package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.User;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/12/12
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class LdapApplicationRoleRepository extends LdapGenericRepository<ClientRole> implements ApplicationRoleDao {

    @Override
    public String getNextRoleId() {
        return getNextId(NEXT_ROLE_ID);
    }

    @Override
    public void addClientRole(Application application, ClientRole role) {
        String dn = addLdapContainer(application.getUniqueId(), CONTAINER_ROLES);
        addObject(dn, role);
    }

    @Override
    public void updateClientRole(ClientRole role) {
        updateObject(role);
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        deleteObject(role);
    }

    @Override
    public ClientRole getClientRole(ClientRole role) {
        return getRoleById(role.getId());
    }

    @Override
    public ClientRole getClientRole(String roleId) {
        return getRoleById(roleId);
    }

    @Override
    public ClientRole getClientRoleByApplicationAndName(Application application, ClientRole role) {
        return getClientRoleByApplicationAndName(application.getClientId(), role.getName());
    }

    @Override
    public ClientRole getClientRoleByApplicationAndName(String applicationId, String roleName) {
        return getObject(searchFilterApplicationAndRoleName(applicationId, roleName), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public List<ClientRole> getClientRolesForApplication(Application application) {
        return getObjects(searchFilterByApplicationId(application.getClientId()));
    }

    @Override
    public List<ClientRole> getAllClientRoles() {
        return getObjects(searchFilterGetAllClientRoles());
    }

    @Override
    public PaginatorContext<ClientRole> getClientRolesPaged(int offset, int limit) {
        return getObjectsPaged(searchFilterGetAllClientRoles(), offset, limit);
    }

    @Override
    public PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, int offset, int limit) {
        return getObjectsPaged(searchFilterByApplicationId(applicationId), offset, limit);
    }

    @Override
    public PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int offset, int limit) {
        return getObjectsPaged(searchFilterApplicationIdAndRoleName(applicationId, roleName), offset, limit);
    }

    private ClientRole getRoleById(String roleId) {
        return getObject(searchFilterByRoleId(roleId), getBaseDn(), SearchScope.SUB);
    }

    private Filter searchFilterApplicationIdAndRoleName(String applicationId, String roleName) {
        LdapSearchBuilder builder = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE);
        if (applicationId != null) {
            builder.addEqualAttribute(ATTR_CLIENT_ID, applicationId);
        }
        if (roleName != null) {
            builder.addEqualAttribute(ATTR_NAME, roleName);
        }
        return builder.build();

    }
    private Filter searchFilterByApplicationId(String applicationId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }
    private Filter searchFilterByRoleId(String roleId) {
       return new LdapSearchBuilder()
               .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
               .addEqualAttribute(ATTR_ID, roleId).build();
    }

    private Filter searchFilterApplicationAndRoleName(String applicationId, String roleName) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addEqualAttribute(ATTR_NAME, roleName).build();
    }

    private Filter searchFilterGetAllClientRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE).build();
    }

    public String getBaseDn(){
        return APPLICATIONS_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_CLIENT_ROLE;
    }

    public String[] getSearchAttributes(){
        return ATTR_CLIENT_ROLE_SEARCH_ATTRIBUTES;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }
}
