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

    public void verifyCallerPrecedence(User user, ClientRole role) {
        int userWeight = userService.getUserWeight(user, getCloudAuthClientId());
        compareWeights(userWeight, role.getRsWeight());
    }

    public void verifyCallerPrecedence(User user, TenantRole role) {
        int userWeight = userService.getUserWeight(user, getCloudAuthClientId());
        ClientRole clientRole = applicationService.getClientRoleById(role.getRoleRsId());
        compareWeights(userWeight, clientRole.getRsWeight());
    }

    private void compareWeights(int callerWeight, int roleWeight) {
        if (callerWeight > roleWeight) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
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
