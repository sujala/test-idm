package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.ClientAuthenticationResult;
import com.rackspace.idm.entities.Permission;

public interface ClientService {

    void add(Client client);

    void addDefinedPermission(Permission permission);

    boolean authenticate(String clientId, String clientSecret);
    boolean authenticate(String clientId, String clientSecret);
    //ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteDefinedPermission(Permission permission);

    Clients getByCustomerId(String customerId, int offset, int limit);

    Client getById(String clientId);

    Client getByName(String clientName);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    void save(Client client);

    void softDelete(String clientId);

    void updateDefinedPermission(Permission permission);
}
