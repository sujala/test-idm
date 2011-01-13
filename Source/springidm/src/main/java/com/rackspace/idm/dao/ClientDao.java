package com.rackspace.idm.dao;

import java.util.List;

import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.Permission;

public interface ClientDao {

    void add(Client client);

    void addDefinedPermission(Permission permission);

    boolean authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteDefinedPermission(Permission permission);

    List<Client> findAll();

    Client findByClientId(String clientId);

    Client findByClientname(String clientName);

    Client findByInum(String inum);

    Clients getByCustomerId(String customerId, int offset, int limit);

    String getClientDnByClientId(String clientId);

    Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);

    List<Permission> getDefinedPermissionsByClientId(String clientId);

    String getUnusedClientInum(String customerInum);

    void save(Client client);

    void setAllClientLocked(String customerId, boolean locked);

    void updateDefinedPermission(Permission permission);
}
