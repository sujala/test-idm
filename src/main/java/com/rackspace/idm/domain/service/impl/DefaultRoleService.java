package com.rackspace.idm.domain.service.impl;

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
import java.util.List;

@Component
public class DefaultRoleService implements RoleService {

    @Autowired
    ApplicationRoleDao applicationRoleDao;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    Configuration config;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ClientRole getRoleByName(String roleName) {
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getSuperUserAdminRole() {
        String roleName = config.getString("cloudAuth.serviceAdminRole");
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getUserAdminRole() {
        String roleName = config.getString("cloudAuth.userAdminRole");
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getUserManageRole() {
        String roleName = config.getString("cloudAuth.userManagedRole");
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getIdentityAdminRole() {
        String roleName = config.getString("cloudAuth.adminRole");
        return applicationRoleDao.getRoleByName(roleName);
    }

    @Override
    public ClientRole getDefaultRole() {
        String roleName = config.getString("cloudAuth.userRole");
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
        List<String> identityRoleNames = getIdentityAccessRoleNames();
        for(String idmAccessRoleName : identityRoleNames) {
            if(StringUtils.equalsIgnoreCase(idmAccessRoleName, role.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ClientRole> getIdentityAccessRoles() {
        Application application = applicationService.getById(getCloudAuthClientId());
        return IteratorUtils.toList(applicationRoleDao.getIdentityRoles(application, getIdentityAccessRoleNames()).iterator());
    }

    private List<String> getIdentityAccessRoleNames() {
        List<String> roleNames = new ArrayList<String>();
        List<Object> roleNameObjs = config.getList("cloudAuth.accessRoleNames");
        for (Object roleName : roleNameObjs) {
            roleNames.add((String) roleName);
        }
        return roleNames;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

}
