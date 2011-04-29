package com.rackspace.idm.domain.service.impl;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.HashHelper;

public class DefaultClientService implements ClientService {

    private final ScopeAccessObjectDao scopeAccessDao;
    private final ClientDao clientDao;
    private final CustomerDao customerDao;
    private final UserDao userDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultClientService(ScopeAccessObjectDao scopeAccessDao, ClientDao clientDao, CustomerDao customerDao,
        UserDao userDao) {
        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.userDao = userDao;
        this.scopeAccessDao = scopeAccessDao;
    }

    @Override
    public void add(Client client) {
        logger.debug("Adding Client: {}", client);
        Customer customer = customerDao.getCustomerByCustomerId(client
            .getCustomerId());

        if (customer == null) {
            logger.warn(
                "Couldn't add client {} because customerId doesn't exist",
                client.getCustomerId());
            throw new NotFoundException("Customer doesn't exist");
        }

        Client exists = clientDao.getClientByClientname(client.getName());

        if (exists != null) {
            logger.warn(
                "Couldn't add client {} because clientname already taken",
                client);
            throw new DuplicateException(String.format(
                "Clientname %s already exists", client.getName()));
        }

        try {
            client.setClientId(HashHelper.MakeSHA1Hash(client.getName()));
            client.setClientSecretObj(ClientSecret.newInstance(HashHelper
                .getRandomSha1()));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unsupported hashing algorithm - {}", e);
            throw new IllegalStateException("Unsupported hashing algorithm", e);
        }

        client.setOrgInum(customer.getInum());
        client.setInum(clientDao.getUnusedClientInum(customer.getInum()));

        clientDao.addClient(client, customer.getUniqueId());
        logger.debug("Added Client: {}", client);
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId,
        String clientSecret) {
        return clientDao.authenticate(clientId, clientSecret);
    }

    @Override
    public void delete(String clientId) {
        Client client = clientDao.getClientByClientId(clientId);

        if (client == null) {
            String errMsg = String.format("Client with clientId %s not found.",
                clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        List<Permission> permissions = clientDao
            .getDefinedPermissionsByClientId(clientId);

        for (Permission perm : permissions) {
            clientDao.deleteDefinedPermission(perm);
        }

        List<ClientGroup> groups = clientDao
            .getClientGroupsByClientId(clientId);

        for (ClientGroup group : groups) {
            clientDao.deleteClientGroup(group);
        }

        clientDao.deleteClient(client);
    }

    @Override
    public void addDefinedPermission(Permission permission) {

        Customer customer = customerDao.getCustomerByCustomerId(permission
            .getCustomerId());

        if (customer == null) {
            logger.warn(
                "Couldn't add permission {} because customerId doesn't exist",
                permission.getCustomerId());
            throw new IllegalStateException("Customer doesn't exist");
        }

        Client client = clientDao.getClientByClientId(permission.getClientId());

        if (client == null) {
            logger.warn(
                "Couldn't add permission {} because clientId doesn't exist",
                permission.getClientId());
            throw new IllegalStateException("Client doesn't exist");
        }

        Permission exists = clientDao
            .getDefinedPermissionByClientIdAndPermissionId(
                permission.getClientId(), permission.getPermissionId());

        if (exists != null) {
            logger
                .warn(
                    "Couldn't add permission {} because permissionId already taken",
                    client);
            throw new DuplicateException(String.format(
                "PermissionId %s already exists", client.getName()));
        }

        clientDao.addDefinedPermission(permission, client.getUniqueId());
    }

    @Override
    public void deleteDefinedPermission(Permission permission) {
        Permission p = this.clientDao
            .getDefinedPermissionByClientIdAndPermissionId(
                permission.getClientId(), permission.getPermissionId());

        if (p == null) {
            throw new NotFoundException("Defined Permission not found.");
        }

        List<Client> clientsWithPermission = this.clientDao
            .getClientsThatHavePermission(p);

        for (Client client : clientsWithPermission) {
            this.clientDao.revokePermissionFromClient(p, client);
        }

        clientDao.deleteDefinedPermission(permission);
    }

    @Override
    public Clients getByCustomerId(String customerId, int offset, int limit) {

        return clientDao.getClientsByCustomerId(customerId, offset, limit);
    }

    @Override
    public Client getById(String clientId) {
        return clientDao.getClientByClientId(clientId);
    }

    @Override
    public Client getClient(String customerId, String clientId) {
        return clientDao.getClientByCustomerIdAndClientId(customerId, clientId);
    }

    @Override
    public Client getByName(String clientName) {
        return clientDao.getClientByClientname(clientName);
    }

    @Override
    public Permission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        Permission permission = clientDao
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);
        return permission;
    }

    @Override
    public List<Permission> getDefinedPermissionsByClientId(String clientId) {
        List<Permission> permissions = clientDao
            .getDefinedPermissionsByClientId(clientId);
        return permissions;
    }

