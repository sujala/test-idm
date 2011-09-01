package com.rackspace.idm.domain.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.NotFoundException;

public class DefaultTenantService implements TenantService {

    private final TenantDao tenantDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultTenantService(TenantDao tenantDao) {
        this.tenantDao = tenantDao;
    }

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
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
    public void addTenantRole(String parentUniqueId, TenantRole role) {
        logger.info("Adding Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao
            .getTenantRoleForParentByRoleNameAndClientId(parentUniqueId,
                role.getName(), role.getClientId());
        if (existingRole == null) {
            this.tenantDao.addTenantRoleToParent(parentUniqueId, role);
        } else {
            for (String tenantId : existingRole.getTenantIds()) {
                role.addTenantId(tenantId);
            }
            this.tenantDao.updateTenantRole(role);
        }
        logger.info("Added Tenant Role {}", role);
    }

    @Override
    public void deleteTenantRole(String parentUniqueId, TenantRole role) {
        logger.info("Deleted Tenant Role {}", role);
        TenantRole existingRole = this.tenantDao
            .getTenantRoleForParentByRoleNameAndClientId(parentUniqueId,
                role.getName(), role.getClientId());

        if (existingRole == null) {
            throw new NotFoundException("Tenant Role not found");
        }

        if (existingRole.containsTenantId(role.getTenantIds()[0])) {
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
    public void updateTenantRole(TenantRole role) {
        logger.info("Updating Tenant Role {}", role);
        this.tenantDao.updateTenantRole(role);
        logger.info("Updated Tenant {}", role);
    }

    @Override
    public TenantRole getTenantRoleForParentByRoleName(String parentUniqueId,
        String roleName) {
        logger.debug("Getting Tenant Role {}", roleName);
        TenantRole role = this.tenantDao.getTenantRoleForParentByRoleName(parentUniqueId, roleName);
        logger.debug("Got Tenant Role {}", roleName);
        return role;
    }

    @Override
    public TenantRole getTenantRoleForParentByRoleNameAndClientId(
        String parentUniqueId, String roleName, String clientId) {
        logger.debug("Getting Tenant Role {}", roleName);
        TenantRole role = this.tenantDao.getTenantRoleForParentByRoleNameAndClientId(parentUniqueId, roleName, clientId);
        logger.debug("Got Tenant Role {}", roleName);
        return role;
    }

    @Override
    public List<TenantRole> getTenantRolesByParent(String parentUniqueId) {
        logger.debug("Getting Tenant Roles");
        List<TenantRole> roles = this.tenantDao.getTenantRolesByParent(parentUniqueId);
        logger.debug("Got {} Tenant Roles", roles.size());
        return roles;
    }

    @Override
    public List<TenantRole> getTenantRolesByParentAndClientId(
        String parentUniqueId, String clientId) {
        logger.debug("Getting Tenant Roles by ClientId: {}", clientId);
        List<TenantRole> roles = this.tenantDao.getTenantRolesByParentAndClientId(parentUniqueId, clientId);
        logger.debug("Got {} Tenant Roles", roles.size());
        return roles;
    }

}
