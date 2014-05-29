package com.rackspace.idm.validation;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/27/12
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */

@Component
public class PrecedenceValidator {

    public static final int RBAC_ROLES_WEIGHT = 1000;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    RoleService roleService;

    @Autowired
    private Configuration config;

    private static final String NOT_AUTHORIZED = "Not Authorized";

    public void verifyCallerRolePrecedence(User user, ClientRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user);
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), role.getRsWeight());
    }

    public void verifyCallerRolePrecedence(User user, TenantRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user);
        ClientRole clientRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), clientRole.getRsWeight());
    }

    public void verifyCallerPrecedenceOverUser(User caller, User user) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(caller);
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user);
        if (callerIdentityRole != null) {
            if (userIdentityRole != null) {
                compareWeights(callerIdentityRole.getRsWeight(), userIdentityRole.getRsWeight());
            }
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    public void verifyCallerRolePrecedenceForAssignment(User user, TenantRole role) {
        ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
        verifyCallerRolePrecedenceForAssignment(user, cRole);
    }

    public void verifyCallerRolePrecedenceForAssignment(User user, ClientRole role) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(user);
        if (callerIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        int callerWeight = callerIdentityRole.getRsWeight();
        compareWeights(callerWeight, role.getRsWeight());
    }

    public void verifyCallerRolePrecedenceForAssignment(User user, List<String> roleNames) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(user);
        if (callerIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }

        verifyRolePrecedenceForAssignment(callerIdentityRole, roleNames);
    }

    public void verifyRolePrecedenceForAssignment(ClientRole clientRole, List<String> roleNames) {
        if (clientRole != null && roleNames != null) {
            verifyRolePrecedenceForAssignment(clientRole.getRsWeight(), roleNames);
        }
    }

    public void verifyRolePrecedenceForAssignment(int roleWeight, List<String> roleNames) {
        for (String roleName : roleNames) {
            ClientRole role = roleService.getRoleByName(roleName);
            if (role != null) {
                compareWeights(roleWeight, role.getRsWeight());
            }
        }
    }

    private void compareWeights(int callerWeight, int roleWeight) {
        if (callerWeight >= roleWeight) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }
}
