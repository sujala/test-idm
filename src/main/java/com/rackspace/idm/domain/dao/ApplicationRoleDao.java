package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;

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

    List<ClientRole> getClientRolesForApplication(Application application);
    List<ClientRole> getAllClientRoles();

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(int limit, int offset, int maxWeightAvailable);
    PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int limit, int offset, int maxWeightAvailable);
    PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int limit, int offset);

    String getNextRoleId();

    List<ClientRole> getIdentityRoles(Application application, List<String> roleNames);
}
