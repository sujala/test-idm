package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.ClientRoleDao;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.unboundid.ldap.sdk.Filter;
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
public class LdapClientRoleRepository extends LdapGenericRepository<ClientRole> implements ClientRoleDao {
    public String getBaseDn(){
        return APPLICATIONS_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_CLIENT_ROLE;
    }

    public String[] getSearchAttributes(){
        return ATTR_CLIENT_ROLE_SEARCH_ATTRIBUTES;
    }

    @Override
    public void addClientRole(String clientUniqueId, ClientRole role) {
        String dn = addLdapContainer(clientUniqueId, CONTAINER_ROLES);
        addObject(dn, role);
    }

    @Override
    public void updateClientRole(ClientRole role) {
        updateObject(role);
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        deleteObject(searchFilterGetClientRoleById(role.getId()));
    }

    @Override
    public ClientRole getClientRole(List<FilterParam> filters) {
        return getObject(searchFilterGetClientRoles(filters));
    }

    @Override
    public List<ClientRole> getClientRoles() {
        return getObjects(searchFilterGetClientRoles());
    }

    @Override
    public List<ClientRole> getClientRoles(List<FilterParam> filters) {
        return getObjects(searchFilterGetClientRoles(filters));
    }

    private Filter searchFilterGetClientRoleById(String roleId) {
       return new LdapSearchBuilder()
               .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE)
               .addEqualAttribute(ATTR_ID, roleId).build();
    }

    private Filter searchFilterGetClientRoles(List<FilterParam> filterParams) {
        LdapSearchBuilder searchBuilder = new LdapSearchBuilder()
                                                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE);
        for (FilterParam filterParam : filterParams) {
            if (filterParam.getParam() == FilterParamName.APPLICATION_ID) {
                searchBuilder.addEqualAttribute(ATTR_CLIENT_ID, filterParam.getStrValue());
            } else if (filterParam.getParam() == FilterParamName.ROLE_ID) {
                searchBuilder.addEqualAttribute(ATTR_ID, filterParam.getStrValue());
            } else if (filterParam.getParam() == FilterParamName.ROLE_NAME) {
                searchBuilder.addEqualAttribute(ATTR_NAME, filterParam.getStrValue());
            }
        }
        return searchBuilder.build();
    }

    private Filter searchFilterGetClientRoles() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CLIENT_ROLE).build();
    }
}
