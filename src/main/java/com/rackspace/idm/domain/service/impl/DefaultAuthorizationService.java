package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthorizedException;
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
import java.util.*;

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
    private RequestContextHolder requestContextHolder;

    //TODO: Remove these constants and lookup via map cache rather than these individual variables serving as a cache
    private ClientRole idmSuperAdminRole = null;
    private ClientRole cloudServiceAdminRole = null;
    private ClientRole cloudIdentityAdminRole = null;
    private ClientRole cloudUserAdminRole = null;
    private ClientRole cloudUserRole = null;
    private ClientRole cloudUserManagedRole = null ;
    private ClientRole rackerRole = null ;

    /**
     * A map of the identity role names to an immutable client role. Use immutable to prevent inadvertent changes that
     * would corrupt the cache.
     */
    private Map<String, ImmutableClientRole> identityRoleNameToRoleMap = new HashMap<String, ImmutableClientRole>();

    /**
     * A map of the identity role ids to an immutable client role. Use immutable versions of the roles to prevent
     * inadvertent changes that would corrupt the cache.
     */
    private Map<String, ImmutableClientRole> identityRoleIdToRoleMap = new HashMap<String, ImmutableClientRole>();

    private Map<String, List<ImmutableClientRole>> identityRoleNameToImplicitRoleMap = new HashMap<String, List<ImmutableClientRole>>();

    @PostConstruct
    public void retrieveAccessControlRoles() {
        //On startup, cache all the identity client roles into easily accessible maps.
        List<ClientRole> identityRoles = roleService.getAllIdentityRoles();
        if (identityRoles != null) {
            //populate the base set of maps
            for (ClientRole identityRole : identityRoles) {
                populateMapsWithRole(identityRole);
            }

            //loop second time to process implicit roles
            for (ClientRole identityRole : identityRoles) {
                populateImplicitMapWithRoles(identityRole);
            }

        }

        //TODO: Remove dependence on these variables and lookup from maps. Loading backup method just to be on safe side
        //to do what legacy code did. Eventually should remove this
        cloudServiceAdminRole = loadClientRole(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
        idmSuperAdminRole = loadClientRole(getIdmClientId(), getIdmSuperAdminRoleName());
        cloudServiceAdminRole = loadClientRole(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
        cloudIdentityAdminRole = loadClientRole(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
        cloudUserAdminRole = loadClientRole(getCloudAuthClientId(), getCloudAuthUserAdminRole());
        cloudUserRole = loadClientRole(getCloudAuthClientId(), getCloudAuthUserRole());
        cloudUserManagedRole = loadClientRole(getCloudAuthClientId(), getCloudAuthUserManagedRole());
        rackerRole = loadClientRole(getIdmClientId(), GlobalConstants.ROLE_NAME_RACKER);

        //add in racker role as we may make auth decisions on it
        if (rackerRole != null) {
            populateMapsWithRole(rackerRole);
        }
    }

    @Override
    public ImmutableClientRole getCachedIdentityRoleById(String id) {
        return identityRoleIdToRoleMap.get(id);
    }

    @Override
    public ImmutableClientRole getCachedIdentityRoleByName(String name) {
        return identityRoleNameToRoleMap.get(name);
    }

    private ClientRole loadClientRole(String clientId, String roleName) {
        ImmutableClientRole role = identityRoleNameToRoleMap.get(roleName);

        ClientRole result;
        if (role == null) {
            result = applicationService.getClientRoleByClientIdAndRoleName(clientId, roleName);
        } else {
            result = role.asClientRole();
        }
        return result;
    }

    private void populateMapsWithRole(ClientRole role) {
        String roleName = role.getName();

        //name should be globally unique, but perform some verification
        if (identityRoleNameToRoleMap.containsKey(roleName)) {
            String errorMsg = String.format("Multiple identity authorization roles exists with name '%s'. Ignoring role with id '%s'", roleName, role.getId());
            logger.error(errorMsg);
        } else {
            identityRoleNameToRoleMap.put(role.getName(), new ImmutableClientRole(role));
            identityRoleIdToRoleMap.put(role.getId(), new ImmutableClientRole(role)); //assume id is globally unique
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
                    ImmutableClientRole implicitClientRole = identityRoleNameToRoleMap.get(implicitIdentityRole.getRoleName());
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
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole));
    }

    public boolean authorizeRacker(ScopeAccess scopeAccess){
        logger.debug("Authorizing {} as a Racker", scopeAccess);
        if (!(scopeAccess instanceof RackerScopeAccess)){
            return false;
        }
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        boolean authorized = authorize(user, scopeAccess, Arrays.asList(rackerRole));
        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(cloudIdentityAdminRole));
    }

    @Override
    public boolean authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess) {
        boolean isRackspaceClient = authorizeRackspaceClient(scopeAccess);
        boolean isIdmSuperAdmin = false;
        //verify if caller is a rackspace client, idm client or super admin
        if(!isRackspaceClient){
            isIdmSuperAdmin = authorizeIdmSuperAdmin(scopeAccess);
        }

        if(!isRackspaceClient && ! isIdmSuperAdmin) {
            throw new ForbiddenException("Access denied");
        }
        return true;
    }

    @Override
    public boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        if (!authorizeUserAccess(user)) {
            return false;
        }
        Domain domain = domainService.getDomain(user.getDomainId());
        if (!authorizeDomainAccess(domain)) {
            return false;
        }
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(cloudUserAdminRole));
    }

    @Override
    public boolean authorizeUserManageRole(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        if (!authorizeUserAccess(user)) {
            return false;
        }
        Domain domain = domainService.getDomain(user.getDomainId());
        if (!authorizeDomainAccess(domain)) {
            return false;
        }
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(cloudUserManagedRole));
    }

    @Override
    public boolean authorizeCloudUser(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        if (!authorizeUserAccess(user)) {
            return false;
        }
        Domain domain = domainService.getDomain(user.getDomainId());
        if (!authorizeDomainAccess(domain)) {
            return false;
        }
        return authorizeRoleAccess(user, scopeAccess, Arrays.asList(cloudUserRole));
    }

    @Override
    public boolean hasDefaultUserRole(EndUser user) {
        if (user == null) {
            return false;
        }
        return containsRole(user, Arrays.asList(cloudUserRole));
    }

    @Override
    public boolean hasUserAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(cloudUserAdminRole));
    }

    @Override
    public boolean hasUserManageRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(cloudUserManagedRole));
    }

    @Override
    public boolean hasIdentityAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(cloudIdentityAdminRole));
    }

    @Override
    public boolean hasServiceAdminRole(EndUser user) {
        if (user == null || user instanceof FederatedUser) {
            return false;
        }
        return containsRole(user, Arrays.asList(cloudServiceAdminRole));
    }

    @Override
    public boolean hasSameDomain(User caller, User retrievedUser) {
        return caller.getDomainId() != null && caller.getDomainId().equals(retrievedUser.getDomainId());
    }

    @Override
    public boolean isSelf(User requester, User requestedUser) {
        if(requester.getUsername() != null && requester.getUsername().equals(requestedUser.getUsername())){
            return true;
        }

        if(requester.getUniqueId() != null && requester.getUniqueId().equals(requestedUser.getUniqueId())){
            return true;
        }

        return false;
    }

    private boolean hasNullvalues(String... values){
        for(String value : values){
            if(value == null){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as idm super admin", scopeAccess);

        if (this.authorizeCustomerIdm(scopeAccess)) {
            return true;
        }

        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        boolean authorized = authorize(user, scopeAccess, Arrays.asList(idmSuperAdminRole));

        logger.debug("Authorized {} as idm super admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeRackspaceClient(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as rackspace client", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }
        boolean authorized = scopeAccess.getClientRCN().equalsIgnoreCase(this.getRackspaceCustomerId());
        logger.debug("Authorized {} as rackspace client - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerIdm(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as Idm", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }

        boolean authorized = getIdmClientId().equalsIgnoreCase(scopeAccess.getClientId())
                && getRackspaceCustomerId().equalsIgnoreCase(scopeAccess.getClientRCN());
        logger.debug("Authorized {} as Idm - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess,
                                               ScopeAccess requestingScopeAccess) {
        logger.debug("Authorizing as Requestor or Owner");

        boolean isRequestor = requestingScopeAccess instanceof ClientScopeAccess
                && targetScopeAccess != null
                && requestingScopeAccess != null
                && requestingScopeAccess.getClientId().equalsIgnoreCase(
                targetScopeAccess.getClientId());

        boolean isOwner = false;

        if (targetScopeAccess instanceof ClientScopeAccess && requestingScopeAccess != null) {
            isOwner = requestingScopeAccess.getClientId().equals(
                    targetScopeAccess.getClientId());
        } else if (targetScopeAccess instanceof UserScopeAccess && requestingScopeAccess instanceof UserScopeAccess) {
            isOwner = ((UserScopeAccess) requestingScopeAccess).getUserRsId()
                    .equals(((UserScopeAccess) targetScopeAccess).getUserRsId());
        } else if (targetScopeAccess instanceof RackerScopeAccess && requestingScopeAccess instanceof RackerScopeAccess) {
            isOwner = ((RackerScopeAccess) requestingScopeAccess).getRackerId()
                    .equals(((RackerScopeAccess) targetScopeAccess).getRackerId());
        }

        logger.debug("Authorized as Requestor({}) or Owner({})", isRequestor, isOwner);
        return (isRequestor || isOwner);
    }

    public void verifyIdmSuperAdminAccess(String authHeader) {
        if(!this.authorizeIdmSuperAdmin(scopeAccessService.getScopeAccessByAccessToken(authHeader))){
            throw new ForbiddenException("Access denied");
        }
    }

    @Override
    public void verifyServiceAdminLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole));
    }

    @Override
    public void verifyRackerOrIdentityAdminAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(rackerRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyIdentityAdminLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyUserAdminLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        Domain domain = domainService.getDomain(user.getDomainId());
        verifyDomainAccess(domain);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole));
    }

    @Override
    public void verifyUserManagedLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        Domain domain = domainService.getDomain(user.getDomainId());
        verifyDomainAccess(domain);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserManagedRole));
    }

    @Override
    public void verifyUserManagedLevelAccess(EndUser user) {
        ClientRole requesterIdentityClientRole = applicationService.getUserIdentityRole(user);
        IdentityUserTypeEnum userIdentityRole = getIdentityTypeRoleAsEnum(requesterIdentityClientRole);
        verifyUserManagedLevelAccess(userIdentityRole);
    }

    @Override
    public void verifyUserManagedLevelAccess(IdentityUserTypeEnum userType) {
        if (userType == null || !userType.hasAtLeastUserManagedAccessLevel()) {
            throw new ForbiddenException(NOT_AUTHORIZED_MSG);
        }
    }

    @Override
    public void verifyUserLevelAccess(ScopeAccess scopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        verifyUserAccess(user);
        Domain domain = domainService.getDomain(user.getDomainId());
        verifyDomainAccess(domain);
        verifyRoleAccess(user, scopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserRole));
    }

    @Override
    public boolean isDefaultUser(User user) {
        // This method returns whether or not a user is a default user
        // A default user is defined as one that has the identity:default role
        // this includes a user that also has the identity:user-manage role
        return containsRole(user, Arrays.asList(cloudUserRole));
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
    public void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess) {
        BaseUser user = userService.getUserByScopeAccess(authScopeAccess);
        if (authorizeRoleAccess(user, authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole))) {
            return;
        }

        List<Tenant> adminTenants = tenantService.getTenantsForScopeAccessByTenantRoles(authScopeAccess);

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

        String userRoleName = identityTypeRole.getName();
        if (identityConfig.getIdentityServiceAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.SERVICE_ADMIN;}
        if (identityConfig.getIdentityIdentityAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.IDENTITY_ADMIN;}
        if (identityConfig.getIdentityUserAdminRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.USER_ADMIN;}
        if (identityConfig.getIdentityUserManagerRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.USER_MANAGER;}
        if (identityConfig.getIdentityDefaultUserRoleName().equals(userRoleName)) { return IdentityUserTypeEnum.DEFAULT_USER;}

        throw new IllegalArgumentException(String.format("Unrecognized identity classification role '%s'", userRoleName));
    }

    public IdentityUserTypeEnum getIdentityTypeRoleAsEnum(BaseUser baseUser) {
        if (baseUser == null) {
            return null;
        } else if (baseUser instanceof Racker) {
            return null;
        } else if (baseUser instanceof FederatedUser) {
            return IdentityUserTypeEnum.DEFAULT_USER; //efficiency. Fed users are hardcoded to be default users
        } else if (!(baseUser instanceof User)) {
            throw new IllegalStateException(String.format("Unknown user type '%s'", baseUser.getClass().getName()));
        }

        User user = (User) baseUser;

        ClientRole identityRole = applicationService.getUserIdentityRole(user);

        return identityRole != null ? getIdentityTypeRoleAsEnum(identityRole) : null;
    }

    public IdentityUserTypeEnum getIdentityTypeRoleAsEnum(ServiceCatalogInfo serviceCatalogInfo) {
        List<TenantRole> userRoles = serviceCatalogInfo.getUserTenantRoles();
        ImmutableClientRole userLowestWeightIdentityRole = null;

        for (TenantRole tenantRole : userRoles) {
            ImmutableClientRole clientRole = identityRoleIdToRoleMap.get(tenantRole.getRoleRsId());

            //verify is identity role (client role not null) && role weight of this client role is < role weight of
            //current lowest weight client role)
            if (clientRole != null && (userLowestWeightIdentityRole == null
                    || clientRole.getRsWeight() < userLowestWeightIdentityRole.getRsWeight())) {
                userLowestWeightIdentityRole = clientRole;
            }
        }

        return userLowestWeightIdentityRole != null ? getIdentityTypeRoleAsEnum(userLowestWeightIdentityRole.asClientRole()) : null;
    }


    private boolean authorize(BaseUser user, ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return false;
        }

        return containsRole(user, clientRoles);
    }

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
    public boolean restrictUserAuthentication(EndUser user, ServiceCatalogInfo serviceCatalogInfo) {
        if(identityConfig.getReloadableConfig().getFeatureUserDisabledByTenantsEnabled()
                && serviceCatalogInfo.allTenantsDisabled()) {

            //identity-admins+ bypass terminator restriction
            IdentityUserTypeEnum userType = getIdentityTypeRoleAsEnum(serviceCatalogInfo);
            if(userType != null && !userType.hasAtLeastIdentityAdminAccessLevel()) {
                return true;
            }
        }
        return false;
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

    public ClientRole getCloudServiceAdminRole() {
        return cloudServiceAdminRole;
    }

    public void setCloudServiceAdminRole(ClientRole cloudServiceAdminRole) {
        this.cloudServiceAdminRole = cloudServiceAdminRole;
    }

    public ClientRole getRackerRole() {
        return rackerRole;
    }

    public  void setRackerRole(ClientRole rackerRole) {
        this.rackerRole = rackerRole;
    }

    public  ClientRole getCloudIdentityAdminRole() {
        return cloudIdentityAdminRole;
    }

    public  void setCloudIdentityAdminRole(ClientRole cloudIdentityAdminRole) {
        this.cloudIdentityAdminRole = cloudIdentityAdminRole;
    }

    public  ClientRole getCloudUserAdminRole() {
        return cloudUserAdminRole;
    }

    public  void setCloudUserAdminRole(ClientRole cloudUserAdminRole) {
        this.cloudUserAdminRole = cloudUserAdminRole;
    }

    public  ClientRole getCloudUserRole() {
        return cloudUserRole;
    }

    public  void setCloudUserRole(ClientRole cloudUserRole) {
        this.cloudUserRole = cloudUserRole;
    }

    public  ClientRole getIdmSuperAdminRole() {
        return idmSuperAdminRole;
    }

    public  void setIdmSuperAdminRole(ClientRole idmSuperAdminRole) {
        this.idmSuperAdminRole = idmSuperAdminRole;
    }

    public ClientRole getCloudUserManagedRole() {
        return cloudUserManagedRole;
    }

    public void setCloudUserManagedRole(ClientRole cloudUserManagedRole) {
        this.cloudUserManagedRole = cloudUserManagedRole;
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
