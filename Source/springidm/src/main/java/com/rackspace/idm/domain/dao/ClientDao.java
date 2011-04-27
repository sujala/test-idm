package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Permission;

public interface ClientDao {

    void addClient(Client client, String customerUniqueId);

    void addClientGroup(ClientGroup clientGroup, String clientUniqueId);

    void addDefinedPermission(Permission permission, String clientUniqueId);

    void addUserToClientGroup(String userUniqueId, ClientGroup group);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void deleteClient(String clientId);

    ClientGroup deleteClientGroup(String customerId, String clientId, String name);

    void deleteDefinedPermission(Permission permission);

    List<Client> getAllClients();

    Client getClientByClientId(String clientId);

    Client getClientByClientname(String clientName);

    Client getClientByCustomerIdAndClientId(String customerId, String clientId);

    Client getClientByInum(String inum);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    ClientGroup getClientGroupByUniqueId(String uniqueId);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    Clients getClientsByCustomerId(String customerId, int offset, int limit);

    List<Client> getClientsThatHavePermission(Permission permission);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    String getUnusedClientInum(String customerInum);

    void grantPermissionToClient(Permission permission, Client client);
    
    boolean isUserInClientGroup(String username, String groupDN);
    
    void removeUserFromGroup(String userUniqueId, ClientGroup group);

    void revokePermissionFromClient(Permission permission, Client client);

    void setClientsLockedFlagByCustomerId(String customerId, boolean locked);
    
    void updateClient(Client client);
    
    void updateClientGroup(ClientGroup group);
    
    void updateDefinedPermission(Permission permission);
}
