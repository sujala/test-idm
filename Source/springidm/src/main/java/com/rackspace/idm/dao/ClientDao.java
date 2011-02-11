package com.rackspace.idm.dao;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientAuthenticationResult;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.Permission;

import java.util.List;

public interface ClientDao {

    void add(Client client, String customerUniqueId);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(Permission permission);

    void addUserToClientGroup(String userUniqueId, ClientGroup group);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    boolean authenticateDeprecated(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String clientId, String name);

    void deleteDefinedPermission(Permission permission);

    List<Client> findAll();

    Client findByClientId(String clientId);

    Client findByClientname(String clientName);

    Client findByInum(String inum);

    ClientGroup findClientGroupByUniqueId(String uniqueId);

    Clients getByCustomerId(String customerId, int offset, int limit);

    String getClientDnByClientId(String clientId);

    ClientGroup getClientGroupByClientIdAndGroupName(String clientId,
        String name);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    String getUnusedClientInum(String customerInum);

    void removeUserFromGroup(String userUniqueId, ClientGroup group);

    void save(Client client);

    void setAllClientLocked(String customerId, boolean locked);

    void updateDefinedPermission(Permission permission);
}
