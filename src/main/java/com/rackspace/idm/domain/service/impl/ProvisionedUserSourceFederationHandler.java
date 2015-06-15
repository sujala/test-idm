package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.SAMLConstants;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedUserRequest;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.util.SamlSignatureValidator;
import com.rackspace.idm.util.predicate.UserEnabledPredicate;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Handles SAML authentication requests against provisioned users. This means against a particular domain.
 */
@Component
public class ProvisionedUserSourceFederationHandler implements FederationHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);
    public static final String DUPLICATE_USERNAME_ERROR_MSG = "The username already exists under a different domainId for this identity provider";
    public static final String DISABLED_DOMAIN_ERROR_MESSAGE = "Domain %s is disabled.";

    @Autowired
    SamlSignatureValidator samlSignatureValidator;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    DomainDao domainDao;

    @Autowired
    PrecedenceValidator precedenceValidator;

    @Autowired
    IdentityUserService identityUserService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    IdentityProviderDao identityProviderDao;

    @Autowired
    FederatedUserDao federatedUserDao;

    @Autowired
    ApplicationRoleDao roleDao;

    @Autowired
    ScopeAccessService scopeAccessService;

    @Autowired
    TenantService tenantService;

    @Autowired
    DomainService domainService;

    @Autowired
    RoleService roleService;

    @Autowired
    UserService userService;

    @Autowired
    GroupService groupService;

    @Autowired
    private Configuration config;

    public SamlAuthResponse processRequestForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        Validate.notNull(samlResponseDecorator, "saml response must not be null");
        Validate.notNull(provider, "provider must not be null");

        TargetUserSourceEnum targetUserSourceEnum = provider.getTargetUserSourceAsEnum();
        if (targetUserSourceEnum != TargetUserSourceEnum.PROVISIONED) {
            throw new IllegalStateException(String.format("Invalid target user source for provisioned user federation", targetUserSourceEnum));
        }

        samlResponseDecorator.checkAndGetAssertion();

        FederatedUserRequest request = parseAndValidateSaml(samlResponseDecorator, provider);

        FederatedUser user = processUserForRequest(request);
        UserScopeAccess token = createToken(user, request.getRequestedTokenExpirationDate());
        ServiceCatalogInfo serviceCatalogInfo = scopeAccessService.getServiceCatalogInfo(user);
        List<TenantRole> tenantRoles = serviceCatalogInfo.getUserTenantRoles();

        //verify the user is allowed to login
        List<OpenstackEndpoint> endpoints = serviceCatalogInfo.getUserEndpoints();
        if (authorizationService.restrictUserAuthentication(user, serviceCatalogInfo)) {
            endpoints = Collections.EMPTY_LIST;
        }

        return new SamlAuthResponse(user, tenantRoles, endpoints, token);
    }

    /**
     * Validate the samlResponse contains the required data. In addition it verifies the specified issuer exists, the
     * signature validates against that issuer, and the domain exists.
     * - a valid issuer
     *
     * @param samlResponseDecorator
     */
    private FederatedUserRequest parseAndValidateSaml(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider) {
        //populate a federated user object based on saml data
        FederatedUserRequest request = new FederatedUserRequest();
        request.setIdentityProvider(provider);

        request.setUser(new FederatedUser());
        request.getUser().setFederatedIdpUri(provider.getUri());

        //validate assertion
        validateSamlAssertionAndPopulateRequest(samlResponseDecorator, request);

        return request;
    }

    private void validateSamlAssertionAndPopulateRequest(SamlResponseDecorator samlResponseDecorator, FederatedUserRequest request) {
        if (samlResponseDecorator.getSamlResponse().getAssertions() == null || samlResponseDecorator.getSamlResponse().getAssertions().size() == 0) {
            throw new BadRequestException("No Assertions specified");
        }

        request.getUser().setUsername(samlResponseDecorator.checkAndGetUsername());
        request.setRequestedTokenExpirationDate(samlResponseDecorator.checkAndGetSubjectConfirmationNotOnOrAfterDate());

        //validate and populate domain
        validateSamlDomainAndPopulateRequest(samlResponseDecorator, request);

        //validate and populate email
        validateSamlEmailAndPopulateRequest(samlResponseDecorator, request);

        validateRolesAndPopulateRequest(samlResponseDecorator.getAttribute(SAMLConstants.ATTR_ROLES), request);
    }

    /**
     * Returns the validated domain
     *
     * @return
     */
    private void validateSamlDomainAndPopulateRequest(SamlResponseDecorator decoratedResponse, FederatedUserRequest request) {
        List<String> domains = decoratedResponse.getAttribute(SAMLConstants.ATTR_DOMAIN);

        if (domains == null || domains.size() == 0) {
            throw new BadRequestException("Domain attribute is not specified");
        }

        if (domains.size() > 1) {
            throw new BadRequestException("Multiple domains specified");
        }

        String requestedDomain = domains.get(0);
        Domain domain = domainDao.getDomain(requestedDomain);
        if (domain == null) {
            throw new BadRequestException("Domain '" + requestedDomain + "' does not exist.");
        }
        else if (!domain.getEnabled()) {
            throw new BadRequestException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, requestedDomain));
        }

        List<User> userAdmins = domainService.getDomainAdmins(domain.getDomainId());

        if(userAdmins.size() == 0) {
            log.error("Unable to get roles for saml assertion due to no user admin for domain {}", domain.getDomainId());
            throw new IllegalStateException("no user admin exists for domain " + domain.getDomainId());
        }

        if(userAdmins.size() > 1 && identityConfig.getStaticConfig().getDomainRestrictedToOneUserAdmin()) {
            log.error("Unable to get roles for saml assertion due to more than one user admin for domain {}", domain.getDomainId());
            throw new IllegalStateException("more than one user admin exists for domain " + domain.getDomainId());
        }

        boolean enabledUserAdmin = org.apache.commons.collections4.CollectionUtils.exists(userAdmins, new UserEnabledPredicate());
        if(!enabledUserAdmin) {
            throw new BadRequestException(String.format(DISABLED_DOMAIN_ERROR_MESSAGE, requestedDomain));
        }

        int numberUsersInDomainAndIdp = identityUserService.getFederatedUsersByDomainIdAndIdentityProviderNameCount(requestedDomain, request.getIdentityProvider().getName());
        if (numberUsersInDomainAndIdp >= getMaxNumberUsersPerDomainAndIdp()) {
            String errMsg = String.format("Cannot create more than %d federated users for an account within an identity provider.", getMaxNumberUsersPerDomainAndIdp());
            throw new BadRequestException(errMsg);
        }

        request.getUser().setDomainId(domain.getDomainId());
    }

    /**
     * Validates and sets the email on the provided federateduserrequest.
     *
     * @return
     */
    private void validateSamlEmailAndPopulateRequest(SamlResponseDecorator decoratedResponse, FederatedUserRequest request) {
        List<String> emails = decoratedResponse.getAttribute(SAMLConstants.ATTR_EMAIL);

        if (emails == null || emails.size() == 0) {
            throw new BadRequestException("Email attribute is not specified");
        }

        if (emails.size() > 1) {
            throw new BadRequestException("Multiple emails specified");
        }

        String email = emails.get(0);

        request.getUser().setEmail(email);
    }

    private void validateRolesAndPopulateRequest(List<String> roleNames, FederatedUserRequest request) {
        if (CollectionUtils.isNotEmpty(roleNames)) {
            Map<String, TenantRole> roles = new HashMap<String, TenantRole>();
            for (String roleName : roleNames) {
                if (roles.containsKey(roleName)) {
                    throw new BadRequestException("role '" + roleName + "' specified more than once");
                }

                //TODO: Candidate for caching...
                ClientRole role = roleService.getRoleByName(roleName);
                if (role == null || role.getRsWeight() != PrecedenceValidator.RBAC_ROLES_WEIGHT) {
                    throw new BadRequestException("Invalid role '" + roleName + "'");
                }

                request.getRequestClientRoleCache().put(roleName, role);

                //create a new global role to add
                TenantRole tenantRole = new TenantRole();
                tenantRole.setRoleRsId(role.getId());
                tenantRole.setClientId(role.getClientId());
                tenantRole.setName(role.getName());
                tenantRole.setDescription(role.getDescription());
                roles.put(roleName, tenantRole);
            }
            request.getUser().getRoles().addAll(roles.values());
        }
    }

    private int getMaxNumberUsersPerDomainAndIdp() {
        return config.getInt("maxNumberOfFederatedUsersInDomainPerIdp", 1000);
    }

    /**
     * If the user already exists, will return a _new_ user object representing the stored user. If the user does not exist, a new user is created in the backend, but the provided
     * user object is altered (updated with ID).
     * <p/>
     * Will also delete expired tokens if the user already existed
     *
     * @param request
     * @return
     */
    private FederatedUser processUserForRequest(FederatedUserRequest request) {
        FederatedUser resultUser = getFederatedUserForIdp(request.getUser().getUsername(), request.getIdentityProvider());
        if (resultUser == null) {
            resultUser = createUserForRequest(request);
        } else if (!request.getUser().getDomainId().equalsIgnoreCase(resultUser.getDomainId())) {
            throw new DuplicateUsernameException(DUPLICATE_USERNAME_ERROR_MSG);
        } else {
            //update email if necessary
            if (!request.getUser().getEmail().equalsIgnoreCase(resultUser.getEmail())) {
                resultUser.setEmail(request.getUser().getEmail());
                federatedUserDao.updateUser(resultUser);
            }

            //update roles as necessary
            reconcileRequestedRbacRolesFromRequest(resultUser, request.getUser().getRoles());

            //clean up any expired tokens
            scopeAccessService.deleteExpiredTokensQuietly(resultUser);
        }

        return resultUser;
    }

    /**
     * Reconcile the requested set of RBAC roles included in the initial saml response to those on the existing user.
     * <p/>
     * Must remove those RBAC roles that are not present in the saml response, and add those in the response that are not
     * not on the user.
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

        //remove roles that user should no longer have
        for (String roleToRemove : remove) {
            tenantService.deleteGlobalRole(existingRbacRoleMap.get(roleToRemove));
        }

        //add roles that user should have
        for (String roleToAdd : add) {
            tenantService.addTenantRoleToUser(existingFederatedUser, desiredRbacRoleMap.get(roleToAdd));
        }

        existingFederatedUser.setRoles(desiredRbacRolesOnUser);
    }

    /**
     * Retrieve the existing user for the specified username and IDP.
     *
     * @param username
     * @param provider
     * @return
     */
    private FederatedUser getFederatedUserForIdp(String username, IdentityProvider provider) {
        FederatedUser user = federatedUserDao.getUserByUsernameForIdentityProviderName(username, provider.getName());
        return user;
    }

    private FederatedUser createUserForRequest(FederatedUserRequest request) {
        FederatedUser userToCreate = request.getUser();

        List<User> domainUserAdmins = domainService.getDomainAdmins(userToCreate.getDomainId());

        String defaultRegion = calculateDefaultRegionForNewUserRequest(request, domainUserAdmins);
        userToCreate.setRegion(defaultRegion);

        List<TenantRole> tenantRoles = calculateRolesForNewUserRequest(request, domainUserAdmins);

        for (String groupId : domainUserAdmins.get(0).getRsGroupId()) {
            userToCreate.getRsGroupId().add(groupId);
        }

        federatedUserDao.addUser(request.getIdentityProvider(), userToCreate);
        tenantService.addTenantRolesToUser(userToCreate, tenantRoles);

        return userToCreate;
    }

    /**
     * Determine the region for the new user based on the passed in request and the user-admins. For now, if multiple
     * userAdmins for the domain exist (and this is allowed per
     * configuration), the first userAdmin returned will be used to determine the default region.
     *
     * @param request
     * @param userAdmins
     * @return
     */
    private String calculateDefaultRegionForNewUserRequest(FederatedUserRequest request, List<User> userAdmins) {
        Assert.notEmpty(userAdmins, "Must provide at least one user admin!");
        return userAdmins.get(0).getRegion();
    }

    /**
     * Determine the roles that the new federated user should receive
     *
     * @return
     */
    private List<TenantRole> calculateRolesForNewUserRequest(FederatedUserRequest request, List<User> userAdmins) {
        FederatedUser userToCreate = request.getUser();

        //get the roles that should be added to the user
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();

        //add in default role which is added to all federated users
        //TODO: Candidate for caching...
        ClientRole roleObj = roleDao.getRoleByName("identity:default");
        TenantRole tenantRole = new TenantRole();
        tenantRole.setRoleRsId(roleObj.getId());
        tenantRole.setClientId(roleObj.getClientId());
        tenantRole.setName(roleObj.getName());
        tenantRole.setDescription(roleObj.getDescription());
        tenantRoles.add(tenantRole);

        //add in the propagating roles
        tenantRoles.addAll(getDomainPropagatingRoles(userAdmins));

    /*
    add in the saml requested roles
     */
        tenantRoles.addAll(userToCreate.getRoles());

        return tenantRoles;
    }

    private UserScopeAccess createToken(FederatedUser user, DateTime requestedExpirationDate) {
        UserScopeAccess token = new UserScopeAccess();
        token.setUserRsId(user.getId());
        token.setAccessTokenString(scopeAccessService.generateToken());
        token.setAccessTokenExp(requestedExpirationDate.toDate());
        token.setClientId(identityConfig.getStaticConfig().getCloudAuthClientId());
        token.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION);

        scopeAccessService.addUserScopeAccess(user, token);

        return token;
    }

    /**
     * Retrieve the propagating roles associated with the domain by retrieving the propagating roles assigned to the user-admin.
     * <p/>
     * Note - yes, such roles probably should be attached to the domain rather than the user-admin, but that's how propagating
     * roles were implemented so that's why we retrieve them off the user-admin instead of the domain. :)
     *
     * @param domainUserAdmins
     * @return
     */
    private List<TenantRole> getDomainPropagatingRoles(List<User> domainUserAdmins) {
        List<TenantRole> propagatingRoles = new ArrayList<TenantRole>();

        for (User userAdmin : domainUserAdmins) {
            for (TenantRole role : tenantService.getTenantRolesForUser(userAdmin)) {
                if (role.getPropagate()) {
                    role.setLdapEntry(null);
                    role.setUserId(null);
                    propagatingRoles.add(role);
                }
            }
        }
        return propagatingRoles;
    }
}
