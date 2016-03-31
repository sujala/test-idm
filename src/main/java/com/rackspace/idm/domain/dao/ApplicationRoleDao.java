package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/12/12
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ApplicationRoleDao {
    void addClientRole(Application application, ClientRole role);
    void updateClientRole(ClientRole role);
    void deleteClientRole(ClientRole role);

    ClientRole getClientRole(ClientRole role);
    ClientRole getClientRole(String applicationId);
    ClientRole getClientRoleByApplicationAndName(Application application, ClientRole role);
    ClientRole getClientRoleByApplicationAndName(String applicationId, String roleName);
    ClientRole getRoleByName(String roleName);

    Iterable<ClientRole> getClientRolesForApplication(Application application);

    PaginatorContext<ClientRole> getAvailableClientRolesByName(String roleName, int maxWeightAvailable, int offset, int limit);
    PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable);
    PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable);

    String getNextRoleId();

    Iterable<ClientRole> getIdentityRoles(Application application, List<String> roleNames);

    Iterable<ClientRole> getClientRoles(List<String> roleIds);

    /**
     * Get a list of all the Identity roles (prefixed with "identity:") that the identity application uses to make
     * authorization decisions.
     *
     * @return
     */
    Iterable<ClientRole> getAllIdentityRoles();
}
