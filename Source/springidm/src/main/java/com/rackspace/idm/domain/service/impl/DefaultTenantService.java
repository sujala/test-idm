package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class DefaultTenantService implements TenantService {

    private final TenantDao tenantDao;
    private final ApplicationDao clientDao;
    private final UserDao userDao;
    private final ScopeAccessDao scopeAccessDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultTenantService(TenantDao tenantDao, ApplicationDao clientDao,
        UserDao userDao, ScopeAccessDao scopeAccessDao) {
        this.tenantDao = tenantDao;
        this.clientDao = clientDao;
        this.userDao = userDao;
        this.scopeAccessDao = scopeAccessDao;
    }

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
        Tenant exists = this.tenantDao.getTenant(tenant.getName());
        if (exists != null) {
            String errMsg = String.format("Tenant with name %s already exists",
                tenant.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        this.tenantDao.addTenant(tenant);
        logger.info("Added Tenant {}", tenant);
    }

    @Override
    public void deleteTenant(String tenantId) {
        logger.info("Deleting Tenant {}", tenantId);
        
        // Delete all tenant roles for this tenant
        List<TenantRole> roles = this.tenantDao.getAllTenantRolesForTenant(tenantId);
        for (TenantRole role : roles) {
            if (role.getTenantIds().length == 1) {
                this.tenantDao.deleteTenantRole(role);
            } else {
                role.removeTenantId(tenantId);
                this.tenantDao.updateTenantRole(role);
            }
        }
        
        // Then delete the tenant
        this.tenantDao.deleteTenant(tenantId);
        
        logger.info("Added Tenant {}", tenantId);
    }

    @Override
    public Tenant getTenant(String tenantId) {
        logger.info("Getting Tenant {}", tenantId);
        Tenant tenant = this.tenantDao.getTenant(tenantId);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public Tenant getTenantByName(String name) {
        logger.info("Getting Tenant {}", name);
        Tenant tenant = this.tenantDao.getTenantByName(name);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public List<Tenant> getTenants() {
        logger.info("Getting Tenants");
        List<Tenant> tenants = this.tenantDao.getTenants();
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        logger.info("Updating Tenant {}", tenant);
        this.tenantDao.updateTenant(tenant);
        logger.info("Updated Tenant {}", tenant);
    }

    @Override
    public List<Tenant> getTenantsForParentByTenantRoles(String parentUniqueId) {
        logger.info("Getting Tenants for Parent");
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<String> tenantIds = new ArrayList<String>();
        List<TenantRole> tenantRoles = this.tenantDao.getTenantRolesByParent(parentUniqueId);
        for (TenantRole role : tenantRoles) {
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    if (!tenantIds.contains(tenantId)) {
                        tenantIds.add(tenantId);
                    }
                }
            }
        }
        for (String tenantId : tenantIds) {
            Tenant tenant = this.getTenant(tenantId);
            if (tenant != null && tenant.isEnabled()) {
                tenants.add(tenant);
            }
        }
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForScopeAccessByTenantRoles(ScopeAccess sa) {
        logger.info("Getting Tenants for ScopeAccess");

        String dn = null;
        try {
            dn = sa.getLDAPEntry().getParentDN().getParent().getParentString();
        } catch (Exception ex) {
            throw new IllegalStateException();
        }
        return getTenantsForParentByTenantRoles(dn);
    }

    @Override
    public void addTenantRole(String parentUniqueId, TenantRole role) {
        // Adding a tenantRole has multiple paths depending on whether
        // the user already has that role on not.
        logger.info("Adding Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao.getTenantRoleForParentById(parentUniqueId, role.getRoleRsId());
        if (existingRole == null) {
            // if the user does not have the role then just add the
            // tenant role normally.
            this.tenantDao.addTenantRoleToParent(parentUniqueId, role);
        } else if (existingRole.getTenantIds() == null || existingRole.getTenantIds().length == 0) {
            // If the user already has the global role then do nothing
        } else {
            // If the new role is not global then add the new tenant
            // to the role and update the role, otherwise just update
            // the role and it will delete the existing tenants and
            // make it a global role.
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    for(String existingId : existingRole.getTenantIds()){
                        if(existingId.equals(tenantId))  //If role is existing then throw error
                            throw new  ClientConflictException("Tenant Role already exists");
                    }
                    existingRole.addTenantId(tenantId);
                }
            } else {
                existingRole.setTenantIds(null);
            }
            this.tenantDao.updateTenantRole(existingRole);
        }
        logger.info("Added Tenant Role {}", role);
    }

    @Override
    public void deleteTenantRole(String parentUniqueId, TenantRole role) {
        logger.info("Deleted Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao.getTenantRoleForParentById(
            parentUniqueId, role.getRoleRsId());

        if (existingRole == null) {
            throw new NotFoundException("Tenant Role not found");
        }

        if (role.getTenantIds() == null || role.getTenantIds().length == 0) {
            this.tenantDao.deleteTenantRole(role);
        } else if (existingRole.containsTenantId(role.getTenantIds()[0])) {
            if (existingRole.getTenantIds().length == 1) {
                this.tenantDao.deleteTenantRole(existingRole);
            } else {
                existingRole.removeTenantId(role.getTenantIds()[0]);
                this.tenantDao.updateTenantRole(existingRole);
            }
        }
        logger.info("Deleted Tenant Role {}", role);
    }

    @Override
    public void deleteGlobalRole(TenantRole role) {
        logger.info("Deleting Global Role {}", role);
        this.tenantDao.deleteTenantRole(role);
        logger.info("Deleted Global Role {}", role);
    }

    @Override
    public TenantRole getTenantRoleForParentById(String parentUniqueId, String id) {
        logger.debug("Getting Tenant Role {}", id);
        TenantRole role = this.tenantDao.getTenantRoleForParentById(parentUniqueId, id);
        if (role != null) {
            ClientRole cRole = this.clientDao.getClientRoleById(role.getRoleRsId());
            role.setName(cRole.getName());
            role.setDescription(cRole.getDescription());
        }
        logger.debug("Got Tenant Role {}", role);
        return role;
    }

    @Override
    public List<TenantRole> getTenantRolesByParent(String parentUniqueId) {
        logger.debug("Getting Tenant Roles");
        List<TenantRole> roles = this.tenantDao
            .getTenantRolesByParent(parentUniqueId);
        for (TenantRole role : roles) {
            if (role != null) {
                ClientRole cRole = this.clientDao.getClientRoleById(role.getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
            }
        }
        logger.debug("Got {} Tenant Roles", roles.size());
        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess) {
        logger.debug("Getting Tenant Roles");

        String parentDn = null;
        try {
            if (scopeAccess instanceof DelegatedClientScopeAccess) {
                parentDn = scopeAccess.getUniqueId();
            } else {
                parentDn = scopeAccess.getLDAPEntry().getParentDN().getParentString();
            }
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        List<TenantRole> roles = this.tenantDao.getTenantRolesByParent(parentDn);
        for (TenantRole role : roles) {
            if (role != null) {
                ClientRole cRole = this.clientDao.getClientRoleById(role.getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
            }
        }
        logger.debug("Got {} Tenant Roles", roles.size());
        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesByParentAndClientId(
        String parentUniqueId, String clientId) {
        logger.debug("Getting Tenant Roles by ClientId: {}", clientId);
        List<TenantRole> roles = this.tenantDao
            .getTenantRolesByParentAndClientId(parentUniqueId, clientId);
        for (TenantRole role : roles) {
            if (role != null) {
                ClientRole cRole = this.clientDao.getClientRoleById(role
                    .getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
            }
        }
        logger.debug("Got {} Tenant Roles", roles.size());
        return roles;
    }

    @Override
    public void addTenantRoleToUser(User user, TenantRole role) {
        if (user == null || StringUtils.isBlank(user.getUniqueId())) {
            throw new IllegalArgumentException(
                "User cannont be null and must have uniqueID");
        }

        Application client = this.clientDao.getClientByClientId(role.getClientId());
        if (client == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.clientDao.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        UserScopeAccess sa = (UserScopeAccess) this.scopeAccessDao.getDirectScopeAccessForParentByClientId(user.getUniqueId(), role.getClientId());

        if (sa == null) {
            sa = new UserScopeAccess();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getRCN());
            sa.setUsername(user.getUsername());
            sa.setUserRCN(user.getCustomerId());
            sa.setUserRsId(user.getId());
            sa = (UserScopeAccess) this.scopeAccessDao.addDirectScopeAccess(user.getUniqueId(), sa);
        }

        addTenantRole(sa.getUniqueId(), role);

        logger.info("Adding tenantRole {} to user {}", role, user);
    }

    @Override
    public void addTenantRoleToClient(Application client, TenantRole role) {
        if (client == null || StringUtils.isBlank(client.getUniqueId())) {
            throw new IllegalArgumentException(
                "Client cannont be null and must have uniqueID");
        }

        Application owner = this.clientDao.getClientByClientId(role.getClientId());
        if (owner == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.clientDao.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccess sa = this.scopeAccessDao.getDirectScopeAccessForParentByClientId(client.getUniqueId(), role.getClientId());

        if (sa == null) {
            sa = new ScopeAccess();
            sa.setClientId(owner.getClientId());
            sa.setClientRCN(owner.getRCN());
            sa = this.scopeAccessDao.addDirectScopeAccess(client.getUniqueId(), sa);
        }

        addTenantRole(sa.getUniqueId(), role);

        logger.info("Added tenantRole {} to client {}", role, client);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(User user) {
        logger.debug("Getting Global Roles for user {}", user.getUsername());
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user);

        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(User user,
        FilterParam[] filters) {
        logger.debug("Getting Global Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user, filters);

        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getGlobalRolesForApplication(
        Application application, FilterParam[] filters) {
        logger.debug("Getting Global Roles for application {}",
            application.getName());
        List<TenantRole> roles = this.tenantDao.getTenantRolesForApplication(
            application, filters);

        return getGlobalRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForUserOnTenant(User user,
        Tenant tenant) {
        logger.debug("Getting Tenant Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user);
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role.containsTenantId(tenant.getTenantId())) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.setTenantIds(new String[]{tenant.getTenantId()});
                tenantRoles.add(newRole);
            }
        }
        logger.debug("Got {} Tenant Roles", roles.size());
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(User user,
        FilterParam[] filters) {
        logger.debug("Getting Tenant Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesForUser(user,
            filters);

        return getTenantOnlyRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForApplication(
        Application application, FilterParam[] filters) {
        logger.debug("Getting Tenant Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesForApplication(
            application, filters);

        return getTenantOnlyRoles(roles);
    }

    @Override
    public List<TenantRole> getTenantRolesForTenant(String tenantId) {

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenant(tenantId);

        List<String> roleIds = new ArrayList<String>();
        for (TenantRole role : roles) {
            if (!roleIds.contains(role.getRoleRsId())) {
                roleIds.add(role.getRoleRsId());
            }
        }

        List<TenantRole> returnedRoles = new ArrayList<TenantRole>();
        for (String roleId : roleIds) {
            ClientRole cRole = this.clientDao.getClientRoleById(roleId);
            if (cRole != null) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(cRole.getClientId());
                newRole.setDescription(cRole.getDescription());
                newRole.setName(cRole.getName());
                newRole.setRoleRsId(cRole.getId());
                newRole.setTenantIds(new String[]{tenantId});
                returnedRoles.add(newRole);
            }
        }
        return returnedRoles;
    }

    @Override
    public List<User> getUsersForTenant(String tenantId) {
        logger.debug("Getting Users for Tenant {}", tenantId);
        List<User> users = new ArrayList<User>();

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenant(tenantId);

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : roles) {
            if (!userIds.contains(role.getUserId())) {
                userIds.add(role.getUserId());
            }
        }

        for (String userId : userIds) {
            User user = this.userDao.getUserById(userId);
            if (user != null && user.isEnabled()) {
                users.add(user);
            }
        }

        logger.debug("Got {} Users for Tenant {}", users.size(), tenantId);
        return users;
    }

    /**
     * get roles in this list that are non-tenant specific
     * @param roles
     */
    private List<TenantRole> getGlobalRoles(List<TenantRole> roles) {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null
                && (role.getTenantIds() == null || role.getTenantIds().length == 0)) {
                ClientRole cRole = this.clientDao.getClientRoleById(role
                    .getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                globalRoles.add(role);
            }
        }
        return globalRoles;
    }

    /**
     * get roles in this list that are tenant specific
     * @param roles
     */
    private List<TenantRole> getTenantOnlyRoles(List<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            // we only want to include roles on a tenant, and not global roles
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.setTenantIds(role.getTenantIds());
                tenantRoles.add(newRole);
            }
        }

        return tenantRoles;
    }

    @Override
    public List<User> getUsersWithTenantRole(Tenant tenant, ClientRole cRole) {
        List<User> users = new ArrayList<User>();

        List<TenantRole> roles = this.tenantDao
            .getAllTenantRolesForTenantAndRole(tenant.getTenantId(),
                cRole.getId());

        List<String> userIds = new ArrayList<String>();

        for (TenantRole role : roles) {
            if (!userIds.contains(role.getUserId())) {
                userIds.add(role.getUserId());
            }
        }

        for (String userId : userIds) {
            User user = this.userDao.getUserById(userId);
            if (user != null && user.isEnabled()) {
                users.add(user);
            }
        }

        logger.debug("Got {} Users for Tenant {}", users.size(),
            tenant.getTenantId());
        return users;

    }
}
