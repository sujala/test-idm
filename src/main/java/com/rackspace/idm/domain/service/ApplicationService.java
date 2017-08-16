package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ApplicationService {

    void add(Application client);

    void delete(String clientId);

    Application loadApplication(String applicationId);
    
    Application getById(String clientId);

    Application getByName(String clientName);

    Iterable<Application> getByType(String type);

    Application checkAndGetApplication(String applicationId);

    Application checkAndGetApplicationByName(String name);

    Applications getClientServices(Application client);

    Application getApplicationByScopeAccess(ScopeAccess scopeAccess);

    ClientSecret resetClientSecret(Application client);

    void save(Application client);
    
    void updateClient(Application client);

    void addClientRole(ClientRole role);

    void addClientRole(ClientRole role, String roleId);
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    Iterable<ClientRole> getClientRolesByClientId(String clientId);

    Iterable<ClientRole> getClientRolesByRoleType(RoleTypeEnum roleType);

    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);

    /**
     * Return the cached, immutable, version of the client role by id.
     *
     * @param id
     * @return
     */
    ImmutableClientRole getCachedClientRoleById(String id);

    /**
     * Return the cached, immutable, version of the client role by name.
     *
     * @param name
     * @return
     */


    ImmutableClientRole getCachedClientRoleByName(String name);


    /**
     * Load the specified ClientRoles by name and maxWeight. The role is returned if the specified weight
     * is LESS THAN the returned ClientRole's weight.
     *
     * Returns an empty paginator context if roleName is null.
     *
     * @param roleName
     * @param maxWeightAvailable
     * @return
     */
    PaginatorContext<ClientRole> getAvailableClientRolesByName(String roleName, int maxWeightAvailable, int offeset, int limit);

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable);

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable);

    Iterable<Application> getOpenStackServices();

    ClientRole getUserIdentityRole(EndUser user);
}
