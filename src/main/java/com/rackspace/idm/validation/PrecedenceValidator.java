package com.rackspace.idm.validation;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
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

    public void verifyCallerPrecedenceOverUser(BaseUser caller, BaseUser user) {
        if (!(caller instanceof EndUser && user instanceof EndUser)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole((EndUser) caller);
        ClientRole userIdentityRole = applicationService.getUserIdentityRole((EndUser) user);
        if (callerIdentityRole != null) {
            if (userIdentityRole != null) {
                compareWeights(callerIdentityRole.getRsWeight(), userIdentityRole.getRsWeight());
            }
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    /**
     * Throws Forbidden exception UNLESS the "caller" role has a higher access level than the "target" role based on standard levels of precedence
     * for roles.
     *
     * @param identityRoleOfCaller
     * @param identityRoleOfTarget
     */
    public void verifyHasGreaterAccess(ClientRole identityRoleOfCaller, ClientRole identityRoleOfTarget) {
        Validate.notNull(identityRoleOfCaller);
        Validate.notNull(identityRoleOfTarget);

        if (!hasGreaterAccess(identityRoleOfCaller, identityRoleOfTarget)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    /**
     * Throws Forbidden exception UNLESS the "caller" role has a higher or equal access level than the "target" role based on standard levels of precedence
     * for roles.
     *
     * @param identityRoleOfCaller
     * @param identityRoleOfTarget
     */
    public void verifyHasGreaterOrEqualAccess(ClientRole identityRoleOfCaller, ClientRole identityRoleOfTarget) {
        Validate.notNull(identityRoleOfCaller);
        Validate.notNull(identityRoleOfTarget);

        if (!hasGreaterOrEqualAccess(identityRoleOfCaller, identityRoleOfTarget)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    /**
     * Whether the "caller" role has a higher access level than the "target" role based on standard levels of precedence
     * for roles.
     *
     * @param identityRoleOfCaller
     * @param identityRoleOfTarget
     */
    public boolean hasGreaterAccess(ClientRole identityRoleOfCaller, ClientRole identityRoleOfTarget) {
        Validate.notNull(identityRoleOfCaller);
        Validate.notNull(identityRoleOfTarget);

        //lesser weight means higher access level
        return callerHasLowerWeight(identityRoleOfCaller.getRsWeight(), identityRoleOfTarget.getRsWeight());
    }

    /**
     * Whether the "caller" role has a higher or equal access level than the "target" role based on standard levels of precedence
     * for roles.
     *
     * @param identityRoleOfCaller
     * @param identityRoleOfTarget
     */
    public boolean hasGreaterOrEqualAccess(ClientRole identityRoleOfCaller, ClientRole identityRoleOfTarget) {
        Validate.notNull(identityRoleOfCaller);
        Validate.notNull(identityRoleOfTarget);

        int callerWeight = identityRoleOfCaller.getRsWeight();
        int targetWeight = identityRoleOfTarget.getRsWeight();

        //lesser weight means higher access level
        return callerWeight == targetWeight || callerHasLowerWeight(callerWeight, targetWeight);
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

    public void verifyCallerRolePrecedenceForAssignment(User user, Collection<String> roleNames) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(user);
        if (callerIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }

        verifyRolePrecedenceForAssignment(callerIdentityRole, roleNames);
    }

    public void verifyRolePrecedenceForAssignment(ClientRole clientRole, Collection<String> roleNames) {
        if (clientRole != null && roleNames != null) {
            verifyRolePrecedenceForAssignment(clientRole.getRsWeight(), roleNames);
        }
    }

    public void verifyRolePrecedenceForAssignment(int roleWeight, Collection<String> roleNames) {
        for (String roleName : roleNames) {
            ClientRole role = roleService.getRoleByName(roleName);
            if (role != null) {
                compareWeights(roleWeight, role.getRsWeight());
            }
        }
    }

    private void compareWeights(int callerWeight, int roleWeight) {
        if (!callerHasLowerWeight(callerWeight, roleWeight)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    private boolean callerHasLowerWeight(int callerWeight, int roleWeight) {
        return callerWeight < roleWeight;
    }
}
