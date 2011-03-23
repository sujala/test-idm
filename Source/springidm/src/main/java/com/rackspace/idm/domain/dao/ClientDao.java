package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Permission;

import java.util.List;

public interface ClientDao {

    void add(Client client, String customerUniqueId);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(Permission permission);

    void addUserToClientGroup(String userUniqueId, ClientGroup group);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String customerId, String clientId, String name);

    void deleteDefinedPermission(Permission permission);

    List<Client> findAll();

    Client getClient(String customerId, String clientId);

    Client findByClientId(String clientId);

    Client findByClientname(String clientName);

    Client findByInum(String inum);

    ClientGroup findClientGroupByUniqueId(String uniqueId);

    Clients getByCustomerId(String customerId, int offset, int limit);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    String getUnusedClientInum(String customerInum);

    void removeUserFromGroup(String userUniqueId, ClientGroup group);

    void save(Client client);

    void setAllClientLocked(String customerId, boolean locked);

    void updateClientGroup(ClientGroup group);

    void updateDefinedPermission(Permission permission);
    
    List<Client> getClientsThatHavePermission(Permission permission);
    
    void grantPermissionToClient(Permission permission, Client client);
    
    void revokePermissionFromClient(Permission permission, Client client);
}
