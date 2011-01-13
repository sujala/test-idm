package com.rackspace.idm.services;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.entities.Client;
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
    private Logger logger;

    public DefaultClientService(ClientDao clientDao, CustomerDao customerDao,
        Logger logger) {
        this.clientDao = clientDao;
        this.customerDao = customerDao;
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

    public void addDefinedPermission(Permission permission) {
        
        Customer customer = customerDao
            .findByCustomerId(permission.getCustomerId());

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

        Permission exists = clientDao.getDefinedPermissionByClientIdAndPermissionId(permission.getClientId(), permission.getPermissionId());
        
        if (exists != null) {
            logger.warn(
                "Couldn't add permission {} because permissionId already taken",
                client);
            throw new DuplicateException(String.format(
                "PermissionId %s already exists", client.getName()));
        }

        clientDao.addDefinedPermission(permission);
    }

    public boolean authenticate(String clientId, String clientSecret) {
        return clientDao.authenticate(clientId, clientSecret);
    }

    public void delete(String clientId) {
        clientDao.delete(clientId);
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

    public Permission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId) {
        Permission permission = clientDao.getDefinedPermissionByClientIdAndPermissionId(clientId, permissionId);
        return permission;
    }

    public List<Permission> getDefinedPermissionsByClientId(String clientId) {
        List<Permission> permissions = clientDao.getDefinedPermissionsByClientId(clientId);
        return permissions;
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
}
