package com.rackspace.idm.domain.dao.impl;

import com.google.common.collect.ImmutableList;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LDAPComponent
public class LdapApplicationRoleRepository extends LdapGenericRepository<ClientRole> implements ApplicationRoleDao {

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    public static final ImmutableList<Integer> roleQueryWeights = ImmutableList.copyOf(Arrays.asList(0, 50, 100, 500, 750, 900, 1000, 2000, 2500));

    @Override
    protected boolean useUuidForRsId() {
        return identityConfig.getReloadableConfig().getUseUuidIdsForNewRolesEnabled();
    }

    @Override
    public String getNextRoleId() {
        return getNextId(NEXT_ROLE_ID);
    }

    @Override
    public void addClientRole(Application application, ClientRole role) {
        String dn = addLdapContainer(application.getUniqueId(), CONTAINER_APPLICATION_ROLES);
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
    public ClientRole getRoleByName(String roleName) {
        return getObject(searchFilterRoleName(roleName), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public ClientRole getClientRoleByApplicationAndName(String applicationId, String roleName) {
        return getObject(searchFilter_applicationAndRoleName(applicationId, roleName), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public Iterable<ClientRole> getClientRolesForApplication(Application application) {
        return getObjects(searchFilter_byApplicationId(application.getClientId()));
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesByName(String roleName, int maxWeightAvailable, int offset, int limit) {
        return getObjectsPaged(searchFilter_roleNameAndWeight(roleName, maxWeightAvailable), offset, limit);
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
    public Iterable<ClientRole> getIdentityRoles(Application application, List<String> roleNames) {
        return getObjects(orFilter(roleNames, ATTR_NAME), application.getUniqueId());
    }

    @Override
    public Iterable<ClientRole> getClientRoles(List<String> roleIds) {
        return getObjects(orFilter(roleIds, ATTR_ID));
    }

    @Override
    public Iterable<ClientRole> getAllIdentityRoles() {
        return getObjects(searchFilterIdentityRole(), getBaseDn());
    }

    @Override
    public int countClientRolesByTenantType(String tenantType) {
        return countObjects(searchFilterTenantType(tenantType));
    }

    private ClientRole getRoleById(String roleId) {
        return getObject(searchFilter_byRoleId(roleId), getBaseDn(), SearchScope.SUB);
    }

    private Filter searchFilterIdentityRole() {
        LdapSearchBuilder builder = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, identityConfig.getStaticConfig().getCloudAuthClientId())
                .addSubStringAttribute(ATTR_NAME, GlobalConstants.IDENTITY_ROLE_PREFIX, null, null);
        return builder.build();
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

    private Filter searchFilter_availableClientRoles(int maxWeightAvailable) {
        List<Filter> orFilterList = getRoleWeightsOrFilter(maxWeightAvailable);
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addOrAttributes(orFilterList).build();
    }

    private List<Filter> getRoleWeightsOrFilter(int maxWeightAvailable) {
        List<Filter> orFilterList = new ArrayList<Filter>();
        //Todo: fix this hack
        // Role weights are stored as strings in the directory.
        // As a result we cannot filter roles with a weight greater than users' role weight
        //rsWeight role 2500 does not exist; added to prevent default user from getting all roles
        for (Integer weight : roleQueryWeights) {
            if (maxWeightAvailable < weight) {
                orFilterList.add(Filter.createEqualityFilter("rsWeight", weight.toString()));
            }
        }
        return orFilterList;
    }

    private Filter searchFilter_availableRolesByApplicationId(String applicationId, int maxWeightAvailable) {
        List<Filter> orFilterList = getRoleWeightsOrFilter(maxWeightAvailable);
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addOrAttributes(orFilterList)
                .build();
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

    private Filter searchFilter_roleNameAndWeight(String roleName, int maxWeightAvailable) {
        List<Filter> orFilterList = getRoleWeightsOrFilter(maxWeightAvailable);
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addOrAttributes(orFilterList)
                .addEqualAttribute(ATTR_NAME, roleName)
                .build();
    }

    private Filter searchFilter_applicationAndRoleName(String applicationId, String roleName) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_CLIENT_ID, applicationId)
                .addEqualAttribute(ATTR_NAME, roleName).build();
    }

    private Filter searchFilterRoleName(String roleName) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_NAME, roleName).build();
    }

    private Filter searchFilter_getAllClientRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE).build();
    }

    private Filter orFilter(List<String> list, String attribute) {
        List<Filter> orComponents = new ArrayList<Filter>();
        for (String value : list) {
            orComponents.add(Filter.createEqualityFilter(attribute, value));
        }
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addOrAttributes(orComponents).build();
    }

    private Filter searchFilterTenantType(String tenantType) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
                .addEqualAttribute(ATTR_RS_TENANT_TYPE, tenantType).build();
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
