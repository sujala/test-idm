package com.rackspace.idm.services;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.entities.*;
import org.slf4j.Logger;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.Clients;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.util.HashHelper;

public class DefaultClientService implements ClientService {

    private ClientDao clientDao;
    private CustomerDao customerDao;
    private UserDao userDao;
    private Logger logger;

    public DefaultClientService(ClientDao clientDao, CustomerDao customerDao,
        UserDao userDao, Logger logger) {
        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.userDao = userDao;
        this.logger = logger;
    }

    public void add(Client client) {
        logger.info("Adding Client: {}", client);
        Customer customer = customerDao
            .findByCustomerId(client.getCustomerId());

        if (customer == null) {
            logger.warn(
                "Couldn't add client {} because customerId doesn't exist",
                client.getCustomerId());
            throw new NotFoundException("Customer doesn't exist");
        }

        Client exists = clientDao.findByClientname(client.getName());

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

        client.setOwner(GlobalConstants.INUM_PREFIX + customer.getInum());
        client.setInum(clientDao.getUnusedClientInum(customer.getInum()));

        clientDao.add(client);
        logger.info("Added Client: {}", client);
    }

    public boolean authenticateDeprecated(String clientId, String clientSecret) {
        return clientDao.authenticateDeprecated(clientId, clientSecret);
    }

    @Override
    public ClientAuthenticationResult authenticate(String clientId,
        String clientSecret) {
        return clientDao.authenticate(clientId, clientSecret);
    }

    public void delete(String clientId) {
        clientDao.delete(clientId);
    }

    public void addDefinedPermission(Permission permission) {

        Customer customer = customerDao.findByCustomerId(permission
            .getCustomerId());

        if (customer == null) {
            logger.warn(
                "Couldn't add permission {} because customerId doesn't exist",
                permission.getCustomerId());
            throw new IllegalStateException("Customer doesn't exist");
        }

        Client client = clientDao.findByClientId(permission.getClientId());

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

        clientDao.addDefinedPermission(permission);
    }

    public void deleteDefinedPermission(Permission permission) {
        clientDao.deleteDefinedPermission(permission);
    }

    public Clients getByCustomerId(String customerId, int offset, int limit) {
        // FIXME: read the default offset and limit from config file
        // instead of the Constants class.

        if (offset < GlobalConstants.LDAP_PAGING_DEFAULT_OFFSET) {
            offset = GlobalConstants.LDAP_PAGING_DEFAULT_OFFSET;
        }

        if (limit < 1) {
            limit = GlobalConstants.LDAP_PAGING_DEFAULT_LIMIT;
        } else if (limit > GlobalConstants.LDAP_PAGING_MAX_LIMIT) {
            limit = GlobalConstants.LDAP_PAGING_MAX_LIMIT;
        }

        return clientDao.getByCustomerId(customerId, offset, limit);
    }

    public Client getById(String clientId) {
        return clientDao.findByClientId(clientId);
    }

    public Client getByName(String clientName) {
        return clientDao.findByClientname(clientName);
    }

    public Permission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        Permission permission = clientDao
            .getDefinedPermissionByClientIdAndPermissionId(clientId,
                permissionId);
        return permission;
    }

    public List<Permission> getDefinedPermissionsByClientId(String clientId) {
        List<Permission> permissions = clientDao
            .getDefinedPermissionsByClientId(clientId);
        return permissions;
    }

    public ClientSecret resetClientSecret(Client client) {

        if (client == null) {
            throw new IllegalArgumentException();
        }

        ClientSecret clientSecret = null;
        try {
            clientSecret = ClientSecret.newInstance(HashHelper.getRandomSha1());
            client.setClientSecretObj(clientSecret);
            clientDao.save(client);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unsupported hashing algorithm - {}", e);
            throw new IllegalStateException("Unsupported hashing algorithm", e);
        }
        return clientSecret;
    }

    public void save(Client client) {
        clientDao.save(client);
    }

    public void softDelete(String clientId) {
        logger.info("Soft Deleting client: {}", clientId);
        Client client = this.clientDao.findByClientId(clientId);
        client.setSoftDeleted(true);
        this.clientDao.save(client);
        logger.info("Soft Deleted cilent: {}", clientId);
    }

    public void updateDefinedPermission(Permission permission) {
        clientDao.updateDefinedPermission(permission);
    }

    public void addClientGroup(ClientGroup clientGroup) {
        clientDao.addClientGroup(clientGroup);
    }

    public void addUserToClientGroup(String username, ClientGroup clientGroup) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        if (clientGroup == null
            || StringUtils.isBlank(clientGroup.getClientId())
            || StringUtils.isBlank(clientGroup.getName())) {
            throw new IllegalArgumentException(
                "clientgroup cannot be null and must have non blank clientId and name");
        }

        User user = userDao.findByUsername(username);
        if (user == null) {
            throw new NotFoundException();
        }

        ClientGroup group = clientDao.getClientGroupByClientIdAndGroupName(
            clientGroup.getClientId(), clientGroup.getName());
        if (group == null) {
            throw new NotFoundException();
        }

        clientDao.addUserToClientGroup(user, group);
    }

    public void deleteClientGroup(String clientId, String name) {
        clientDao.deleteClientGroup(clientId, name);
    }

    public ClientGroup getClientGroupByClientIdAndGroupName(String clientId,
        String name) {
        ClientGroup group = clientDao.getClientGroupByClientIdAndGroupName(
            clientId, name);
        return group;
    }

    public List<ClientGroup> getClientGroupsByClientId(String clientId) {
        List<ClientGroup> groups = clientDao
            .getClientGroupsByClientId(clientId);
        return groups;
    }

    public void removeUserFromClientGroup(String username,
        ClientGroup clientGroup) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("username cannot be blank");
        }

        if (clientGroup == null
            || StringUtils.isBlank(clientGroup.getClientId())
            || StringUtils.isBlank(clientGroup.getName())) {
            throw new IllegalArgumentException(
                "clientgroup cannot be null and must have non blank clientId and name");
        }

        User user = userDao.findByUsername(username);
        if (user == null) {
            throw new NotFoundException();
        }

        ClientGroup group = clientDao.getClientGroupByClientIdAndGroupName(
            clientGroup.getClientId(), clientGroup.getName());
        if (group == null) {
            throw new NotFoundException();
        }

        clientDao.removeUserFromGroup(user, group);
    }
    
    public List<ClientGroup> getClientGroupsForUser(String username) {
        logger.info("Getting Groups for User: {}", username);
        String[] groupIds = userDao.getGroupIdsForUser(username);

        if (groupIds == null) {
            return null;
        }

        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        for (String groupId : groupIds) {
            groups.add(clientDao.findClientGroupByUniqueId(groupId));
        }

        logger.info("Got Groups for User: {} - {}", username, groups);
        return groups;
    }
}
