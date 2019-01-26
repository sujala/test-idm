package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultAuthorizationService implements AuthorizationService {
    public static final String NOT_AUTHORIZED_MSG = "Not Authorized";
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthorizationService.class);

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Configuration config;
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
    private DelegationService delegationService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private PrecedenceValidator precedenceValidator;

    @Autowired
    private IdentityUserService identityUserService;

    private Map<String, List<ImmutableClientRole>> identityRoleNameToImplicitRoleMap = new HashMap<String, List<ImmutableClientRole>>();

    @PostConstruct
    public void retrieveAccessControlRoles() {
        //On startup, cache all the identity client roles into easily accessible maps.
        List<ClientRole> identityRoles = roleService.getAllIdentityRoles();
        if (identityRoles != null) {
            for (ClientRole identityRole : identityRoles) {
                populateImplicitMapWithRoles(identityRole);
            }
        }
    }

    private void populateImplicitMapWithRoles(ClientRole role) {
        String roleName = role.getName();

        if (identityRoleNameToImplicitRoleMap.containsKey(roleName)) {
            String errorMsg = String.format("Multiple identity authorization roles exists with name '%s'. Ignoring role with id '%s'", roleName, role.getId());
            logger.error(errorMsg);
        } else {
            Set<IdentityRole> implicitRoles = identityConfig.getStaticConfig().getImplicitRolesForRole(roleName);

            List<ImmutableClientRole> implicitClientRoles = new ArrayList<ImmutableClientRole>();
            if (CollectionUtils.isNotEmpty(implicitRoles)) {
                for (IdentityRole implicitIdentityRole : implicitRoles) {
                    ImmutableClientRole implicitClientRole = applicationService.getCachedClientRoleByName(implicitIdentityRole.getRoleName());
                    if (implicitClientRole != null) {
                        implicitClientRoles.add(implicitClientRole);
                    } else {
                        logger.warn(String.format("Implicit role '%s' for role '%s' was not found. Skipping.", implicitIdentityRole.getRoleName(), roleName));
                    }
                }
                if (CollectionUtils.isNotEmpty(implicitClientRoles)) {
                    identityRoleNameToImplicitRoleMap.put(roleName, Collections.unmodifiableList(implicitClientRoles));
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

        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(getServiceAdminRole().asClientRole()));
    }

    public boolean authorizeRacker(ScopeAccess scopeAccess){
        logger.debug("Authorizing {} as a Racker", scopeAccess);
        if (!(scopeAccess instanceof RackerScopeAccess)){
            return false;
        }
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        boolean authorized = authorize(user, scopeAccess, Arrays.asList(applicationService.getCachedClientRoleByName(GlobalConstants.ROLE_NAME_RACKER).asClientRole()));
        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
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
    public boolean hasUserManageRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(getUserManageRole().asClientRole()));
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
    public void verifyServiceAdminLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(getServiceAdminRole().asClientRole()));
    }

    @Override
    public void verifyEffectiveCallerCanImpersonate() {
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

        if (caller instanceof Racker) {
            //rackers must have impersonate group in edir
            Racker racker = (Racker) caller;
            List<String> rackerRoles = userService.getRackerEDirRoles(racker.getRackerId());
            if(rackerRoles.isEmpty() || !rackerRoles.contains(identityConfig.getStaticConfig().getRackerImpersonateRoleName())) {
                throw new ForbiddenException("Missing RackImpersonation role needed for this operation.");
            }
        } else {
            verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
        }
    }

    @Override
    public void verifySelf(User requester, User requestedUser) {
        if (!(requester.getUsername().equals(requestedUser.getUsername()) && (requester.getUniqueId().equals(requestedUser.getUniqueId())))) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
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
    public void checkAuthAndHandleFailure(boolean authorized, ScopeAccess scopeAccess) {
        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                    scopeAccess.getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
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
        if (identityConfig.getIdentityServiceAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.SERVICE_ADMIN;}
        if (identityConfig.getIdentityIdentityAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.IDENTITY_ADMIN;}
        if (identityConfig.getIdentityUserAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.USER_ADMIN;}
        if (identityConfig.getIdentityUserManagerRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.USER_MANAGER;}
        if (identityConfig.getIdentityDefaultUserRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.DEFAULT_USER;}

        return null;
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


    private boolean authorize(BaseUser user, ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return false;
        }

        return containsRole(user, clientRoles);
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

    private void verifyRoleAccess(BaseUser user, ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        if (!authorizeRoleAccess(user, scopeAccess, clientRoles)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private boolean authorizeRoleAccess(BaseUser user, ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        String rolesString = getRoleString(clientRoles);

        logger.debug("Authorizing {} as {}", scopeAccess, rolesString);
        boolean authorized = authorize(user, scopeAccess, clientRoles);
        logger.debug(String.format("Authorized %s as %s - %s", scopeAccess, rolesString, authorized));
        return authorized;
    }

    private void verifyDomainAccess(Domain domain) {
        if(domain != null && !domain.getEnabled()) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new NotAuthorizedException(errMsg);
        }
    }

    private boolean authorizeDomainAccess(Domain domain) {
        return domain == null || domain.getEnabled();

    }

    private void verifyUserAccess(BaseUser user) {
        if( user != null && user.isDisabled() ) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new NotAuthorizedException(errMsg);
        }
    }

    private boolean authorizeUserAccess(BaseUser user) {
        return user == null || !user.isDisabled();

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
        Set<String> rolesAtOrHigherThanLevel = new HashSet<String>();

        if (IdentityUserTypeEnum.SERVICE_ADMIN.hasLevelAccessOf(identityType)) {
            rolesAtOrHigherThanLevel.add(identityConfig.getStaticConfig().getIdentityServiceAdminRoleName());
        }

        if (IdentityUserTypeEnum.IDENTITY_ADMIN.hasLevelAccessOf(identityType)) {
            rolesAtOrHigherThanLevel.add(identityConfig.getStaticConfig().getIdentityIdentityAdminRoleName());
        }

        if (IdentityUserTypeEnum.USER_ADMIN.hasLevelAccessOf(identityType)) {
            rolesAtOrHigherThanLevel.add(identityConfig.getStaticConfig().getIdentityUserAdminRoleName());
        }

        if (IdentityUserTypeEnum.USER_MANAGER.hasLevelAccessOf(identityType)) {
            rolesAtOrHigherThanLevel.add(identityConfig.getStaticConfig().getIdentityUserManagerRoleName());
        }

        if (IdentityUserTypeEnum.DEFAULT_USER.hasLevelAccessOf(identityType)) {
            rolesAtOrHigherThanLevel.add(identityConfig.getStaticConfig().getIdentityDefaultUserRoleName());
        }

        return rolesAtOrHigherThanLevel;
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

    private String getRoleString(List<ClientRole> clientRoles) {
        List<String> roles = new ArrayList<String>();
        for (ClientRole clientRole : clientRoles) {
            roles.add(clientRole.getName());
        }
        return StringUtils.join(roles, " ");
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getIdmSuperAdminRoleName() {
        return config.getString("idm.superAdminRole");
    }

    private String getCloudAuthServiceAdminRole() {
        return config.getString("cloudAuth.serviceAdminRole");
    }

    private String getCloudAuthIdentityAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }

    private String getCloudAuthUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getCloudAuthUserManagedRole() {
        return config.getString("cloudAuth.userManagedRole");
    }

    public void setConfig(Configuration config) {
        this.config = config;
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
