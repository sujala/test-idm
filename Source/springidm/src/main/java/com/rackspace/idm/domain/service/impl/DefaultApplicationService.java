package com.rackspace.idm.domain.service.impl;

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

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DefaultApplicationService implements ApplicationService {

    private final ScopeAccessDao scopeAccessDao;
    private final ApplicationDao clientDao;
    private final CustomerDao customerDao;
    private final UserDao userDao;
    private final TenantDao tenantDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultApplicationService(ScopeAccessDao scopeAccessDao,
        ApplicationDao clientDao, CustomerDao customerDao, UserDao userDao, TenantDao tenantDao) {
        this.clientDao = clientDao;
        this.customerDao = customerDao;
        this.userDao = userDao;
        this.tenantDao = tenantDao;
        this.scopeAccessDao = scopeAccessDao;
    }

    @Override
    public void add(Application client) {
        logger.debug("Adding Client: {}", client);

        Application existingApplication = clientDao.getClientByClientname(client.getName());
        if (existingApplication != null) {
            logger.warn("Couldn't add client {} because clientname already taken", client);
            throw new DuplicateException(String.format("Clientname %s already exists", client.getName()));
        }

        try {
            client.setClientId(HashHelper.makeSHA1Hash(client.getName()));
            client.setClientSecretObj(ClientSecret.newInstance(HashHelper.getRandomSha1()));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unsupported hashing algorithm - {}", e);
            throw new IllegalStateException("Unsupported hashing algorithm", e);
        }

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

        List<ClientGroup> groups = clientDao.getClientGroupsByClientId(clientId);

        for (ClientGroup group : groups) {
            clientDao.deleteClientGroup(group);
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

        permission = this.scopeAccessDao.definePermission(sa.getUniqueId(), permission);
        logger.debug("Defined Permission: {}", permission);
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
    public void addClientGroup(ClientGroup clientGroup) {
        logger.debug("Adding Client group: {}", clientGroup);
        Application client = clientDao.getClientByClientId(clientGroup.getClientId());

        if (client == null) {
            logger.warn("Couldn't add group {} because clientId doesn't exist", clientGroup.getClientId());
            throw new NotFoundException("Client doesn't exist");
        }

        Customer customer = customerDao.getCustomerByCustomerId(clientGroup.getCustomerId());

        if (customer == null) {
            logger.warn("Could not add group {} because customer {} not found", clientGroup.getName(), clientGroup.getCustomerId());
            throw new NotFoundException();
        }

        clientDao.addClientGroup(clientGroup, client.getUniqueId());
        logger.debug("Added Client group: {}", clientGroup);
    }

    @Override
    public void addUserToClientGroup(String username, String customerId,
        String clientId, String groupName) {
        logger.debug("Adding User: {} to Client group: {}", username, groupName);
        ClientGroup group = this.getClientGroup(customerId, clientId, groupName);

        if (group == null) {
            String errMsg = String.format("ClientGroup with Name %s, ClientId %s, and CustomerId %s not found.", groupName, clientId, customerId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        this.addUserToClientGroup(username, group);
        logger.debug("Added User: {} to Client group: {}", username, groupName);
    }

    @Override
    public void deleteClientGroup(String customerId, String clientId,
        String groupName) {
        logger.debug("Deleting Client Group: {} , Client: {}", groupName,
            clientId);
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(groupName)) {
            throw new IllegalArgumentException();
        }

        ClientGroup groupToDelete = clientDao.getClientGroup(customerId,
            clientId, groupName);

        if (groupToDelete == null) {
            throw new NotFoundException(
                String.format("Client Group with Name %s, ClientId %s, and CustomerId %s not found",
                        customerId, clientId, groupName));
        }

        clientDao.deleteClientGroup(groupToDelete);
        logger.debug("Deleted Client Group: {} from Client: {}", groupName, clientId);

        logger.debug("Deleting Users from Client Group: {} , Client: {}", groupName, clientId);
        userDao.removeUsersFromClientGroup(groupToDelete);
        logger.debug("Deleted Users from Client Group: {} , Client: {}", groupName, clientId);
    }

    @Override
    public ClientGroup getClientGroup(String customerId, String clientId, String groupName) {
        ClientGroup group = clientDao.getClientGroup(customerId, clientId, groupName);
        return group;
    }

    @Override
    public List<ClientGroup> getClientGroupsByClientId(String clientId) {
        logger.debug("Finding Client Groups by ClientID: {}", clientId);
        List<ClientGroup> groups = clientDao.getClientGroupsByClientId(clientId);
        logger.debug("Found {} Client Groups by ClientID: {}", groups.size(), clientId);
        return groups;
    }

    @Override
    public void removeUserFromClientGroup(String username, ClientGroup clientGroup) {
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

        Customer customer = customerDao.getCustomerByCustomerId(clientGroup.getCustomerId());

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
                if (filterByClient && !group.getClientId().equalsIgnoreCase(clientId)) {
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

        logger.debug("Got {} Group(s) for User: {} - {}", new Object[]{groups.size(), username, clientId});
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

        ClientGroup foundGroup = this.clientDao.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        if (foundGroup == null) {
            String errMsg = String.format("ClientGroup %s not found", group);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        boolean isMember = this.clientDao.isUserInClientGroup(username, foundGroup.getUniqueId());

        logger.debug(String.format("Is user %s member of %s - %s", username, group, isMember));

        return isMember;
    }

    @Override
    public void updateClientGroup(ClientGroup group) {
        clientDao.updateClientGroup(group);
    }

    @Override
    public DefinedPermission checkAndGetPermission(String customerId, String clientId, String permissionId) throws NotFoundException {
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

    void addUserToClientGroup(String username, ClientGroup clientGroup) {
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
}
