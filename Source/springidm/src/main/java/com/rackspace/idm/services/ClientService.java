package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.ClientAuthenticationResult;
import com.rackspace.idm.entities.Permission;

public interface ClientService {

    void add(Client client);
    
    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(Permission permission);
    
    void addUserToClientGroup(String username, ClientGroup clientGroup);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    boolean authenticateDeprecated(String clientId, String clientSecret);

    void delete(String clientId);
    
    void deleteClientGroup(String customerId, String clientId, String name);

    void deleteDefinedPermission(Permission permission);
    
    Clients getByCustomerId(String customerId, int offset, int limit);

    Client getById(String clientId);
    
    Client getByName(String clientName);
    
    Client getClient(String customerId, String clientId);
    
    ClientGroup getClientGroup(String customerId, String clientId, String groupName);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    List<ClientGroup> getClientGroupsForUser(String username);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);
    
    void removeUserFromClientGroup(String username, ClientGroup clientGroup);

    ClientSecret resetClientSecret(Client client);

    void save(Client client);

    void softDelete(String clientId);

    void updateDefinedPermission(Permission permission);
}