    @Override
    public ClientSecret resetClientSecret(Client client) {

        if (client == null) {
            throw new IllegalArgumentException();
        }

        ClientSecret clientSecret = null;
        try {
            clientSecret = ClientSecret.newInstance(HashHelper.getRandomSha1());
            client.setClientSecretObj(clientSecret);
            clientDao.updateClient(client);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unsupported hashing algorithm - {}", e);
            throw new IllegalStateException("Unsupported hashing algorithm", e);
        }
        return clientSecret;
    }

    @Override
    public void save(Client client) {
        clientDao.updateClient(client);
    }

    @Override
    public void softDelete(String clientId) {
        logger.info("Soft Deleting client: {}", clientId);
        Client client = this.clientDao.getClientByClientId(clientId);
        client.setSoftDeleted(true);
        this.clientDao.updateClient(client);
        logger.info("Soft Deleted cilent: {}", clientId);
    }

    @Override
    public void grantPermission(String clientId, Permission p) {
        
        // p has resourceGroup info coming in
        //
        // exists = findPermision(p) --> has no resourceGroup information
        // if not exists error
        //
        // getOrCreateScopeAccess(targetClientId, permisison.getclientId)
        //
        // p = scopeAccess.addPermission(p)
        
        
        
        
        Client targetClient = this.clientDao.getClientByClientId(clientId);

        if (targetClient == null) {
            throw new NotFoundException("Client Not Found");
        }

        Permission permission = this.clientDao
            .getDefinedPermissionByClientIdAndPermissionId(p.getClientId(),
                p.getPermissionId());

        if (permission == null) {
            throw new NotFoundException("Permission Not Found");
        }

        try {
            clientDao.grantPermissionToClient(permission, targetClient);
        } catch (DuplicateException drx) {
            logger.warn("Client {} already has permission {}", targetClient,
                permission);
            return;
        }
    }

    @Override
    public void revokePermission(String clientId, Permission p) {

        Client targetClient = getClient(clientId);

        if (targetClient == null) {
            throw new NotFoundException("Client Not Found");
        }

        Permission permission = this.clientDao
            .getDefinedPermissionByClientIdAndPermissionId(p.getClientId(),
                p.getPermissionId());

        if (permission == null) {
            throw new NotFoundException("Permission Not Found");
        }

        try {
            clientDao.revokePermissionFromClient(permission, targetClient);
        } catch (NotFoundException nfe) {
            logger.warn("Client {} doesn't have permission {}", targetClient,
                permission);
            return;
        }
    }

    private Client getClient(String clientId) {
        Client targetClient = this.clientDao.getClientByClientId(clientId);
        if (targetClient == null) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return targetClient;
    }

    @Override
    public void updateDefinedPermission(Permission permission) {
        clientDao.updateDefinedPermission(permission);
    }

    @Override
    public void addClientGroup(ClientGroup clientGroup) {

        Client client = clientDao
            .getClientByClientId(clientGroup.getClientId());

        if (client == null) {
            logger.warn("Couldn't add group {} because clientId doesn't exist",
                clientGroup.getClientId());
            throw new NotFoundException("Client doesn't exist");
        }

        Customer customer = customerDao.getCustomerByCustomerId(clientGroup
            .getCustomerId());

        if (customer == null) {
            logger.warn("Could not add group {} because customer {} not found",
                clientGroup.getName(), clientGroup.getCustomerId());
            throw new NotFoundException();
        }

        clientDao.addClientGroup(clientGroup, client.getUniqueId());
    }

    @Override
    public void addUserToClientGroup(String username, String customerId,
        String clientId, String groupName) {

        ClientGroup group = this
            .getClientGroup(customerId, clientId, groupName);

        if (group == null) {
            String errMsg = String
                .format(
                    "ClientGroup with Name %s, ClientId %s, and CustomerId %s not found.",
                    groupName, clientId, customerId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        this.addUserToClientGroup(username, group);
    }

    @Override
    public void deleteClientGroup(String customerId, String clientId,
        String groupName) {

        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException();
        }

        ClientGroup groupToDelete = clientDao.getClientGroup(customerId, clientId, groupName);

        if (groupToDelete == null) {
            throw new NotFoundException(
                String
                    .format(
                        "Client Group with Name %s, ClientId %s, and CustomerId %s not found",
                        customerId, clientId, groupName));
        }

    	clientDao.deleteClientGroup(groupToDelete);
        userDao.removeUsersFromClientGroup(groupToDelete);
    }

    @Override
    public ClientGroup getClientGroup(String customerId, String clientId,
        String groupName) {
        ClientGroup group = clientDao.getClientGroup(customerId, clientId,
            groupName);

        return group;
    }

    @Override
    public List<ClientGroup> getClientGroupsByClientId(String clientId) {
        List<ClientGroup> groups = clientDao
            .getClientGroupsByClientId(clientId);
        return groups;
    }

    @Override
    public void removeUserFromClientGroup(String username,
        ClientGroup clientGroup) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        User user = userDao.getUserByUsername(username);
        if (user == null) {
            throw new NotFoundException();
        }

        Customer customer = customerDao.getCustomerByCustomerId(clientGroup
            .getCustomerId());

        if (customer == null) {
            throw new NotFoundException();
        }

        try {
            clientDao.removeUserFromGroup(user.getUniqueId(), clientGroup);
        } catch (NotFoundException nfe) {
            logger.warn("User {} isn't in group {}", user, clientGroup);
            return;
        }
    }

