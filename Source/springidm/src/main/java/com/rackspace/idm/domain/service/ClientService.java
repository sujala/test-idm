package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Permission;

public interface ClientService {

    void add(Client client);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(Permission permission);
    
    void addUserToClientGroup(String username, String customerId, String clientId, String groupName);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String customerId, String clientId, String name);

    void deleteDefinedPermission(Permission permission);

    Clients getByCustomerId(String customerId, int offset, int limit);

    Client getById(String clientId);

    Client getByName(String clientName);

    Client getClient(String customerId, String clientId);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    List<ClientGroup> getClientGroupsForUser(String username);

    List<ClientGroup> getClientGroupsForUserByClientIdAndType(String username, String clientId, String type);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);
    
    Permission checkAndGetPermission(String customerId, String clientId, String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    void grantPermission(String clientId, Permission p);
    
    boolean isUserMemberOfClientGroup(String username, ClientGroup group);

    void revokePermission(String clientId, Permission p);

    void removeUserFromClientGroup(String username, ClientGroup clientGroup);

    ClientSecret resetClientSecret(Client client);

    void save(Client client);

    void softDelete(String clientId);

    void updateClientGroup(ClientGroup group);

    void updateDefinedPermission(Permission permission);
    
    List<Client> getClientsThatHavePermission(Permission permission);
}
