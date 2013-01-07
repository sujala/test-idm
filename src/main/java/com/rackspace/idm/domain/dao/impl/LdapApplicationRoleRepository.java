package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Autowired
    private Configuration config;

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
        return getObject(searchFilter_applicationAndRoleName(applicationId, roleName), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public List<ClientRole> getClientRolesForApplication(Application application) {
        return getObjects(searchFilter_byApplicationId(application.getClientId()));
    }

    @Override
    public List<ClientRole> getAllClientRoles() {
        return getObjects(searchFilter_getAllClientRoles());
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable) {
        return getObjectsPaged(searchFilter_availableClientRoles(maxWeightAvailable), offset, limit);
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable) {
        return getObjectsPaged(searchFilter_availableRolesByApplicationId(applicationId, maxWeightAvailable), offset, limit);
    }

    @Override
    public PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int offset, int limit) {
        return getObjectsPaged(searchFilterApplicationIdAndRoleName(applicationId, roleName), offset, limit);
    }

    @Override
    public List<ClientRole> getIdentityRoles(Application application, List<String> roleNames) {
        return getObjects(orFilter(roleNames), application.getUniqueId());
    }

    private ClientRole getRoleById(String roleId) {
        return getObject(searchFilter_byRoleId(roleId), getBaseDn(), SearchScope.SUB);
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

    private Filter searchFilter_availableClientRoles(int maxWeightAvaibale) {
        List<Filter> orFilterList = getRoleWeightsOrFilter(maxWeightAvaibale);
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addOrAttributes(orFilterList).build();
    }

    private List<Filter> getRoleWeightsOrFilter(int maxWeightAvailable) {
        List<Filter> orFilterList = new ArrayList<Filter>();
        for (Integer weight : getRoleWeights()) {
            if (!(weight < maxWeightAvailable)) {
                orFilterList.add(Filter.createEqualityFilter("rsWeight", weight.toString()));
            }
        }
        return orFilterList;
    }

    private List<Integer> getRoleWeights() {
        List<Integer> weights = new ArrayList<Integer>();
        weights.add(config.getInt("cloudAuth.defaultUser.rsWeight"));
        weights.add(config.getInt("cloudAuth.userAdmin.rsWeight"));
        weights.add(config.getInt("cloudAuth.special.rsWeight"));
        weights.add(config.getInt("cloudAuth.admin.rsWeight"));
        weights.add(config.getInt("cloudAuth.serviceAdmin.rsWeight"));
        return weights;
    }

    private Filter searchFilter_availableRolesByApplicationId(String applicationId, int maxWeightAvailable) {
        List<Filter> orFilterList = getRoleWeightsOrFilter(maxWeightAvailable);
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addOrAttributes(orFilterList).build();
    }

    private Filter searchFilter_byApplicationId(String applicationId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId).build();
    }
    private Filter searchFilter_byRoleId(String roleId) {
       return new LdapSearchBuilder()
               .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
               .addEqualAttribute(ATTR_ID, roleId).build();
    }

    private Filter searchFilter_applicationAndRoleName(String applicationId, String roleName) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addEqualAttribute(ATTR_NAME, roleName).build();
    }

    private Filter searchFilter_getAllClientRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE).build();
    }

    private Filter orFilter(List<String> names) {
        List<Filter> orComponents = new ArrayList<Filter>();
        for (String name : names) {
            orComponents.add(Filter.createEqualityFilter(ATTR_NAME, name));
        }
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addOrAttributes(orComponents).build();
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