    @Override
    public List<ClientGroup> getClientGroupsForUserByClientIdAndType(
        String username, String clientId, String type) {

        logger.debug("Getting Groups for User: {}", username);
        String[] groupIds = userDao.getGroupIdsForUser(username);

        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        if (groupIds == null) {
            return groups;
        }

        boolean filterByClient = !StringUtils.isBlank(clientId);
        boolean filterByType = !StringUtils.isBlank(type);

        for (String groupId : groupIds) {
            boolean addGroup = true;
            ClientGroup group = clientDao.getClientGroupByUniqueId(groupId);
            if (group != null) {
                if (filterByClient
                    && !group.getClientId().equalsIgnoreCase(clientId)) {
                    addGroup = false;
                }
                if (filterByType && !group.getType().equalsIgnoreCase(type)) {
                    addGroup = false;
                }
                if (addGroup) {
                    groups.add(group);
                }
            }
        }

        logger.info("Got Groups for User: {} - {}", username, groups);
        return groups;
    }

    @Override
    public List<ClientGroup> getClientGroupsForUser(String username) {
        logger.debug("Getting Groups for User: {}", username);
        String[] groupIds = userDao.getGroupIdsForUser(username);

        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        if (groupIds == null) {
            return groups;
        }

        for (String groupId : groupIds) {
            ClientGroup group = clientDao.getClientGroupByUniqueId(groupId);
            if (group != null) {
                groups.add(group);
            }
        }

        logger.debug("Got Groups for User: {} - {}", username, groups);
        return groups;
    }

    @Override
    public boolean isUserMemberOfClientGroup(String username, ClientGroup group) {
        logger.debug("Is user {} member of {}", username, group);

        ClientGroup foundGroup = this.clientDao.getClientGroup(
            group.getCustomerId(), group.getClientId(), group.getName());
        if (foundGroup == null) {
            String errMsg = String.format("ClientGroup %s not found", group);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        boolean isMember = this.clientDao.isUserInClientGroup(username,
            foundGroup.getUniqueId());

        logger.debug(String.format("Is user %s member of %s - %s", username,
            group, isMember));

        return isMember;
    }

    @Override
    public void updateClientGroup(ClientGroup group) {

        clientDao.updateClientGroup(group);
    }

    @Override
    public List<Client> getClientsThatHavePermission(Permission permission) {
        Permission p = this.clientDao
            .getDefinedPermissionByClientIdAndPermissionId(
                permission.getClientId(), permission.getPermissionId());

        if (p == null) {
            throw new NotFoundException("Permission Not Found");
        }

        return this.clientDao.getClientsThatHavePermission(permission);
    }
    
    @Override
    public PermissionObject checkAndGetPermission(String customerId, String clientId, String permissionId) 

    throws NotFoundException {
    
        Permission permission = this.getDefinedPermissionByClientIdAndPermissionId(clientId,
        permissionId);

        if (permission == null || !customerId.equalsIgnoreCase(permission.getCustomerId())
            || !clientId.equalsIgnoreCase(permission.getClientId()) || !permission.getEnabled()) {
            String errorMsg = String.format("Permission Not Found: %s", permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        PermissionObject permObj = new PermissionObject(permission.getCustomerId(), permission.getClientId(), permission.getPermissionId(),
            permission.getValue());
      
        return permObj;
    }  

    private void addUserToClientGroup(String username, ClientGroup clientGroup) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        User user = userDao.getUserByUsername(username);
        if (user == null) {
            throw new NotFoundException(String.format(
                "User with username %s not found", username));
        }

        if (user.isDisabled()) {
            throw new UserDisabledException(String.format(
                "User %s is disabled and cannot be added to group", username));
        }

        try {
            clientDao.addUserToClientGroup(user.getUniqueId(), clientGroup);
        } catch (DuplicateException drx) {
            logger.warn("User {} already in group {}", user, clientGroup);
            return;
        }
    }

    @Override
    public Clients getClientServices(Client client) {
        if (client == null || client.getUniqueId() == null) {
            throw new IllegalArgumentException("Client cannont be null and must have uniqueID");
        }
        List<ScopeAccessObject> services = this.scopeAccessDao.getScopeAccessesByParent(client.getUniqueId());
        
        List<Client> clientList = new ArrayList<Client>();
        
        for (ScopeAccessObject service : services) {
            if (service instanceof ScopeAccessObject) {
                clientList.add(this.getById(service.getClientId()));
            }
        }
        
        Clients clients = new Clients();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());

        return clients;
    }
}
