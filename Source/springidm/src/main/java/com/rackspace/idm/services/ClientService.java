package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;

public interface ClientService {

    void add(Client client);

    boolean authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    List<Client> getByCustomerId(String customerId);

    Client getById(String clientId);

    Client getByName(String clientName);

    void save(Client client);
    
    void softDelete(String clientId);
    
    List<Permission> getDefinedPermissionsByClientId(String clientId);
    
    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId, String permissionId);
    
    void addDefinedPermission(Permission permission);
    
    void updateDefinedPermission(Permission permission);
    
    void deleteDefinedPermission(Permission permission);
}
