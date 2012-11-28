package com.rackspace.idm.domain.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.HashHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultApplicationService implements ApplicationService {

    @Autowired
    private ScopeAccessDao scopeAccessDao;
    @Autowired
    private ApplicationDao clientDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TenantDao tenantDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void add(Application client) {
        logger.debug("Adding Client: {}", client);

        Application existingApplication = clientDao.getClientByClientname(client.getName());
        if (existingApplication != null) {
            logger.warn("Couldn't add client {} because clientname already taken", client);
            throw new DuplicateException(String.format("Clientname %s already exists", client.getName()));
        }

        client.setClientId(HashHelper.makeSHA1Hash(client.getName()));
        client.setClientSecretObj(ClientSecret.newInstance(HashHelper.getRandomSha1()));

        clientDao.addClient(client);
        logger.debug("Added Client: {}", client);
    }

    @Override
    public void delete(String clientId) {
        logger.debug("Delete Client: {}", clientId);
        Application client = clientDao.getClientByClientId(clientId);

        if (client == null) {
            String errMsg = String.format("Client with clientId %s not found.", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        List<DefinedPermission> definedPermissions = this.getDefinedPermissionsByClient(client);

        for (DefinedPermission definedPerm : definedPermissions) {
            this.deleteDefinedPermission(definedPerm);
        }
        
        List<ClientRole> roles = clientDao.getClientRolesByClientId(clientId);
        
        for (ClientRole role : roles) {
            this.deleteClientRole(role);
        }

        clientDao.deleteClient(client);
        logger.debug("Deleted Client: {}", clientId);
    }

    @Override
    public void addDefinedPermission(DefinedPermission permission) {
        logger.debug("Define Permission: {}", permission);
        Customer customer = customerDao.getCustomerByCustomerId(permission.getCustomerId());

        if (customer == null) {
            logger.warn("Couldn't add permission {} because customerId doesn't exist", permission.getCustomerId());
            throw new IllegalStateException("Customer doesn't exist");
        }

        Application client = clientDao.getClientByClientId(permission.getClientId());

        if (client == null) {
            logger.warn("Couldn't add permission {} because clientId doesn't exist", permission.getClientId());
            throw new IllegalStateException("Client doesn't exist");
        }

        ScopeAccess sa = this.scopeAccessDao.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        if (sa == null) {
            sa = new ClientScopeAccess();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getRCN());
            sa = this.scopeAccessDao.addDirectScopeAccess(client.getUniqueId(), sa);
        }

        DefinedPermission exists = (DefinedPermission) this.scopeAccessDao.getPermissionByParentAndPermission(sa.getUniqueId(), permission);

        if (exists != null) {
            logger.warn("Couldn't add permission {} because permissionId already taken", client);
            throw new DuplicateException(String.format("PermissionId %s already exists", client.getName()));
        }

        logger.debug("Defined Permission: {}", this.scopeAccessDao.definePermission(sa.getUniqueId(), permission));
    }

    @Override
    public void deleteDefinedPermission(DefinedPermission permission) {
        logger.debug("Delete Permission: {}", permission);
        Permission perm = new Permission();
        perm.setClientId(permission.getClientId());
        perm.setCustomerId(permission.getCustomerId());
        perm.setPermissionId(permission.getPermissionId());

        List<Permission> perms = this.scopeAccessDao.getPermissionsByPermission(permission);

        for (Permission p : perms) {
            logger.debug("Deleting Permission: {}", permission);
            this.scopeAccessDao.removePermissionFromScopeAccess(p);
        }
        logger.debug("Deleted Permission: {}", permission);
    }

    @Override
    public Application loadApplication(String applicationId) {
        Application client = clientDao.getClientByClientId(applicationId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return client;
    }

    @Override
    public Applications getAllApplications(List<FilterParam> filters, int offset, int limit) {
        return clientDao.getAllClients(filters, offset, limit);
    }

    @Override
    public Applications getByCustomerId(String customerId, int offset, int limit) {
        return clientDao.getClientsByCustomerId(customerId, offset, limit);
    }

    @Override
    public Application getById(String clientId) {
        return clientDao.getClientByClientId(clientId);
    }

    @Override
    public Application checkAndGetApplication(String applicationId) {
        Application application = getById(applicationId);
        if (application == null) {
            String errMsg = String.format("Service %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return application;
    }

    @Override
    public Application getClient(String customerId, String clientId) {
        return clientDao.getClientByCustomerIdAndClientId(customerId, clientId);
    }

    @Override
    public Application getByName(String clientName) {
        return clientDao.getClientByClientname(clientName);
    }

    @Override
    public Application getClientByScope(String scope) {
        return clientDao.getClientByScope(scope);
    }

    @Override
    public DefinedPermission getDefinedPermissionByClientIdAndPermissionId(
        String clientId, String permissionId) {
        logger.debug("Find Permission: {} by ClientId: {}", permissionId, clientId);

        Application client = this.getClient(clientId);

        Permission permission = new DefinedPermission();
        permission.setPermissionId(permissionId);
        permission.setCustomerId(client.getRCN());
        permission.setClientId(client.getClientId());

        permission = this.scopeAccessDao.getPermissionByParentAndPermission(client.getUniqueId(), permission);

        DefinedPermission perm = (DefinedPermission) permission;

        logger.debug("Found Permission: {}", perm);
        return perm;
    }

    @Override
    public List<DefinedPermission> getDefinedPermissionsByClient(Application client) {
        logger.debug("Find Permission by ClientId: {}", client.getClientId());
        Permission filter = new Permission();
        filter.setClientId(client.getClientId());
        filter.setCustomerId(client.getRCN());

        List<Permission> permissions = this.scopeAccessDao.getPermissionsByParentAndPermission(client.getUniqueId(), filter);

        List<DefinedPermission> perms = new ArrayList<DefinedPermission>();
        for (Permission p : permissions) {
            perms.add((DefinedPermission) p);
        }

        logger.debug("Found {} Permission(s) by ClientId: {}",
            permissions.size(), client.getClientId());
        return perms;
    }

    @Override
    public ClientSecret resetClientSecret(Application client) {
        if (client == null) {
            throw new IllegalArgumentException();
        }
        logger.debug("Reseting Client secret ClientId: {}", client.getClientId());

        ClientSecret clientSecret = null;
        clientSecret = ClientSecret.newInstance(HashHelper.getRandomSha1());
        client.setClientSecretObj(clientSecret);
        clientDao.updateClient(client);
        logger.debug("Reset Client secret ClientId: {}", client.getClientId());
        return clientSecret;
    }

    @Override
    public void save(Application client) {
        clientDao.updateClient(client);
    }

    @Override
    public List<Application> getAvailableScopes() {
        List<Application> clientList = this.clientDao.getAvailableScopes();

        if (clientList == null) {
            String errorMsg = String.format("No defined scope accesses found for this application.");
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return clientList;
    }

    private Application getClient(String clientId) {
        Application targetClient = this.clientDao.getClientByClientId(clientId);
        if (targetClient == null) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return targetClient;
    }

    @Override
    public void updateDefinedPermission(DefinedPermission permission) {
        this.scopeAccessDao.updatePermissionForScopeAccess(permission);
    }

    @Override
    public DefinedPermission checkAndGetPermission(String customerId, String clientId, String permissionId) {
        logger.debug("Check and get Permission: {} for ClientId: {}", permissionId, clientId);
        DefinedPermission permission = this.getDefinedPermissionByClientIdAndPermissionId(clientId, permissionId);

        if (permission == null
            || !customerId.equalsIgnoreCase(permission.getCustomerId())
            || !clientId.equalsIgnoreCase(permission.getClientId())
            || !permission.getEnabled()) {
            String errorMsg = String.format("Permission Not Found: %s", permissionId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        logger.debug("Found Permission: {} for ClientId: {}", permission, clientId);
        return permission;
    }

    @Override
    public Applications getClientServices(Application client) {
        logger.debug("Finding Client Services for Client: {}", client.getClientId());
        if (client == null || client.getUniqueId() == null) {
            throw new IllegalArgumentException("Client cannont be null and must have uniqueID");
        }
        List<ScopeAccess> services = this.scopeAccessDao.getScopeAccessesByParent(client.getUniqueId());

        List<Application> clientList = new ArrayList<Application>();

        for (ScopeAccess service : services) {
            if (service instanceof ScopeAccess) {
                clientList.add(this.getById(service.getClientId()));
            }
        }

        Applications clients = new Applications();
        clients.setClients(clientList);
        clients.setOffset(0);
        clients.setLimit(clientList.size());
        clients.setTotalRecords(clientList.size());

        logger.debug("Found {} Client Service(s) for Client: {}", clientList.size(), client.getClientId());
        return clients;
    }

    @Override
    public void updateClient(Application client) {
        logger.info("Updating Client: {}", client);
        this.clientDao.updateClient(client);
        logger.info("Updated Client: {}", client);
    }

    @Override
    public void addClientRole(ClientRole role) {
        addClientRole(role, clientDao.getNextRoleId());
    }

    @Override
    public void addClientRole(ClientRole role, String roleId) {
        logger.info("Adding Client Role: {}", role);
        Application client = clientDao.getClientByClientId(role.getClientId());
        if (client == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        ClientRole exists = clientDao.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (exists != null) {
            String errMsg = String.format("Role with name %s already exists", role.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        role.setId(roleId);
        clientDao.addClientRole(client.getUniqueId(), role);
        logger.info("Added Client Role: {}", role);
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        logger.info("Delete Client Role: {}", role);
        
        List<TenantRole> tenantRoles = this.tenantDao.getAllTenantRolesForClientRole(role);
        for (TenantRole tenantRole : tenantRoles) {
            this.tenantDao.deleteTenantRole(tenantRole);
        }
        
        this.clientDao.deleteClientRole(role);
        logger.info("Deleted Client Role: {}", role);
    }

    @Override
    public void updateClientRole(ClientRole role) {
        logger.info("Update Client Role: {}", role);
        this.clientDao.updateClientRole(role);
        logger.info("Udpated Client Role: {}", role);
    }

    @Override
    public List<ClientRole> getClientRolesByClientId(String clientId) {
        logger.debug("Getting Client Roles for client: {}", clientId);
        List<ClientRole> roles = this.clientDao.getClientRolesByClientId(clientId);
        logger.debug("Got {} Client Roles", roles.size());
        return roles;
    }
    
    @Override
    public List<ClientRole> getAllClientRoles(List<FilterParam> filters) {
        logger.debug("Getting Client Roles");
        List<ClientRole> roles = this.clientDao.getAllClientRoles(filters);
        logger.debug("Got {} Client Roles", roles.size());
        return roles;
    }

    @Override
    public ClientRole getClientRoleByClientIdAndRoleName(String clientId,
        String roleName) {
        logger.debug("Getting Client Role {} for client {}", roleName, clientId);
        ClientRole role = this.clientDao.getClientRoleByClientIdAndRoleName(clientId, roleName);
        logger.debug("Got Client Role {} for client {}", roleName, clientId);
        return role;
    }

    @Override
    public ClientRole getClientRoleById(String id) {
        logger.debug("Getting Client Role {}", id);
        ClientRole role = this.clientDao.getClientRoleById(id);
        logger.debug("Got Client Role {}", id);
        return role;
    }

    @Override
    public List<Application> getOpenStackServices() {
        logger.debug("Getting Open Stack Services");
        List<Application> clients = this.clientDao.getOpenStackServices();
        logger.debug("Got {} Open Stack Services", clients.size());
        return clients;
    }
    
    @Override
    public void softDeleteApplication(Application application) {
        logger.debug("SoftDeleting Application: {}", application);
        clientDao.softDeleteApplication(application);
        logger.debug("SoftDeleted Application: {}", application);
    }

	@Override
	public void setScopeAccessDao(ScopeAccessDao scopeAccessDao) {
		this.scopeAccessDao = scopeAccessDao;
	}

	@Override
	public void setApplicationDao(ApplicationDao applicationDao) {
		this.clientDao = applicationDao;
	}

	@Override
	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
		
	}

	@Override
	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	@Override
	public void setTenantDao(TenantDao tenantDao) {
		this.tenantDao = tenantDao;
	}
}
