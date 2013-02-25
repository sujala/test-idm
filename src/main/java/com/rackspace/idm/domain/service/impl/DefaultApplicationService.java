package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.HashHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultApplicationService implements ApplicationService {

    @Autowired
    private ApplicationDao applicationDao;
    @Autowired
    private ApplicationRoleDao applicationRoleDao;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private TenantService tenantService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void add(Application client) {
        logger.debug("Adding Client: {}", client);

        Application existingApplication = applicationDao.getClientByClientname(client.getName());
        if (existingApplication != null) {
            logger.warn("Couldn't add client {} because clientname already taken", client);
            throw new DuplicateException(String.format("Clientname %s already exists", client.getName()));
        }

        client.setClientId(HashHelper.makeSHA1Hash(client.getName()));
        client.setClientSecretObj(ClientSecret.newInstance(HashHelper.getRandomSha1()));

        applicationDao.addClient(client);
        logger.debug("Added Client: {}", client);
    }

    @Override
    public void delete(String clientId) {
        logger.debug("Delete Client: {}", clientId);
        Application client = applicationDao.getClientByClientId(clientId);

        if (client == null) {
            String errMsg = String.format("Client with clientId %s not found.", clientId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        List<DefinedPermission> definedPermissions = this.getDefinedPermissionsByClient(client);

        for (DefinedPermission definedPerm : definedPermissions) {
            this.deleteDefinedPermission(definedPerm);
        }
        
        List<ClientRole> roles = applicationDao.getClientRolesByClientId(clientId);
        
        for (ClientRole role : roles) {
            this.deleteClientRole(role);
        }

        applicationDao.deleteClient(client);
        logger.debug("Deleted Client: {}", clientId);
    }

    @Override
    public void addDefinedPermission(DefinedPermission permission) {
        logger.debug("Define Permission: {}", permission);
        Customer customer = customerService.getCustomer(permission.getCustomerId());

        if (customer == null) {
            logger.warn("Couldn't add permission {} because customerId doesn't exist", permission.getCustomerId());
            throw new IllegalStateException("Customer doesn't exist");
        }

        Application client = applicationDao.getClientByClientId(permission.getClientId());

        if (client == null) {
            logger.warn("Couldn't add permission {} because clientId doesn't exist", permission.getClientId());
            throw new IllegalStateException("Client doesn't exist");
        }

        ScopeAccess sa = scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());
        if (sa == null) {
            sa = new ClientScopeAccess();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getRCN());
            sa = scopeAccessService.addDirectScopeAccess(client.getUniqueId(), sa);
        }

        DefinedPermission exists = (DefinedPermission) scopeAccessService.getPermissionForParent(sa.getUniqueId(), permission);

        if (exists != null) {
            logger.warn("Couldn't add permission {} because permissionId already taken", client);
            throw new DuplicateException(String.format("PermissionId %s already exists", client.getName()));
        }

        logger.debug("Defined Permission: {}", scopeAccessService.definePermission(sa.getUniqueId(), permission));
    }

    @Override
    public void deleteDefinedPermission(DefinedPermission definedPermission) {
        logger.debug("Delete Permission: {}", definedPermission);
        Permission permission = new Permission();
        permission.setClientId(definedPermission.getClientId());
        permission.setCustomerId(definedPermission.getCustomerId());
        permission.setPermissionId(definedPermission.getPermissionId());

        List<Permission> permissions = scopeAccessService.getPermissionsByPermission(definedPermission);

        for (Permission perm : permissions) {
            logger.debug("Deleting Permission: {}", perm);
            scopeAccessService.removePermission(perm);
        }
        logger.debug("Deleted Permission: {}", definedPermission);
    }

    @Override
    public Application loadApplication(String applicationId) {
        Application client = applicationDao.getClientByClientId(applicationId);
        if (client == null) {
            String errMsg = String.format("Client %s not found", applicationId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return client;
    }

    @Override
    public Applications getAllApplications(List<FilterParam> filters, int offset, int limit) {
        return applicationDao.getAllClients(filters, offset, limit);
    }

    @Override
    public Applications getByCustomerId(String customerId, int offset, int limit) {
        return applicationDao.getClientsByCustomerId(customerId, offset, limit);
    }

    @Override
    public Application getById(String clientId) {
        return applicationDao.getClientByClientId(clientId);
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
        return applicationDao.getClientByCustomerIdAndClientId(customerId, clientId);
    }

    @Override
    public Application getByName(String clientName) {
        return applicationDao.getClientByClientname(clientName);
    }

    @Override
    public Application getClientByScope(String scope) {
        return applicationDao.getClientByScope(scope);
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

        permission = scopeAccessService.getPermissionForParent(client.getUniqueId(), permission);

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

        List<Permission> permissions = scopeAccessService.getPermissionsForParentByPermission(client.getUniqueId(), filter);

        List<DefinedPermission> perms = new ArrayList<DefinedPermission>();
        for (Permission p : permissions) {
            perms.add((DefinedPermission) p);
        }

        logger.debug("Found {} Permission(s) by ClientId: {}", permissions.size(), client.getClientId());
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
        applicationDao.updateClient(client);
        logger.debug("Reset Client secret ClientId: {}", client.getClientId());
        return clientSecret;
    }

    @Override
    public void save(Application client) {
        applicationDao.updateClient(client);
    }

    @Override
    public List<Application> getAvailableScopes() {
        List<Application> clientList = applicationDao.getAvailableScopes();

        if (clientList == null) {
            String errorMsg = String.format("No defined scope accesses found for this application.");
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        return clientList;
    }

    private Application getClient(String clientId) {
        Application targetClient = this.applicationDao.getClientByClientId(clientId);
        if (targetClient == null) {
            String errorMsg = String.format("Client Not Found: %s", clientId);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        return targetClient;
    }

    @Override
    public void updateDefinedPermission(DefinedPermission permission) {
        scopeAccessService.updatePermission(permission);
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
            throw new IllegalArgumentException("Client cannot be null and must have uniqueID");
        }
        List<ScopeAccess> services = scopeAccessService.getScopeAccessesForParent(client.getUniqueId());

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
        this.applicationDao.updateClient(client);
        logger.info("Updated Client: {}", client);
    }

    @Override
    public void addClientRole(ClientRole role) {
        addClientRole(role, applicationRoleDao.getNextRoleId());
    }

    @Override
    public void addClientRole(ClientRole role, String roleId) {
        logger.info("Adding Client Role: {}", role);
        Application application = applicationDao.getClientByClientId(role.getClientId());
        if (application == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        ClientRole exists = applicationRoleDao.getClientRoleByApplicationAndName(application, role);
        if (exists != null) {
            String errMsg = String.format("Role with name %s already exists", role.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        role.setId(roleId);
        applicationRoleDao.addClientRole(application, role);
        logger.info("Added Client Role: {}", role);
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        logger.info("Delete Client Role: {}", role);
        
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForClientRole(role);
        for (TenantRole tenantRole : tenantRoles) {
            tenantService.deleteTenantRole(tenantRole);
        }
        
        this.applicationRoleDao.deleteClientRole(role);
        logger.info("Deleted Client Role: {}", role);
    }

    @Override
    public void updateClientRole(ClientRole role) {
        logger.info("Update Client Role: {}", role);
        this.applicationRoleDao.updateClientRole(role);
        logger.info("Udpated Client Role: {}", role);
    }

    @Override
    public List<ClientRole> getClientRolesByClientId(String clientId) {
        logger.debug("Getting Client Roles for client: {}", clientId);
        Application application = applicationDao.getClientByClientId(clientId);
        if (application == null) {
            throw new NotFoundException(String.format("Client with id %s does not exit", clientId));
        }
        List<ClientRole> roles = this.applicationRoleDao.getClientRolesForApplication(application);
        logger.debug("Got {} Client Roles", roles.size());
        return roles;
    }

    @Override
    public PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int offset, int limit) {
        logger.debug("Getting all Client Roles page: {}");
        PaginatorContext<ClientRole> context = applicationRoleDao.getClientRolesPaged(applicationId, roleName, offset, limit);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable) {
        logger.debug("Getting all Client Roles page: {}");
        PaginatorContext<ClientRole> context = applicationRoleDao.getAvailableClientRolesPaged(applicationId, offset, limit, maxWeightAvailable);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable) {
        logger.debug("Getting all Client Roles page: {}");
        PaginatorContext<ClientRole> context = applicationRoleDao.getAvailableClientRolesPaged(offset, limit, maxWeightAvailable);
        logger.debug("Got {} Client Roles", context.getTotalRecords());
        return context;
    }
    
    @Override
    public List<ClientRole> getAllClientRoles() {
        logger.debug("Getting Client Roles");
        List<ClientRole> roles = this.applicationRoleDao.getAllClientRoles();
        logger.debug("Got {} Client Roles", roles.size());
        return roles;
    }

    @Override
    public ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName) {
        logger.debug("Getting Client Role {} for client {}", roleName, clientId);
        ClientRole role = this.applicationRoleDao.getClientRoleByApplicationAndName(clientId, roleName);
        logger.debug("Got Client Role {} for client {}", roleName, clientId);
        return role;
    }

    @Override
    public ClientRole getClientRoleById(String roleId) {
        logger.debug("Getting Client Role {}", roleId);
        ClientRole role = this.applicationRoleDao.getClientRole(roleId);
        logger.debug("Got Client Role {}", roleId);
        return role;
    }

    @Override
    public List<Application> getOpenStackServices() {
        logger.debug("Getting Open Stack Services");
        List<Application> clients = this.applicationDao.getOpenStackServices();
        logger.debug("Got {} Open Stack Services", clients.size());
        return clients;
    }
    
    @Override
    public void softDeleteApplication(Application application) {
        logger.debug("SoftDeleting Application: {}", application);
        applicationDao.softDeleteApplication(application);
        logger.debug("SoftDeleted Application: {}", application);
    }

    @Override
    public ClientRole getUserIdentityRole(User user, String applicationId, List<String> roleNames) {
        logger.debug("getting identity:* role for user: {}", user);
        Application application = applicationDao.getClientByClientId(applicationId);
        List<ClientRole> identityRoles = applicationRoleDao.getIdentityRoles(application, roleNames);

        TenantRole match = tenantService.getTenantRoleForUser(user, identityRoles);

        if (match != null) {
            for (ClientRole role : identityRoles) {
                if (role.getId().equals(match.getRoleRsId())) {
                    return role;
                }
            }
        }
        return null;
    }
}
