package com.rackspace.idm.domain.service.impl;


import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainRequestHandler;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DomainDefaultException;
import com.rackspace.idm.util.RoleUtil;
import com.rackspace.idm.util.predicate.UserEnabledPredicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.rackspace.idm.ErrorCodes.ERROR_MESSAGE_ONLY_ONE_USER_ADMIN_ALLOWED;


@Component
public class DefaultCreateSubUserService implements CreateSubUserService {
    private static final Logger log = LoggerFactory.getLogger(FederatedDomainRequestHandler.class);

    public static final String ERROR_MSG_DOMAIN_DEFAULT_INVALID = "Domain defaults can not be determined";

    @Autowired
    RoleService roleService;

    @Autowired
    UserService userService;

    @Autowired
    DomainService domainService;

    @Autowired
    TenantService tenantService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    RequestContextHolder requestContextHolder;

    @Autowired
    ApplicationService applicationService;

    @Override
    public User setDefaultsAndCreateUser(org.openstack.docs.identity.api.v2.User userForCreate, User userForDefaults) {
        User user = this.userConverterCloudV20.fromUser(userForCreate);
        setUserDefaults(user, userForDefaults);
        userService.addSubUserV20(user, CreateUserUtil.isCreateUserOneCall(userForCreate));
        return user;
    }

    /**
     * This is expensive if we have to do it on every auth/validate for a delegate.
     *
     * TODO: Investigate moving all this to domain
     *
     * @param domainId
     * @return
     *
     * @throws DomainDefaultException If defaults can't be determined for the domain
     */
    @Override
    public DomainSubUserDefaults calculateDomainSubUserDefaults(String domainId) {
        User userAdmin = getUserAdminForSubUserDefaults(domainId);
        Domain domain = domainService.getDomain(domainId);
        List<TenantRole> subUserRoles = getAssignableCallerRoles(userAdmin);

        ImmutableClientRole icr = applicationService.getCachedClientRoleByName(identityConfig.getStaticConfig().getIdentityDefaultUserRoleName());
        TenantRole tr = RoleUtil.newTenantRoleFromClientRole(icr);
        subUserRoles.add(tr);

        return new DomainSubUserDefaults(domain, userAdmin.getRsGroupId(), userAdmin.getRegion(), subUserRoles);
    }

    /**
     * Currently numerous defaults are based on the user-admin of a domain. Need to retrieve the user-admin to copy them
     * off.
     *
     * This is extremely inefficient.
     */
    private User getUserAdminForSubUserDefaults(String domainId) {
        List<User> userAdmins = domainService.getDomainAdmins(domainId);

        if(userAdmins.size() == 0) {
            log.error("A user admin for domain {} does not exist", domainId);
            throw new DomainDefaultException(ERROR_MSG_DOMAIN_DEFAULT_INVALID, ErrorCodes.ERROR_CODE_DOMAIN_DEFAULT_MISSING_USER_ADMIN);
        }

        if(userAdmins.size() > 1 && identityConfig.getStaticConfig().getDomainRestrictedToOneUserAdmin()) {
            String errorMessage = String.format(ERROR_MESSAGE_ONLY_ONE_USER_ADMIN_ALLOWED, domainId);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        User firstEnabledUserAdmin = org.apache.commons.collections4.CollectionUtils.find(userAdmins, new UserEnabledPredicate());
        if(firstEnabledUserAdmin == null) {
            log.error("An enabled user admin does not exist for domain {}", domainId);
            throw new DomainDefaultException(ERROR_MSG_DOMAIN_DEFAULT_INVALID, ErrorCodes.ERROR_CODE_DOMAIN_DEFAULT_NO_ENABLED_USER_ADMIN);
        }
        return firstEnabledUserAdmin;
    }


    private void setUserDefaults(User user, User userForDefaults) {
        if (StringUtils.isBlank(userForDefaults.getDomainId())) {
            throw new BadRequestException("Default user cannot be created if caller does not have a domain");
        }

        IdentityUserTypeEnum callerUserType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();
        if (!(IdentityUserTypeEnum.IDENTITY_ADMIN.equals(callerUserType) || IdentityUserTypeEnum.SERVICE_ADMIN.equals(callerUserType))) {
            user.setTokenFormat(null);
        }

        String callerDefaultRegion = getRegionBasedOnCaller(userForDefaults);
        List<TenantRole> callerRoles = getAssignableCallerRoles(userForDefaults);
        Iterable<Group> callerGroups = userService.getGroupsForUser(userForDefaults.getId());

        user.setMossoId(userForDefaults.getMossoId());
        user.setNastId(userForDefaults.getNastId());
        user.setDomainId(userForDefaults.getDomainId());
        user.setRegion(callerDefaultRegion);

        CreateUserUtil.attachRoleToUser(roleService.getDefaultRole(), user);
        attachRolesToUser(callerRoles, user);
        attachGroupsToUser(callerGroups, user);
    }

    /**
     * [1] When an identity:user-admin or identity:user-manage creates a new user, that new user should get the same
     * RAX-AUTH:defaultRegion that the identity:user-admin of that domain has regardless of the user that creating user.
     * [2] if there is more than 1 user-admin of an account (which should be very, very unlikely) then the default
     * region is the same as the user that created them.
     * [3] If for some completely unknown reason, there is no user-admin for the domain (which technically is an invalid
     * state), set the default region to the same as the user that created them.
     * @param caller
     * @return
     */
    private String getRegionBasedOnCaller(User caller) {
        String defaultRegion;

        List<User> admins = this.domainService.getDomainAdmins(caller.getDomainId());
        if (admins.size() == 1) {
            defaultRegion = admins.get(0).getRegion();
        } else if (identityConfig.getStaticConfig().getDomainRestrictedToOneUserAdmin() && admins.size() > 1) {
            throw new IllegalStateException("Can't retrieve single user-admin for domain " + caller.getDomainId());
        } else {
            //either 0 admins or > 1 admins
            defaultRegion = caller.getRegion();
        }

        return defaultRegion;
    }

    /**
     * Gets all the roles from the caller that can be assignable to users that the caller creates.
     * This is temporary. Roles that a user gets by default should not be based on the caller. It
     * will be based on some sort of domain template.
     */
    private List<TenantRole> getAssignableCallerRoles(User caller) {
        List<TenantRole> assignableTenantRoles = new ArrayList<TenantRole>();

        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(caller);
        for (TenantRole tenantRole : tenantRoles) {
            if (!IdentityUserTypeEnum.isIdentityUserTypeRoleName(tenantRole.getName()) && tenantRole.getPropagate()) {
                TenantRole assignableTenantRole = new TenantRole();
                assignableTenantRole.setClientId(tenantRole.getClientId());
                assignableTenantRole.setDescription(tenantRole.getDescription());
                assignableTenantRole.setName(tenantRole.getName());
                assignableTenantRole.setRoleRsId(tenantRole.getRoleRsId());
                assignableTenantRole.setTenantIds(tenantRole.getTenantIds());
                assignableTenantRoles.add(assignableTenantRole);
            }
        }

        return assignableTenantRoles;
    }

    private void attachRolesToUser(List<TenantRole> tenantRoles, User user) {
        for (TenantRole tenantRole : tenantRoles) {
            if (!user.getRoles().contains(tenantRole)) {
                user.getRoles().add(tenantRole);
            }
        }
    }

    private void attachGroupsToUser(Iterable<Group> groups, User user) {
        for (Group group : groups) {
            user.getRsGroupId().add(group.getGroupId());
        }
    }

}
