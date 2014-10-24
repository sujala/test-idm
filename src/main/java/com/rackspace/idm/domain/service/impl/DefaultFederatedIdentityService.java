package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.v20.federated.FederatedUserRequest;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.util.SamlResponseValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

/**
 * This class is responsible for handling identity operations related
 * to identities that do not exist locally in the directory store
 *
 */
@Component
public class DefaultFederatedIdentityService implements FederatedIdentityService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);
    public static final String DUPLICATE_USERNAME_ERROR_MSG = "The username already exists under a different domainId for this identity provider";

    @Autowired
    SamlResponseValidator samlResponseValidator;

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

    @Override
    public AuthData processSamlResponse(Response response) {
        SamlResponseDecorator decoratedSamlResponse = new SamlResponseDecorator(response);
        FederatedUserRequest request = samlResponseValidator.validateAndPopulateRequest(decoratedSamlResponse);

        FederatedUser user = processUserForRequest(request);

        UserScopeAccess token = createToken(user, request.getRequestedTokenExpirationDate());
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(token);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(user);

        return getAuthData(user, token, endpoints, tenantRoles);
    }

    @Override
    public AuthData getAuthenticationInfo(UserScopeAccess token) {
        FederatedUser user = federatedUserDao.getUserById(token.getUserRsId());
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(user);

        return getAuthData(user, token, null, tenantRoles);
    }

    /**
     * If the user already exists, will return a _new_ user object representing the stored user. If the user does not exist, a new user is created in the backend, but the provided
     * user object is altered (updated with ID).
     *
     * Will also delete expired tokens if the user already existed
     *
     * @param request
     * @return
     */
    private FederatedUser processUserForRequest(FederatedUserRequest request) {
        FederatedUser resultUser = getFederatedUserForIdp(request.getFederatedUser().getUsername(), request.getIdentityProvider());
        if (resultUser == null) {
            resultUser = createUserForRequest(request);
        } else if (!request.getFederatedUser().getDomainId().equalsIgnoreCase(resultUser.getDomainId())) {
            throw new DuplicateUsernameException(DUPLICATE_USERNAME_ERROR_MSG);
        } else {
            //update email if necessary
            if (!request.getFederatedUser().getEmail().equalsIgnoreCase(resultUser.getEmail())) {
                resultUser.setEmail(request.getFederatedUser().getEmail());
                federatedUserDao.updateUser(resultUser);
            }

            //update roles as necessary
            reconcileRequestedRbacRolesFromRequest(resultUser, request.getFederatedUser().getRoles());

            //clean up any expired tokens
            scopeAccessService.deleteExpiredTokensQuietly(resultUser);
        }

        return resultUser;
    }

    /**
     * Reconcile the requested set of RBAC roles included in the initial saml response to those on the existing user.
     *
     * Must remove those RBAC roles that are not present in the saml response, and add those in the response that are not
     * not on the user.
     *
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
        FederatedUser userToCreate = request.getFederatedUser();

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
     * @return
     */
    private List<TenantRole> calculateRolesForNewUserRequest(FederatedUserRequest request, List<User> userAdmins) {
        FederatedUser userToCreate = request.getFederatedUser();

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
        token.setAccessTokenString(generateToken());
        token.setAccessTokenExp(requestedExpirationDate.toDate());
        token.setClientId(getCloudAuthClientId());
        token.getAuthenticatedBy().add(GlobalConstants.AUTHENTICATED_BY_FEDERATION);

        scopeAccessService.addUserScopeAccess(user, token);

        return token;
    }

    private AuthData getAuthData(FederatedUser user, UserScopeAccess token, List<OpenstackEndpoint> endpoints, List<TenantRole> tenantRoles) {
        AuthData authData = new AuthData();
        authData.setToken(token);
        authData.setUser(user);
        authData.setEndpoints(endpoints);

        token.setRoles(tenantRoles);
        user.setRoles(tenantRoles);

        return authData;
    }

    private List<TenantRole> convertSamlProvidedRolesToTenantRoles(List<String> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();

        //get the roles passed in with the saml assertion and add as global roles
        if (CollectionUtils.isNotEmpty(roles)) {
            for (String role : roles) {
                ClientRole roleObj = roleDao.getRoleByName(role);
                TenantRole tenantRole = new TenantRole();
                tenantRole.setRoleRsId(roleObj.getId());
                tenantRole.setClientId(roleObj.getClientId());
                tenantRole.setName(roleObj.getName());
                tenantRole.setDescription(roleObj.getDescription());
                tenantRoles.add(tenantRole);
            }
        }

        return tenantRoles;
    }

    /**
     * Retrieve the propagating roles associated with the domain by retrieving the propagating roles assigned to the user-admin.
     *
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

    String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    int getDefaultCloudAuthTokenExpirationSeconds() {
        return config.getInt("token.cloudAuthExpirationSeconds");
    }

    String getCloudAuthClientId() {
        //ScopeAccesses require a client id. This is legacy stuff that needs to be removed.
        //In the meantime, we're hard coding the cloud auth client id in the scope access.
        return config.getString("cloudAuth.clientId");
    }

}
