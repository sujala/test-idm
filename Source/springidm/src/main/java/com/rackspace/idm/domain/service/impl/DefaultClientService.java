package com.rackspace.idm.domain.service.impl;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.HashHelper;

public class DefaultClientService implements ClientService {

    private final ScopeAccessDao scopeAccessDao;
    private final ClientDao clientDao;
    private final CustomerDao customerDao;
    private final UserDao userDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultClientService(ScopeAccessDao scopeAccessDao,
        ClientDao clientDao, CustomerDao customerDao, UserDao userDao) {
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
        logger.debug("Delete Client: {}", clientId);
        Client client = clientDao.getClientByClientId(clientId);

        if (client == null) {
            String errMsg = String.format("Client with clientId %s not found.",
                clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        
        List<PermissionEntity> definedPermissions = this.getDefinedPermissionsByClient(client);
        
        for (PermissionEntity definedPerm : definedPermissions) {
            this.deleteDefinedPermission(definedPerm);
        }

        List<ClientGroup> groups = clientDao
            .getClientGroupsByClientId(clientId);

        for (ClientGroup group : groups) {
            clientDao.deleteClientGroup(group);
        }

        clientDao.deleteClient(client);
        logger.debug("Deleted Client: {}", clientId);
    }

    @Override
    public void addDefinedPermission(PermissionEntity permission) {
        logger.debug("Define Permission: {}", permission);
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

        ScopeAccess sa = this.scopeAccessDao
            .getScopeAccessForParentByClientId(client.getUniqueId(),
                client.getClientId());

        if (sa == null) {
            sa = new ClientScopeAccessObject();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getCustomerId());
            sa = this.scopeAccessDao.addScopeAccess(client.getUniqueId(), sa);
        }

        PermissionEntity exists = this.scopeAccessDao
            .getPermissionByParentAndPermissionId(sa.getUniqueId(), permission);

        if (exists != null) {
            logger
                .warn(
                    "Couldn't add permission {} because permissionId already taken",
                    client);
            throw new DuplicateException(String.format(
                "PermissionId %s already exists", client.getName()));
        }

        permission = this.scopeAccessDao.definePermission(sa.getUniqueId(),
            permission);
        logger.debug("Defined Permission: {}", permission);
    }

    @Override
    public void deleteDefinedPermission(PermissionEntity permission) {
        logger.debug("Delete Permission: {}", permission);
        PermissionEntity perm = new PermissionEntity();
        perm.setClientId(permission.getClientId());
        perm.setCustomerId(permission.getCustomerId());
        perm.setPermissionId(permission.getPermissionId());
        
        List<PermissionEntity> perms = this.scopeAccessDao.getPermissionsByPermission(permission);
        
        for(PermissionEntity p : perms) {
            logger.debug("Deleting Permission: {}", permission);
            this.scopeAccessDao.removePermissionFromScopeAccess(p);
        }
        logger.debug("Deleted Permission: {}", permission);
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
    public PermissionEntity getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        logger.debug("Find Permission: {} by ClientId: {}", permissionId, clientId);

        Client client = this.getClient(clientId);
        
        PermissionEntity permission = new PermissionEntity();
        permission.setPermissionId(permissionId);
        permission.setCustomerId(client.getCustomerId());
        permission.setClientId(client.getClientId());

        permission = this.scopeAccessDao.getPermissionByParentAndPermissionId(
            client.getUniqueId(), permission);

        logger.debug("Found Permission: {}", permission);
        return permission;
    }

    @Override
    public List<PermissionEntity> getDefinedPermissionsByClient(
        Client client) {
        logger.debug("Find Permission by ClientId: {}", client.getClientId());
        PermissionEntity filter = new PermissionEntity();
        filter.setClientId(client.getClientId());
        filter.setCustomerId(client.getCustomerId());
        
        List<PermissionEntity> permissions = this.scopeAccessDao
            .getPermissionsByParentAndPermissionId(client.getUniqueId(),
                filter);
        logger.debug("Found {} Permission(s) by ClientId: {}", permissions.size(), client.getClientId());
        return permissions;
    }

    @Override
    public ClientSecret resetClientSecret(Client client) {
        if (client == null) {
            throw new IllegalArgumentException();
        }
        logger.debug("Reseting Client secret ClientId: {}", client.getClientId());

        ClientSecret clientSecret = null;
        try {
            clientSecret = ClientSecret.newInstance(HashHelper.getRandomSha1());
            client.setClientSecretObj(clientSecret);
            clientDao.updateClient(client);
            logger.debug("Reset Client secret ClientId: {}", client.getClientId());
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
    public void updateDefinedPermission(PermissionEntity permission) {
        this.scopeAccessDao.updatePermissionForScopeAccess(permission);
    }

    @Override
    public void addClientGroup(ClientGroup clientGroup) {
        logger.debug("Adding Client group: {}", clientGroup);
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
        logger.debug("Added Client group: {}", clientGroup);
    }

    @Override
    public void addUserToClientGroup(String username, String customerId,
        String clientId, String groupName) {
        logger.debug("Adding User: {} to Client group: {}", username, groupName);
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
        logger.debug("Added User: {} to Client group: {}", username, groupName);
    }

    @Override
    public void deleteClientGroup(String customerId, String clientId,
        String groupName) {
        logger.debug("Deleting Client Group: {} , Client: {}", groupName, clientId);
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException();
        }

        ClientGroup groupToDelete = clientDao.getClientGroup(customerId,
            clientId, groupName);

        if (groupToDelete == null) {
            throw new NotFoundException(
                String
                    .format(
                        "Client Group with Name %s, ClientId %s, and CustomerId %s not found",
                        customerId, clientId, groupName));
        }

        clientDao.deleteClientGroup(groupToDelete);
        logger.debug("Deleted Client Group: {} from Client: {}", groupName, clientId);
        
        logger.debug("Deleting Users from Client Group: {} , Client: {}", groupName, clientId);
        userDao.removeUsersFromClientGroup(groupToDelete);
        logger.debug("Deleted Users from Client Group: {} , Client: {}", groupName, clientId);
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
        logger.debug("Finding Client Groups by ClientID: {}", clientId);
        List<ClientGroup> groups = clientDao
            .getClientGroupsByClientId(clientId);
        logger.debug("Found {} Client Groups by ClientID: {}", groups.size(), clientId);
        return groups;
    }

    @Override
    public void removeUserFromClientGroup(String username,
        ClientGroup clientGroup) {
        logger.debug("Removing User: {} from Client Group: {}", username, clientGroup);
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        User user = userDao.getUserByUsername(username);
        if (user == null) {
            String msg = "User not found: " + username; 
            logger.warn(msg);
            throw new NotFoundException(msg);
        }

        Customer customer = customerDao.getCustomerByCustomerId(clientGroup
            .getCustomerId());

        if (customer == null) {
            String msg = "Customer not found: " + clientGroup.getCustomerId();  
            logger.warn(msg);
            throw new NotFoundException(msg);
        }

        try {
            clientDao.removeUserFromGroup(user.getUniqueId(), clientGroup);
            logger.debug("Removed User: {} from Client Group: {}", username, clientGroup);
        } catch (NotFoundException nfe) {
            logger.warn("User {} isn't in group {}", user, clientGroup);
        }
    }

    @Override
    public List<ClientGroup> getClientGroupsForUserByClientIdAndType(
        String username, String clientId, String type) {

        logger.debug("Getting Groups for User: {} by ClientId: {}", username, clientId);
        
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

        logger.debug("Got {} Group(s) for User: {} - {}", new Object[] { groups.size(), username, clientId});
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

        logger.debug("Got {} Group(s) for User: {} - {}", groups.size(), username);
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
    public PermissionEntity checkAndGetPermission(String customerId,
        String clientId, String permissionId)

    throws NotFoundException {
        logger.debug("Check and get Permission: {} for ClientId: {}", permissionId, clientId);
        PermissionEntity permission = this
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);

        if (permission == null
            || !customerId.equalsIgnoreCase(permission.getCustomerId())
            || !clientId.equalsIgnoreCase(permission.getClientId())
            || !permission.getEnabled()) {
            String errorMsg = String.format("Permission Not Found: %s",
                permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Found Permission: {} for ClientId: {}", permission, clientId);
        return permission;
    }

    private void addUserToClientGroup(String username, ClientGroup clientGroup) {
        logger.debug("Add User: {} to ClientGroup: {}", username, clientGroup);
        if (StringUtils.isBlank(username)) {
            logger.warn("Username is blank");
            throw new IllegalArgumentException("username cannot be blank");
        }

        User user = userDao.getUserByUsername(username);
        if (user == null) {
            logger.warn("User: {} not found", username);
            throw new NotFoundException(String.format(
                "User with username %s not found", username));
        }

        if (user.isDisabled()) {
            logger.warn("User: {} is disabled", username);
            throw new UserDisabledException(String.format(
                "User %s is disabled and cannot be added to group", username));
        }

        try {
            clientDao.addUserToClientGroup(user.getUniqueId(), clientGroup);
            logger.debug("Added User: {} to ClientGroup: {}", username, clientGroup);
        } catch (DuplicateException drx) {
            logger.warn("User {} already in group {}", user, clientGroup);
        }
    }

    @Override
    public Clients getClientServices(Client client) {
        logger.debug("Finding Client Services for Client: {}", client.getClientId());
        if (client == null || client.getUniqueId() == null) {
            throw new IllegalArgumentException(
                "Client cannont be null and must have uniqueID");
        }
        List<ScopeAccess> services = this.scopeAccessDao
            .getScopeAccessesByParent(client.getUniqueId());

        List<Client> clientList = new ArrayList<Client>();

        for (ScopeAccess service : services) {
            if (service instanceof ScopeAccess) {
                clientList.add(this.getById(service.getClientId()));
            }
        }

        Clients clients = new Clients();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());

        logger.debug("Found {} Client Service(s) for Client: {}", clientList.size(), client.getClientId());
        return clients;
    }
}
