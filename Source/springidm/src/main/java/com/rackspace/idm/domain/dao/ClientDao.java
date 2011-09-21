package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.Clients;

public interface ClientDao {

    void addClient(Client client);

    void addClientGroup(ClientGroup clientGroup, String clientUniqueId);

    void addUserToClientGroup(String userUniqueId, ClientGroup group);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void deleteClient(Client client);

    void deleteClientGroup(ClientGroup group);

    List<Client> getAllClients();

    Client getClientByClientId(String clientId);

    Client getClientByClientname(String clientName);

    Client getClientByCustomerIdAndClientId(String customerId, String clientId);

    Client getClientById(String inum);
    
    Client getClientByScope(String scope);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    ClientGroup getClientGroupByUniqueId(String uniqueId);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    Clients getClientsByCustomerId(String customerId, int offset, int limit);
    
    boolean isUserInClientGroup(String username, String groupDN);
    
    void removeUserFromGroup(String userUniqueId, ClientGroup group);

    void setClientsLockedFlagByCustomerId(String customerId, boolean locked);
    
    void updateClient(Client client);
    
    void updateClientGroup(ClientGroup group);
    
    List<Client> getAvailableScopes();
    
    void addClientRole(String clientUniqueId, ClientRole role);
    
    void deleteClientRole(ClientRole role);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    List<ClientRole> getClientRolesByClientId(String clientId);
    
    void updateClientRole(ClientRole role);
    
    ClientRole getClientRoleById(String id);
    
    String getNextClientId();

    String getNextRoleId();
}
