package com.rackspace.idm.domain.dao;

import java.util.List;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FilterParam;

public interface ApplicationDao {

    void addClient(Application client);

    void addClientGroup(ClientGroup clientGroup, String clientUniqueId);

    void addUserToClientGroup(String userUniqueId, ClientGroup group);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void deleteClient(Application client);

    void deleteClientGroup(ClientGroup group);

    List<Application> getAllClients();

    Application getClientByClientId(String clientId);

    Application getClientByClientname(String clientName);

    Application getClientByCustomerIdAndClientId(String customerId, String clientId);

    Application getClientById(String inum);
    
    Application getClientByScope(String scope);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    ClientGroup getClientGroupByUniqueId(String uniqueId);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    Applications getClientsByCustomerId(String customerId, int offset, int limit);
    
    Applications getAllClients(FilterParam[] filters, int offset, int limit);
    
    boolean isUserInClientGroup(String username, String groupDN);
    
    void removeUserFromGroup(String userUniqueId, ClientGroup group);

    void setClientsLockedFlagByCustomerId(String customerId, boolean locked);
    
    void updateClient(Application client);
    
    void updateClientGroup(ClientGroup group);
    
    List<Application> getAvailableScopes();
    
    void addClientRole(String clientUniqueId, ClientRole role);
    
    void deleteClientRole(ClientRole role);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    List<ClientRole> getClientRolesByClientId(String clientId);
    
    List<ClientRole> getAllClientRoles(FilterParam[] filters);
    
    void updateClientRole(ClientRole role);
    
    ClientRole getClientRoleById(String id);
    
    List<ClientRole> getAllClientRoles();
    
    List<Application> getOpenStackServices();

    String getNextRoleId();
}
