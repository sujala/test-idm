package com.rackspace.idm.domain.service.federation.v2;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.predicate.UserEnabledPredicate;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.rackspace.idm.ErrorCodes.*;

/**
 * Handles SAML authentication requests against provisioned users. This means against a particular domain.
 */
@Component
public class FederatedDomainRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FederatedDomainRequestHandler.class);
    private static final BigDecimal NUMBER_SEC_IN_HOUR = BigDecimal.valueOf(3600);
    public static final String DISABLED_DOMAIN_ERROR_MESSAGE = "Domain %s is disabled.";
    public static final String DUPLICATE_USERNAME_ERROR_MSG = "The username already exists under a different domainId for this identity provider";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private DomainService domainService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private FederatedUserDao federatedUserDao;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    public SamlAuthResponse processAuthRequestForProvider(FederatedDomainAuthRequest authRequest, IdentityProvider originIdentityProvider) {
        // Just a few sanity checks
        Validate.notNull(authRequest, "request must not be null");
        Validate.notNull(originIdentityProvider, "Origin IDP must not be null");
        Validate.isTrue(originIdentityProvider.getFederationTypeAsEnum() == IdentityProviderFederationTypeEnum.DOMAIN, "Provider must be a DOMAIN provider");

        validateRequestedToken(authRequest);

        /*
        Authorize the idp for this domain. Must be:
        1. A global provider where approvedDomainGroup == GLOBAL
        2. Contains the domain as part of approved Domains

        Do this prior to checking for domain existence to prevent leaking whether a given domain exists and to avoid a
        directory search when IDP isn't authorized even if the domain existed
         */
        if (!(originIdentityProvider.isApprovedForDomain(authRequest.getDomainId()))) {
            throw new ForbiddenException(String.format("Not authorized for domain '%s'", authRequest.getDomainId()), ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }

        /*
         Validate the domain has an enabled user-admin. Domains without an enabled user-admin can not be authenticated
         against
         */
        Domain requestedDomain = validateRequestedDomain(authRequest);
        User domainUserAdmin = validateAndRetrieveUserAdminForDomain(authRequest.getDomainId());

        // Validate the requested roles exist
        List<TenantRole> requestedRoles = validateAndGenerateRequestedRoles(authRequest);

        FederatedUser federatedUser = processUserForRequest(authRequest, requestedRoles, domainUserAdmin, originIdentityProvider);

        UserScopeAccess token = createToken(federatedUser, authRequest);

        ServiceCatalogInfo serviceCatalogInfo = scopeAccessService.getServiceCatalogInfo(federatedUser);
        List<TenantRole> tenantRoles = serviceCatalogInfo.getUserTenantRoles();

        // Verify Terminator use case and blank out catalog if necessary
        List<OpenstackEndpoint> endpoints = serviceCatalogInfo.getUserEndpoints();
        if (authorizationService.restrictUserAuthentication(serviceCatalogInfo)) {
            endpoints = Collections.EMPTY_LIST;
        }

        return new SamlAuthResponse(federatedUser, tenantRoles, endpoints, token);
    }

    private void validateRequestedToken(FederatedDomainAuthRequest authRequest) {
        DateTime requestedExp = authRequest.getRequestedTokenExpiration();
        DateTime now = new DateTime();

        // TODO: Eventually may want to allow for some clock skew from originating system
        int maxTokenLifetime = identityConfig.getReloadableConfig().getFederatedDomainTokenLifetimeMax();
        int timeDelta = Seconds.secondsBetween(now, requestedExp).getSeconds();
        if (timeDelta > maxTokenLifetime) {
            // Convert max lifetime in seconds to hours for error message
            BigDecimal maxSec = BigDecimal.valueOf(maxTokenLifetime);
            BigDecimal maxD = maxSec.divide(NUMBER_SEC_IN_HOUR, 1, BigDecimal.ROUND_DOWN);
            throw new BadRequestException(String.format("Invalid requested token expiration. " +
                    "Tokens cannot be requested with an expiration of more than '%s' hours.", maxD.toPlainString()), ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP);
        }
    }

    /**
     * Returns the validated domain
     *
     * @return
     */
    private Domain validateRequestedDomain(FederatedDomainAuthRequest authRequest) {
        String requestedDomain = authRequest.getDomainId();

        Domain domain = domainService.getDomain(requestedDomain);
        if (domain == null) {
            throw new BadRequestException(String.format("Domain '%s' does not exist.", requestedDomain), ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }
        else if (!domain.getEnabled()) {
            throw new BadRequestException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, requestedDomain), ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }

        return domain;
    }

    private User validateAndRetrieveUserAdminForDomain(String domainId) {
        List<User> userAdmins = domainService.getDomainAdmins(domainId);
        if(userAdmins.size() == 0) {
            log.error("Unable to get roles for saml assertion due to no user admin for domain {}", domainId);
            throw new ForbiddenException("The specified domain can not be used for federation. It does not have an owner", ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }

        if(userAdmins.size() > 1 && identityConfig.getStaticConfig().getDomainRestrictedToOneUserAdmin()) {
            log.error("Unable to get roles for saml assertion due to more than one user admin for domain {}", domainId);
            throw new IllegalStateException(String.format("More than one user admin exists for domain %s", domainId));
        }

        User firstEnabledUserAdmin = org.apache.commons.collections4.CollectionUtils.find(userAdmins, new UserEnabledPredicate());
        if(firstEnabledUserAdmin == null) {
            throw new ForbiddenException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, domainId), ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }
        return firstEnabledUserAdmin;
    }

    private List<TenantRole> validateAndGenerateRequestedRoles(FederatedDomainAuthRequest authRequest) {
        Set<String> requestedRoles = authRequest.getRoleNames();

        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        if (CollectionUtils.isNotEmpty(requestedRoles)) {
            for (String roleName : requestedRoles) {
                //TODO: Candidate for caching...
                ClientRole role = roleService.getRoleByName(roleName);
                if (role == null || role.getRsWeight() != PrecedenceValidator.RBAC_ROLES_WEIGHT
                        || role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.TENANT) {
                    throw new BadRequestException(String.format("Invalid role '%s'", roleName), ERROR_CODE_FEDERATION2_FORBIDDEN_FEDERATED_ROLE);
                }

                // Create a new globally assigned tenant role
                TenantRole tenantRole = new TenantRole();
                tenantRole.setRoleRsId(role.getId());
                tenantRole.setClientId(role.getClientId());
                tenantRole.setName(role.getName());
                tenantRole.setDescription(role.getDescription());
                tenantRoles.add(tenantRole);
            }
        }
        return tenantRoles;
    }

    /**
     * If the user already exists, will return a _new_ user object representing the stored user. If the user does not exist, a new user is created in the backend, but the provided
     * user object is altered (updated with ID).
     * <p/>
     * Will also delete expired tokens if the user already existed
     *
     * @param authRequest
     * @return
     */
    private FederatedUser processUserForRequest(FederatedDomainAuthRequest authRequest, List<TenantRole> requestedRoles, User domainUserAdmin, IdentityProvider originIdp) {
        FederatedUser existingUser = federatedUserDao.getUserByUsernameForIdentityProviderId(authRequest.getUsername(), originIdp.getProviderId());
        if (existingUser != null && !authRequest.getDomainId().equalsIgnoreCase(existingUser.getDomainId())) {
            throw new DuplicateUsernameException(DUPLICATE_USERNAME_ERROR_MSG, ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE);
        }

        /*
          If the existing user is expired (no expiration time or expiration is in the past), delete the user and create
          a new one. Given the automated purge process there is a chance that the purge federated user application could
          delete this user before it was updated causing an error.
         */
        if (existingUser != null && (existingUser.getExpiredTimestamp() == null || new DateTime().isAfter(new DateTime(existingUser.getExpiredTimestamp())))) {
            try {
                identityUserService.deleteUser(existingUser);
            } catch (Exception e) {
                // Ignore error as we're just trying to clean up. Perhaps was already deleted via purge
                log.info("Encountered error deleting an expired federated user. Ignoring error", e);
            }
            existingUser = null;
        }

        if (existingUser == null) {
            validateMaxUserCountInDomain(authRequest, originIdp);
            existingUser = createNewFederatedUser(authRequest, requestedRoles, domainUserAdmin, originIdp);
        }  else {
            existingUser = reconcileExistingUser(authRequest, requestedRoles, existingUser);
        }

        return existingUser;
    }

    /**
     * Reconciles the federated user provided in the latest auth w/ the shell user stored within backend. Updates the
     * shell user to reflect the latest state provided by the IDP.
     *
     * @param authRequest
     * @param requestedRoles
     * @param existingUser
     * @return
     */
    private FederatedUser reconcileExistingUser(FederatedDomainAuthRequest authRequest, List<TenantRole> requestedRoles, FederatedUser existingUser) {
         /*
        If there is an existing user, must verify that the domains are the same.
         */
        if (!authRequest.getDomainId().equalsIgnoreCase(existingUser.getDomainId())) {
            throw new DuplicateUsernameException(DUPLICATE_USERNAME_ERROR_MSG);
        }

        /*
        An existing user may or may not need to be updated. Only update the user when necessary
         */
        boolean updateUser = false;

        // Update email if necessary
        if (!authRequest.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            existingUser.setEmail(authRequest.getEmail());
            updateUser = true;
        }

        // Update user expiration if necessary
        if (existingUser.getExpiredTimestamp() == null ||
                new DateTime(existingUser.getExpiredTimestamp()).isBefore(authRequest.getRequestedTokenExpiration())) {
            existingUser.setExpiredTimestamp(calculateUserExpirationFromTokenExpiration(authRequest.getRequestedTokenExpiration()).toDate());
            updateUser = true;
        }

        if (updateUser) {
            federatedUserDao.updateUser(existingUser);
        }

        // Update roles as necessary
        reconcileRequestedRbacRolesFromRequest(existingUser, requestedRoles);

        return existingUser;
    }

    /**
     * Reconcile the requested set of RBAC roles specified by the IDP to those on the existing user.
     * Must:
     * 1. Remove those RBAC roles that were not included by IDP, but are on the existing user
     * 2. Add those roles that were included by IDP, but are on the existing user
     *
     * This makes the assumption that RBAC roles that can be assigned via federation are NOT propagating roles (which
     * they never should be). Otherwise this method could result in propagating roles being removed when they shouldn't be.
     *
     * @param existingFederatedUser
     * @param desiredRbacRolesOnUser
     */
    private void reconcileRequestedRbacRolesFromRequest(FederatedUser existingFederatedUser, List<TenantRole> desiredRbacRolesOnUser) {
        Map<String, TenantRole> desiredRbacRoleMap = new HashMap<String, TenantRole>();
        for (TenantRole tenantRole : desiredRbacRolesOnUser) {
            desiredRbacRoleMap.put(tenantRole.getName(), tenantRole);
        }

        List<TenantRole> existingRbacRolesOnUser = tenantService.getRbacRolesForUser(existingFederatedUser);
        Map<String, TenantRole> existingRbacRoleMap = new HashMap<String, TenantRole>();
        for (TenantRole tenantRole : existingRbacRolesOnUser) {
            existingRbacRoleMap.put(tenantRole.getName(), tenantRole);
        }

        Collection<String> add = ListUtils.removeAll(desiredRbacRoleMap.keySet(), existingRbacRoleMap.keySet());
        Collection<String> remove = ListUtils.removeAll(existingRbacRoleMap.keySet(), desiredRbacRoleMap.keySet());

        // Remove roles that user should no longer have
        for (String roleToRemove : remove) {
            tenantService.deleteGlobalRole(existingRbacRoleMap.get(roleToRemove));
        }

        // Add roles that user should have
        for (String roleToAdd : add) {
            tenantService.addTenantRoleToUser(existingFederatedUser, desiredRbacRoleMap.get(roleToAdd));
        }

        existingFederatedUser.setRoles(desiredRbacRolesOnUser);
    }

    private FederatedUser createNewFederatedUser(FederatedDomainAuthRequest authRequest, List<TenantRole> requestedRoles, User domainUserAdmin, IdentityProvider originIdp) {
        List<TenantRole> userRoles = calculateStandardRolesForDomainUser(authRequest, domainUserAdmin);
        userRoles.addAll(requestedRoles);

        FederatedUser federatedUser = new FederatedUser();
        federatedUser.setUsername(authRequest.getUsername());
        federatedUser.setDomainId(authRequest.getDomainId());
        federatedUser.setEmail(authRequest.getEmail());
        federatedUser.setFederatedIdpUri(authRequest.getOriginIssuer());
        federatedUser.setExpiredTimestamp(calculateUserExpirationFromTokenExpiration(authRequest.getRequestedTokenExpiration()).toDate());
        federatedUser.setRegion(domainUserAdmin.getRegion());

        for (String groupId : domainUserAdmin.getRsGroupId()) {
            federatedUser.getRsGroupId().add(groupId);
        }

        federatedUserDao.addUser(originIdp, federatedUser);
        tenantService.addTenantRolesToUser(federatedUser, userRoles);

        return federatedUser;
    }

    /**
     * Determine the roles that all federated users within the specified user-admin's domain will have
     *
     * @return
     */
    private List<TenantRole> calculateStandardRolesForDomainUser(FederatedDomainAuthRequest request, User userAdmin) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();

        // Add in default role which is added to all federated users
        ImmutableClientRole roleObj = authorizationService.getCachedIdentityRoleByName(IdentityUserTypeEnum.DEFAULT_USER.getRoleName());
        TenantRole tenantRole = new TenantRole();
        tenantRole.setRoleRsId(roleObj.getId());
        tenantRole.setClientId(roleObj.getClientId());
        tenantRole.setName(roleObj.getName());
        tenantRole.setDescription(roleObj.getDescription());
        tenantRoles.add(tenantRole);

        // Add in the propagating roles
        tenantRoles.addAll(getDomainPropagatingRoles(userAdmin));

        return tenantRoles;
    }

    /**
     * Retrieve the propagating roles associated with the domain by retrieving the propagating roles assigned to the user-admin.
     * <p/>
     * Note - yes, such roles probably should be attached to the domain rather than the user-admin, but that's how propagating
     * roles were implemented so that's why we retrieve them off the user-admin instead of the domain. :)
     *
     * @param userAdmin
     * @return
     */
    private List<TenantRole> getDomainPropagatingRoles(User userAdmin) {
        List<TenantRole> propagatingRoles = new ArrayList<TenantRole>();
        for (TenantRole role : tenantService.getTenantRolesForUser(userAdmin)) {
            if (role.getPropagate()) {
                role.setUniqueId(null);
                role.setUserId(null);
                propagatingRoles.add(role);
            }
        }
        return propagatingRoles;
    }

    /**
     * Determine when the new/updated user will expire
     *
     * @param tokenExpiration
     * @return
     */
    private DateTime calculateUserExpirationFromTokenExpiration(DateTime tokenExpiration) {
        final int futureExpirationSeconds = identityConfig.getReloadableConfig().getFederatedDeltaExpiration();
        DateTime userExpirationDate = tokenExpiration.plusSeconds(futureExpirationSeconds);
        return userExpirationDate;
    }

    /**
     * Validates there is room to create a new user within the IDP's user limits
     *
     * @param authRequest
     */
    private void validateMaxUserCountInDomain(FederatedDomainAuthRequest authRequest, IdentityProvider originIdp) {
        final Integer maxUserCount = identityConfig.getReloadableConfig().getIdentityFederationMaxUserCountPerDomainForIdp(originIdp.getUri());
        if (maxUserCount != null && maxUserCount > 0) {
            final String providerId = originIdp.getProviderId();
            final String domainId = authRequest.getDomainId();
            final int count = identityUserService.getUnexpiredFederatedUsersByDomainIdAndIdentityProviderNameCount(domainId, providerId);
            if (count >= maxUserCount) {
                throw new ForbiddenException(String.format("Maximum number of users reached for domain '%s'", domainId), ERROR_CODE_FEDERATION2_FORBIDDEN_REACHED_MAX_USERS_LIMIT);
            }
        }
    }

    private UserScopeAccess createToken(FederatedUser user, FederatedDomainAuthRequest authRequest) {
        UserScopeAccess token = new UserScopeAccess();
        token.setUserRsId(user.getId());
        token.setAccessTokenString(scopeAccessService.generateToken()); // Will get replaced w/ AE token during save
        token.setAccessTokenExp(authRequest.getRequestedTokenExpiration().toDate());
        token.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());

        // ALL Domain IDP Auth get mapped to FEDERATION + add in whatever IDP specified
        token.getAuthenticatedBy().add(AuthenticatedByMethodEnum.FEDERATION.getValue());
        token.getAuthenticatedBy().add(authRequest.getAuthenticatedByForRequest().getValue());

        scopeAccessService.addUserScopeAccess(user, token);

        return token;
    }
}
