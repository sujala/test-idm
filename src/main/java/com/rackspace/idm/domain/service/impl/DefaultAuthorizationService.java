package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.validation.PrecedenceValidator;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.rackspace.idm.GlobalConstants.NOT_AUTHORIZED_MSG;

@Component
public class DefaultAuthorizationService implements AuthorizationService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthorizationService.class);

    @Autowired
    private ApplicationService applicationService;

    @Setter
    @Autowired
    private IdentityConfig identityConfig;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private UserService userService;
    @Autowired
    private DomainService domainService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private PrecedenceValidator precedenceValidator;

    @Autowired
    private IdentityUserService identityUserService;

    private Map<String, List<ImmutableClientRole>> identityRoleNameToImplicitRoleMap = new HashMap<String, List<ImmutableClientRole>>();

    @PostConstruct
    public void retrieveAccessControlRoles() {
        //On startup, cache implicit role map.
        populateImplicitMapWithRoles();
    }

    private void populateImplicitMapWithRoles() {
        Map<String, Set<String>> implicitRoles = identityConfig.getStaticConfig().getImplicitRoleMap();

        if (MapUtils.isNotEmpty(implicitRoles)) {
            for (Map.Entry<String, Set<String>> implicitEntrySet : implicitRoles.entrySet()) {
                String parentRoleName = implicitEntrySet.getKey();
                Set<String> implicitRoleNames = implicitEntrySet.getValue();
                List<ImmutableClientRole> implicitClientRoles = new ArrayList<>();
                for (String implicitRoleName : implicitRoleNames) {
                    ImmutableClientRole implicitClientRole = applicationService.getCachedClientRoleByName(implicitRoleName);
                    if (implicitClientRole != null) {
                        implicitClientRoles.add(implicitClientRole);
                    } else {
                        logger.warn(String.format("Implicit role '%s' for role '%s' was not found. Skipping.", implicitRoleName, parentRoleName));
                    }
                }
                if (CollectionUtils.isNotEmpty(implicitClientRoles)) {
                    identityRoleNameToImplicitRoleMap.put(parentRoleName, Collections.unmodifiableList(implicitClientRoles));
                }
            }
        }
    }

    @Override
    public List<ImmutableClientRole> getImplicitRolesForRole(String roleName) {
        List<ImmutableClientRole> clientRoles = identityRoleNameToImplicitRoleMap.get(roleName);
        if (clientRoles == null) {
            clientRoles = Collections.EMPTY_LIST;
        }
        return clientRoles;
    }

    @Override
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return false;
        }
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        IdentityUserTypeEnum userType = getIdentityTypeRoleAsEnum(user);
        return userType != null && userType.hasLevelAccessOf(IdentityUserTypeEnum.SERVICE_ADMIN);
    }

    @Override
    public boolean hasDefaultUserRole(EndUser user) {
        if (user == null) {
            return false;
        }
        return containsRole(user, Arrays.asList(getDefaultUserRole().asClientRole()));
    }

    @Override
    public boolean hasUserAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(getUserAdminRole().asClientRole()));
    }

    @Override
    public boolean hasIdentityAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(getIdentityAdminRole().asClientRole()));
    }

    @Override
    public boolean hasServiceAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(getServiceAdminRole().asClientRole()));
    }

    @Override
    public boolean hasSameDomain(EndUser caller, EndUser retrievedUser) {
        return caller.getDomainId() != null && caller.getDomainId().equals(retrievedUser.getDomainId());
    }

    @Override
    public boolean isSelf(BaseUser requester, User requestedUser) {
        return requester.getUniqueId() != null && requester.getUniqueId().equals(requestedUser.getUniqueId());
    }

    private ImmutableClientRole getServiceAdminRole() {
        return applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.SERVICE_ADMIN.getRoleName());
    }

    private ImmutableClientRole getIdentityAdminRole() {
        return applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.IDENTITY_ADMIN.getRoleName());
    }

    private ImmutableClientRole getUserAdminRole() {
        return applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.USER_ADMIN.getRoleName());
    }

    private ImmutableClientRole getUserManageRole() {
        return applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.USER_MANAGER.getRoleName());
    }

    private ImmutableClientRole getDefaultUserRole() {
        return applicationService.getCachedClientRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName());
    }

    @Override
    public void verifyEffectiveCallerCanImpersonate() {
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

        if (caller instanceof Racker) {
            //rackers must have impersonate group in edir
            Racker racker = (Racker) caller;
            List<String> rackerRoles = userService.getRackerIamRoles(racker.getRackerId());
            if(rackerRoles.isEmpty() || !rackerRoles.contains(identityConfig.getStaticConfig().getRackerImpersonateRoleName())) {
                throw new ForbiddenException("Missing RackImpersonation role needed for this operation.");
            }
        } else {
            verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
        }
    }

    @Override
    public void verifyEffectiveCallerHasTenantAccess(String tenantId) {
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();
        IdentityUserTypeEnum callerType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();

        // High level admins have implicit access to all tenants
        if (callerType.hasAtLeastIdentityAdminAccessLevel()) {
            return;
        }

        List<Tenant> adminTenants = new ArrayList<>();
        if (caller instanceof EndUser) {
            // Only EndUsers will ever have access to tenants.
            adminTenants = tenantService.getTenantsForUserByTenantRoles((EndUser) caller);
        }

        for (Tenant tenant : adminTenants) {
            if (tenant.getTenantId().equals(tenantId)) {
                return;
            }
        }

        String errMsg = NOT_AUTHORIZED_MSG;
        logger.warn(errMsg);
        throw new ForbiddenException(errMsg);
    }

    @Override
    public void verifyDomain(BaseUser caller, BaseUser retrievedUser) {
        if (!caller.getId().equals(retrievedUser.getId())) {
            if (caller.getDomainId() == null || !caller.getDomainId().equals(retrievedUser.getDomainId())) {
                throw new ForbiddenException(NOT_AUTHORIZED_MSG);
            }
        }
    }

    @Override
    public IdentityUserTypeEnum getIdentityTypeRoleAsEnum(ClientRole identityTypeRole) {
        Validate.notNull(identityTypeRole);

        IdentityUserTypeEnum identityUserTypeEnum = convertToIdentityUserType(identityTypeRole);
        if (identityUserTypeEnum == null) {
            throw new IllegalArgumentException(String.format("Unrecognized identity classification role '%s'", identityTypeRole.getName()));
        }
        return identityUserTypeEnum;
    }

    private IdentityUserTypeEnum convertToIdentityUserType(ClientRole identityTypeRole) {
        Validate.notNull(identityTypeRole);

        String userRoleName = identityTypeRole.getName();
        return IdentityUserTypeEnum.fromRoleName(userRoleName);
    }

    public IdentityUserTypeEnum getIdentityTypeRoleAsEnum(BaseUser baseUser) {
        if (baseUser == null) {
            return null;
        } else if (baseUser instanceof Racker) {
            return null;
        } else if (!(baseUser instanceof EndUser)) {
            throw new IllegalStateException(String.format("Unknown user type '%s'", baseUser.getClass().getName()));
        }

        EndUser user = (EndUser) baseUser;

        ClientRole identityRole = applicationService.getUserIdentityRole(user);

        return identityRole != null ? getIdentityTypeRoleAsEnum(identityRole) : null;
    }

    @Override
    public IdentityUserTypeEnum getIdentityTypeRoleAsEnum(Collection<TenantRole> userRoles) {
        ClientRole userLowestWeightIdentityRole = null;
        IdentityUserTypeEnum userLowestWeightIdentityRoleType = null;

        for (TenantRole tenantRole : userRoles) {
            ImmutableClientRole immutableClientRole = applicationService.getCachedClientRoleById(tenantRole.getRoleRsId());
            if (immutableClientRole != null) {
                //try to convert to classification role
                ClientRole clientRole = immutableClientRole.asClientRole();
                IdentityUserTypeEnum identityUserTypeEnum = convertToIdentityUserType(clientRole);
                if (identityUserTypeEnum != null) {
                    //only comparing identity user types
                    //role weight of this client role is < role weight of
                    //current lowest weight client role)
                    if (userLowestWeightIdentityRole == null
                            || clientRole.getRsWeight() < userLowestWeightIdentityRole.getRsWeight()) {
                        userLowestWeightIdentityRole = clientRole;
                        userLowestWeightIdentityRoleType = identityUserTypeEnum;
                    }
                }
            }
        }
        return userLowestWeightIdentityRoleType;
    }

    /**
     * Whether the user has at least one of the specified roles
     * @param user
     * @param clientRoles
     * @return
     */
    private boolean containsRole(BaseUser user, List<ClientRole> clientRoles) {
        HashSet<String> clientRoleIds = new HashSet<String>();
        for (ClientRole role : clientRoles) {
            clientRoleIds.add(role.getId());
        }

        for (String roleId : clientRoleIds) {
            if (tenantService.doesUserContainTenantRole(user, roleId)) {
                return true;
            }
        }

        return false;
    }

    private void verifyUserAccess(BaseUser user) {
        if( user != null && user.isDisabled() ) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new NotAuthorizedException(errMsg);
        }
    }

    @Override
    public boolean authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName) {
        List<String> rolesToSearch = new ArrayList<String>();
        if (StringUtils.isNotBlank(roleName)) {
            rolesToSearch.add(roleName);
        }
        rolesToSearch.addAll(getIdentityRolesForLevel(identityType));
        return authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    @Override
    public boolean authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum identityType, String... roleNames) {
        List<String> rolesToSearch = new ArrayList<>(Arrays.asList(roleNames));
        rolesToSearch.addAll(getIdentityRolesForLevel(identityType));
        return authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    @Override
    public boolean authorizeEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum identityType) {
        List<String> rolesToSearch = new ArrayList<>(getIdentityRolesForLevel(identityType));
        return authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    /**
     * Identity "user type" roles have a hierarchy. Service Admins < Identity Admins < User Admins < User Managers < Default Users
     *
     * When a service requires a certain access level, any user with that level or higher can access that service. When given
     * a "access level" (user type), this method returns all the access levels (user types) that can access that level.
     *
     * @param identityType
     * @return
     */
    private Set<String> getIdentityRolesForLevel(IdentityUserTypeEnum identityType) {
        return identityType.getTypesEqualOrHigherThanMeAsRoleNames();
    }

    @Override
    public void verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum identityType, String roleName) {
        List<String> rolesToSearch = new ArrayList<String>();
        if (StringUtils.isNotBlank(roleName)) {
            rolesToSearch.add(roleName);
        }
        rolesToSearch.addAll(getIdentityRolesForLevel(identityType));
        verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    @Override
    public void verifyEffectiveCallerHasIdentityTypeLevelAccessOrRoles(IdentityUserTypeEnum identityType, String... roleNames) {
        List<String> rolesToSearch = new ArrayList<>(Arrays.asList(roleNames));
        rolesToSearch.addAll(getIdentityRolesForLevel(identityType));
        verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    @Override
    public void verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum identityType) {
        verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(identityType, null);
    }

    @Override
    public void verifyEffectiveCallerHasRoleByName(String roleName) {
        Assert.hasText(roleName);

        List<String> rolesToSearch = new ArrayList<String>();
        rolesToSearch.add(roleName);

        verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(rolesToSearch);
    }

    @Override
    public void verifyEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames) {
        Assert.notNull(roleNames);

        //user must not be disabled... legacy check
        RequestContext requestContext = requestContextHolder.getRequestContext();
        verifyUserAccess(requestContext.getEffectiveCaller());

        boolean found = authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(roleNames);

        if (!found) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(String... roleNames) {
        Assert.notNull(roleNames);

        return authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(roleNames));
    }

    @Override
    public boolean authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(List<String> roleNames) {
        Assert.notNull(roleNames);

        RequestContext requestContext = requestContextHolder.getRequestContext();

        boolean found = false;
        for (int i=0; i<roleNames.size() && !found; i++) {
            if (requestContext.getEffectiveCallerAuthorizationContext().hasRoleWithName(roleNames.get(i))) {
                found = true;
            }
        }

        return found;
    }

    @Override
    public boolean restrictUserAuthentication(ServiceCatalogInfo serviceCatalogInfo) {
        return restrictEndpointsForTerminator(serviceCatalogInfo);
    }

    @Override
    public boolean restrictTokenEndpoints(ServiceCatalogInfo serviceCatalogInfo) {
        return restrictEndpointsForTerminator(serviceCatalogInfo);
    }

    @Override
    public boolean restrictEndpointsForTerminator(ServiceCatalogInfo serviceCatalogInfo) {
        return serviceCatalogInfo.allTenantsDisabled() &&
                serviceCatalogInfo.getUserTypeEnum() != null &&
                !serviceCatalogInfo.getUserTypeEnum().hasAtLeastIdentityAdminAccessLevel();
    }

    @Override
    public void verifyEffectiveCallerHasManagementAccessToUser(String userId) {
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

        // Verify caller is at least user-manage+
        verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_MANAGER);

        // Verify the target user exists
        User user = userService.checkAndGetUserById(userId);
        requestContextHolder.getRequestContext().setTargetEndUser(user);

        // If domain based identity role, must verify user has access to domain
        IdentityUserTypeEnum callersUserType = requestContextHolder.getRequestContext().getEffectiveCallerAuthorizationContext().getIdentityUserType();

        // Verify the caller has precedence over the user being modified
        precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(user);

        if (callersUserType == null) {
            // If we don't know the type of user, we can't authorize the user for anything
            throw new ForbiddenException(NOT_AUTHORIZED_MSG);
        } else if (callersUserType.isDomainBasedAccessLevel()) {
            // Verify that caller's domainId matches the target user's domainId
            if (!caller.getDomainId().equalsIgnoreCase(user.getDomainId())) {
                throw new ForbiddenException(NOT_AUTHORIZED_MSG);
            }
        }
    }

    @Override
    public boolean isCallerAuthorizedToManageDelegationAgreement(DelegationAgreement delegationAgreement) {
        Validate.notNull(delegationAgreement);
        Validate.notNull(delegationAgreement.getPrincipal());

        boolean isAuthorized = false;
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();
        DelegationPrincipal principal = delegationAgreement.getPrincipal();

        if (delegationAgreement.isEffectivePrincipal((EndUser) caller)) {
           isAuthorized = true;
        } else if (authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                Collections.singletonList(IdentityRole.RCN_ADMIN.getRoleName()))
                && domainService.doDomainsShareRcn(caller.getDomainId(), principal.getDomainId())) {
            isAuthorized = true;
        } else if (authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                Collections.singletonList(IdentityUserTypeEnum.USER_ADMIN.getRoleName()))
                && caller.getDomainId().equalsIgnoreCase(principal.getDomainId())) {
            isAuthorized = true;
        } else if (authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                Collections.singletonList(IdentityUserTypeEnum.USER_MANAGER.getRoleName()))
                && caller.getDomainId().equalsIgnoreCase(principal.getDomainId())) {
            if (principal.getPrincipalType().equals(PrincipalType.USER)) {
                EndUser principalUser = identityUserService.getEndUserById(principal.getId());
                isAuthorized = !hasUserAdminRole(principalUser);
            } else {
                isAuthorized = true;
            }
        }

        return isAuthorized;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }
}