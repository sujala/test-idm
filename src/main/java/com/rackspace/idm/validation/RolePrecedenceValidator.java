package com.rackspace.idm.validation;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/27/12
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class RolePrecedenceValidator {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;

    private final String NOT_AUTHORIZED = "Not Authorized to manage specified role";

    public void verifyCallerRolePrecedence(User user, ClientRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user, getCloudAuthClientId(), getIdentityRoleNames());
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), role.getRsWeight());
    }

    public void verifyCallerRolePrecedence(User user, TenantRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user, getCloudAuthClientId(), getIdentityRoleNames());
        ClientRole clientRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), clientRole.getRsWeight());
    }

    public void verifyCallerPrecedenceOverUser(User caller, User user) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(caller, getCloudAuthClientId(), getIdentityRoleNames());
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user, getCloudAuthClientId(), getIdentityRoleNames());
        if (callerIdentityRole != null) {
            if (userIdentityRole != null) {
                compareWeights(callerIdentityRole.getRsWeight(), userIdentityRole.getRsWeight());
            }
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    private void compareWeights(int callerWeight, int roleWeight) {
        if (callerWeight > roleWeight) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private List<String> getIdentityRoleNames() {
        List<String> names = new ArrayList<String>();
        names.add(config.getString("cloudAuth.userRole"));
        names.add(config.getString("cloudAuth.userAdminRole"));
        names.add(config.getString("cloudAuth.adminRole"));
        names.add(config.getString("cloudAuth.serviceAdminRole"));
        return names;
    }

    private void setUserService(UserService service) {
        this.userService = service;
    }

    private void setApplicationService(ApplicationService service) {
        this.applicationService = service;
    }

    private void setConfig(Configuration config) {
        this.config = config;
    }
}
