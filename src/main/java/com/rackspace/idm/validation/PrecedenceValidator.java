package com.rackspace.idm.validation;

import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

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

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    private static final String NOT_AUTHORIZED = "Not Authorized";

    public void verifyCallerRolePrecedence(User user, ClientRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user);
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), role.getRsWeight());
    }

    public void verifyCallerRolePrecedence(EndUser user, TenantRole role) {
        ClientRole userIdentityRole = applicationService.getUserIdentityRole(user);
        ClientRole clientRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (userIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        compareWeights(userIdentityRole.getRsWeight(), clientRole.getRsWeight());
    }

    public void verifyCallerRolePrecedence(IdentityUserTypeEnum callerType, TenantRole role) {
        ClientRole clientRole = applicationService.getClientRoleById(role.getRoleRsId());
        compareWeights(callerType.getLevelAsInt(), clientRole.getRsWeight());
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
     * Verifies that effective caller has precedence over user by only checking the identity roles. User-manager will be
     * allow to have precedence over other user-manage.
     *
     * @throws ForbiddenException
     * @param user
     */
    public void verifyEffectiveCallerPrecedenceOverUser(BaseUser user) {
        BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();
        if (!(caller instanceof EndUser)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }

        IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallerAuthorizationContext().getIdentityUserType();
        ClientRole userIdentityRole = applicationService.getUserIdentityRole((EndUser) user);
        if (userIdentityRole != null) {
            if(!(IdentityUserTypeEnum.USER_MANAGER == callerType && callerType.getRoleName().equalsIgnoreCase(userIdentityRole.getName()))) {
                compareWeights(callerType.getLevelAsInt(), userIdentityRole.getRsWeight());
            }
        }

        if (callerType.isDomainBasedAccessLevel()) {
            authorizationService.verifyDomain(caller, user);
        }
    }

    /**
     * Verifies that the provided caller can list roles for the given user given the following rules:
    * 1. Users with role 'identity:get-user-roles-global' can list roles for any user
    * 2. Users can always list roles for themselves
    * 3. Users can list roles based on usual order of precedence service-admin -> identity-admin -> user-admin -> user-manage -> default-user
    * 4. If user-admin or below, the users must be in the same domain
     *
     * NOTE: The only exception to the above rules is that user-managers within the same domain can list roles for each other.
     *
     * @throws ForbiddenException if the call is not allowed to list roles for the given user
    */
    public void verifyCallerCanListRolesForUser(BaseUser caller, BaseUser user) {
        if (!user.getId().equals(caller.getId()) &&
                !authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.GET_USER_ROLES_GLOBAL.getRoleName())) {

            verifyCallerPrecedenceOverUserForListRoles(caller, user);

            IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(caller);
            if (userType.isDomainBasedAccessLevel() && !caller.getDomainId().equals(user.getDomainId())) {
                throw new ForbiddenException(NOT_AUTHORIZED);
            }
        }
    }

    private void verifyCallerPrecedenceOverUserForListRoles(BaseUser caller, BaseUser user) {
        if (!(caller instanceof EndUser && user instanceof EndUser)) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole((EndUser) caller);
        ClientRole userIdentityRole = applicationService.getUserIdentityRole((EndUser) user);
        if (callerIdentityRole != null && userIdentityRole != null) {
            // Allow users with user-manager role to list roles for other user-managers.
            if(!(callerIdentityRole.getName().equals(IdentityUserTypeEnum.USER_MANAGER.getRoleName())
                    && callerIdentityRole.equals(userIdentityRole))) {
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

    public void verifyCallerRolePrecedenceForAssignment(EndUser user, TenantRole role) {
        ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
        verifyCallerRolePrecedenceForAssignment(user, cRole);
    }

    public void verifyCallerRolePrecedenceForAssignment(EndUser user, ClientRole role) {
        ClientRole callerIdentityRole = applicationService.getUserIdentityRole(user);
        if (callerIdentityRole == null) {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        int callerWeight = callerIdentityRole.getRsWeight();
        compareWeights(callerWeight, role.getRsWeight());
    }

    public void verifyCallerRolePrecedenceForAssignment(IdentityUserTypeEnum callerType, ClientRole role) {
        compareWeights(callerType.getLevelAsInt(), role.getRsWeight());
    }

    public void verifyCallerRolePrecedenceForAssignment(EndUser user, Collection<String> roleNames) {
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
