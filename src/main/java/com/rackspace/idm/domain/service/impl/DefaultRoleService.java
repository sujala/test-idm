package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.RoleService;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DefaultRoleService implements RoleService {

    @Autowired
    ApplicationRoleDao applicationRoleDao;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    Configuration config;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    TenantRoleDao tenantRoleDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ClientRole getRoleByName(String roleName) {
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getSuperUserAdminRole() {
        return applicationRoleDao.getRoleByName(IdentityUserTypeEnum.SERVICE_ADMIN.getRoleName());
    }

    @Override
    public ClientRole getUserAdminRole() {
        return applicationRoleDao.getRoleByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName());
    }

    @Override
    public ClientRole getUserManageRole() {
        return applicationRoleDao.getRoleByName(IdentityUserTypeEnum.USER_MANAGER.getRoleName());
    }

    @Override
    public ClientRole getIdentityAdminRole() {
        return applicationRoleDao.getRoleByName(IdentityUserTypeEnum.IDENTITY_ADMIN.getRoleName());
    }

    @Override
    public ClientRole getDefaultRole() {
        return applicationRoleDao.getRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName());
    }

    @Override
    public ClientRole getComputeDefaultRole() {
        String serviceName = config.getString("serviceName.cloudServers");
        Application application = applicationService.getByName(serviceName);
        String defaultRoleName = application.getOpenStackType().concat(":default");
        return applicationRoleDao.getRoleByName(defaultRoleName);
    }

    @Override
    public ClientRole getObjectStoreDefaultRole() {
        String serviceName = config.getString("serviceName.cloudFiles");
        Application application = applicationService.getByName(serviceName);
        String defaultRoleName = application.getOpenStackType().concat(":default");
        return applicationRoleDao.getRoleByName(defaultRoleName);
    }

    @Override
    public boolean isIdentityUserTypeRole(ClientRole role) {
        return isIdentityUserTypeRole(role.getName());
    }

    @Override
    public boolean isIdentityUserTypeRole(String rolename) {
        return IdentityUserTypeEnum.isIdentityUserTypeRoleName(rolename);
    }

    @Override
    public List<ClientRole> getIdentityAccessRoles() {
        Application application = applicationService.getById(identityConfig.getStaticConfig().getCloudAuthClientId());
        return IteratorUtils.toList(applicationRoleDao.getIdentityRoles(application, IdentityUserTypeEnum.getUserTypeRoleNames()).iterator());
    }

    @Override
    public List<ClientRole> getAllIdentityRoles() {
        return IteratorUtils.toList(applicationRoleDao.getAllIdentityRoles().iterator());
    }

    @Override
    public boolean isRoleAssigned (String roleId) {
        Validate.notNull(roleId);
        return tenantRoleDao.getCountOfTenantRoleAssignmentsByRoleId(roleId) > 0;
    }
}
