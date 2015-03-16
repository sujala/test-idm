package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.RoleService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ClientRole getRoleByName(String roleName) {
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getSuperUserAdminRole() {
        String roleName = identityConfig.getStaticConfig().getIdentityServiceAdminRoleName();
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getUserAdminRole() {
        String roleName = identityConfig.getStaticConfig().getIdentityUserAdminRoleName();
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getUserManageRole() {
        String roleName = identityConfig.getStaticConfig().getIdentityUserManagerRoleName();
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getIdentityAdminRole() {
        String roleName = identityConfig.getStaticConfig().getIdentityIdentityAdminRoleName();
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getDefaultRole() {
        String roleName = identityConfig.getStaticConfig().getIdentityDefaultUserRoleName();
        return applicationRoleDao.getRoleByName(roleName);
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
    public boolean isIdentityAccessRole(ClientRole role) {
        return isIdentityAccessRole(role.getName());
    }

    @Override
    public boolean isIdentityAccessRole(String rolename) {
        String[] identityRoleNames = identityConfig.getStaticConfig().getIdentityAccessRoleNames();
        for(String idmAccessRoleName : identityRoleNames) {
            if(StringUtils.equalsIgnoreCase(idmAccessRoleName, rolename)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ClientRole> getIdentityAccessRoles() {
        Application application = applicationService.getById(identityConfig.getStaticConfig().getCloudAuthClientId());
        return IteratorUtils.toList(applicationRoleDao.getIdentityRoles(application, Arrays.asList(identityConfig.getStaticConfig().getIdentityAccessRoleNames())).iterator());
    }

    @Override
    public List<ClientRole> getAllIdentityRoles() {
        return IteratorUtils.toList(applicationRoleDao.getAllIdentityRoles().iterator());
    }
}
