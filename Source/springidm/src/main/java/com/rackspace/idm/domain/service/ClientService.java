package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.DefinedPermission;

public interface ClientService {

    void add(Client client);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(DefinedPermission permission);
    
    void addUserToClientGroup(String username, String customerId, String clientId, String groupName);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String customerId, String clientId, String groupName);

    void deleteDefinedPermission(DefinedPermission permission);

    Clients getByCustomerId(String customerId, int offset, int limit);

    Client getById(String clientId);

    Client getByName(String clientName);

    Client getClient(String customerId, String clientId);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    List<ClientGroup> getClientGroupsForUser(String username);

    List<ClientGroup> getClientGroupsForUserByClientIdAndType(String username, String clientId, String type);

    Clients getClientServices(Client client);
    
    DefinedPermission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);
    
    DefinedPermission checkAndGetPermission(String customerId, String clientId, String permissionId);

    List<DefinedPermission> getDefinedPermissionsByClient(Client client);

    boolean isUserMemberOfClientGroup(String username, ClientGroup group);

    void removeUserFromClientGroup(String username, ClientGroup clientGroup);

    ClientSecret resetClientSecret(Client client);

    void save(Client client);

    void softDelete(String clientId);
    
    void updateClient(Client client);

    void updateClientGroup(ClientGroup group);

    void updateDefinedPermission(DefinedPermission permission);

    Client getClientByScope(String scope);
    
    List<Client> getAvailableScopes();
    
    void addClientRole(ClientRole role);
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    List<ClientRole> getClientRolesByClientId(String clientId);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);
    
    List<ClientRole> getAllClientRoles();
    
    List<Client> getOpenStackServices();
}
