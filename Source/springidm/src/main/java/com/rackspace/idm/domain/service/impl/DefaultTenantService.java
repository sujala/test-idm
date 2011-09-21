package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.NotFoundException;

public class DefaultTenantService implements TenantService {

    private final TenantDao tenantDao;
    private final ClientDao clientDao;
    private final ScopeAccessDao scopeAccessDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultTenantService(TenantDao tenantDao, ClientDao clientDao,
        ScopeAccessDao scopeAccessDao) {
        this.tenantDao = tenantDao;
        this.clientDao = clientDao;
        this.scopeAccessDao = scopeAccessDao;
    }

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
        tenant.setTenantId(this.tenantDao.getNextTenantId());
        this.tenantDao.addTenant(tenant);
        logger.info("Added Tenant {}", tenant);
    }

    @Override
    public void deleteTenant(String tenantId) {
        logger.info("Deleting Tenant {}", tenantId);
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
        List<TenantRole> tenantRoles = this.tenantDao
            .getTenantRolesByParent(parentUniqueId);
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
            tenants.add(this.getTenant(tenantId));
        }
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public void addTenantRole(String parentUniqueId, TenantRole role) {
        // Adding a tenantRole has multiple paths depending on whether
        // the user already has that role on not.
        logger.info("Adding Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao
            .getTenantRoleForParentById(parentUniqueId,
                role.getId());
        if (existingRole == null) {
            // if the user does not have the role then just add the
            // tenant role normally.
            this.tenantDao.addTenantRoleToParent(parentUniqueId, role);
        } else if (existingRole.getTenantIds() == null
            || existingRole.getTenantIds().length == 0) {
            // If the user already has the global role then do nothing
        } else {
            // If the new role is not global then add the new tenant
            // to the role and update the role, otherwise just update
            // the role and it will delete the existing tenants and
            // make it a global role.
            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : existingRole.getTenantIds()) {
                    role.addTenantId(tenantId);
                }
            }
            this.tenantDao.updateTenantRole(role);
        }
        logger.info("Added Tenant Role {}", role);
    }

    @Override
    public void deleteTenantRole(String parentUniqueId, TenantRole role) {
        logger.info("Deleted Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao
            .getTenantRoleForParentById(parentUniqueId,
                role.getId());

        if (existingRole == null) {
            throw new NotFoundException("Tenant Role not found");
        }

        if (role.getTenantIds() == null || role.getTenantIds().length == 0) {
            this.tenantDao.deleteTenantRole(role);
        } else if (existingRole.containsTenantId(role.getTenantIds()[0])) {
            if (existingRole.getTenantIds().length == 1) {
                this.tenantDao.deleteTenantRole(role);
            } else {
                existingRole.removeTenantId(role.getTenantIds()[0]);
                this.tenantDao.updateTenantRole(existingRole);
            }
        }
        logger.info("Deleted Tenant Role {}", role);
    }

    @Override
    public TenantRole getTenantRoleForParentById(
        String parentUniqueId, String id) {
        logger.debug("Getting Tenant Role {}", id);
        TenantRole role = this.tenantDao
            .getTenantRoleForParentById(parentUniqueId,id);
        if (role != null) {
            ClientRole cRole = this.clientDao.getClientRoleById(role.getId());
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
                ClientRole cRole = this.clientDao.getClientRoleById(role.getId());
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
                ClientRole cRole = this.clientDao.getClientRoleById(role.getId());
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

        Client client = this.clientDao.getClientByClientId(role.getClientId());
        if (client == null) {
            String errMsg = String.format("Client %s not found",
                role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.clientDao.getClientRoleByClientIdAndRoleName(
            role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found",
                role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        UserScopeAccess sa = (UserScopeAccess) this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(user.getUniqueId(),
                role.getClientId());

        if (sa == null) {
            sa = new UserScopeAccess();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getRCN());
            sa.setUsername(user.getUsername());
            sa.setUserRCN(user.getCustomerId());
            sa = (UserScopeAccess) this.scopeAccessDao.addDirectScopeAccess(
                user.getUniqueId(), sa);
        }

        addTenantRole(sa.getUniqueId(), role);

        logger.info("Adding tenantRole {} to user {}", role, user);
    }

    @Override
    public void addTenantRoleToClient(Client client, TenantRole role) {
        if (client == null || StringUtils.isBlank(client.getUniqueId())) {
            throw new IllegalArgumentException(
                "User cannont be null and must have uniqueID");
        }

        Client owner = this.clientDao.getClientByClientId(role.getClientId());
        if (owner == null) {
            String errMsg = String.format("Client %s not found",
                role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.clientDao.getClientRoleByClientIdAndRoleName(
            role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found",
                role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ScopeAccess sa = this.scopeAccessDao
            .getDirectScopeAccessForParentByClientId(client.getUniqueId(),
                role.getClientId());

        if (sa == null) {
            sa = new ScopeAccess();
            sa.setClientId(client.getClientId());
            sa.setClientRCN(client.getRCN());
            sa = this.scopeAccessDao.addDirectScopeAccess(client.getUniqueId(),
                sa);
        }

        addTenantRole(sa.getUniqueId(), role);

        logger.info("Added tenantRole {} to client {}", role, client);
    }

    private ClientRole getClientRoleById(String id) {
        return this.clientDao.getClientRoleById(id);
    }
}
