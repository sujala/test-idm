package com.rackspace.idm.dao;

import java.util.List;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;

public interface ClientDao {

    void add(Client client);

    Client findByClientname(String clientName);

    Client findByClientId(String clientId);

    Client findByInum(String inum);

    List<Client> findAll();

    void save(Client client);

    void setAllClientLocked(String customerId, boolean locked);

    void delete(String clientId);

    String getClientDnByClientId(String clientId);

    String getUnusedClientInum(String customerInum);

    boolean authenticate(String clientId, String clientSecret);

    List<Client> getByCustomerId(String customerId);

    void addDefinedPermission(Permission permission);

    void updateDefinedPermission(Permission permission);

    void deleteDefinedPermission(Permission permission);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);
}
