package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.Collections;
import java.util.List;

@Component
public class DefaultAuthenticateResponseService implements AuthenticateResponseService{

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthConverterCloudV20 authConverterCloudV20;

    @Autowired
    private TokenConverterCloudV20 tokenConverterCloudV20;

    @Autowired
    private UserService userService;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private IdentityUserService identityUserService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public AuthenticateResponse buildAuthResponseForAuthenticate(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest) {
        /**
        * Common case will be a successful auth, so get the service catalog assuming the user has access to specified tenant.
        * The user would have successfully authenticated prior to reaching this point
        */
        ServiceCatalogInfo scInfo = scopeAccessService.getServiceCatalogInfo(authResponseTuple.getUser());

        boolean restrictingAuthByTenant = isUserRestrictingAuthByTenant(authenticationRequest);

        /**
        * If user specified a tenantId/tenantName for auth request, must verify user has access to that tenant (regardless of
        * whether the tenant is disabled or not)
         */
        if (restrictingAuthByTenant) {
            verifyUserHasRoleOnSpecifiedTenant(authResponseTuple.getUser(), scInfo, authenticationRequest);
        }

        // Verify the user is allowed to login [TERMINATOR]
        if (authorizationService.restrictUserAuthentication(scInfo)) {
            //terminator is in effect. All tenants disabled so blank service catalog
            scInfo = new ServiceCatalogInfo(scInfo.getUserTenantRoles(), scInfo.getUserTenants(), Collections.EMPTY_LIST, scInfo.getUserTypeEnum());
        } else if (restrictingAuthByTenant) {
            /**
            * If terminator is in play, doesn't matter if the user specified a disabled tenant. However, if terminator
            * is not in play and user specified tenant, must validate that the tenant the user specified is actually
            * enabled, otherwise throw an error
             */
            verifyUserSpecifiedTenantIsEnabled(authResponseTuple.getUser(), scInfo, authenticationRequest);
        }

        // Remove Admin URLs if non admin token or if user does not have a user type (all users should, but just in case)
        if (scInfo.getUserTypeEnum() == null ||
                !scInfo.getUserTypeEnum().hasLevelAccessOf(IdentityUserTypeEnum.SERVICE_ADMIN)) {
            stripEndpoints(scInfo.getUserEndpoints());
        }

        AuthenticateResponse auth;
        if (restrictingAuthByTenant) {
            //tenant was specified
            Tenant tenant = getTenantForAuthRequest(authenticationRequest, scInfo);

            org.openstack.docs.identity.api.v2.Token convertedToken;
            if (authResponseTuple.isImpersonation()) {
                convertedToken = tokenConverterCloudV20.toToken(authResponseTuple.getImpersonatedScopeAccess(), null);
            } else {
                convertedToken = tokenConverterCloudV20.toToken(authResponseTuple.getUserScopeAccess(), null);
            }

            convertedToken.setTenant(convertTenantEntityToApi(tenant));

            // Filter the service catalog when the tenant specified is NOT the mosso tenant
            if(!isMossoTenant(tenant.getTenantId(), scInfo.getUserTenantRoles())) {
                scInfo = scInfo.filterEndpointsByTenant(tenant);
            }

            auth = authConverterCloudV20.toAuthenticationResponse(authResponseTuple,
                    scInfo);
            auth.setToken(convertedToken);
        } else {
            auth = authConverterCloudV20.toAuthenticationResponse(authResponseTuple, scInfo);
        }

        // Do not expose the core contact ID through auth
        auth.getUser().setContactId(null);

        // If this is a scoped token we clear out the service catalog
        if (authenticationRequest.getScope() != null) {
            auth.setServiceCatalog(null);
        }

        return auth;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(RackerScopeAccess sa) {
        AuthenticateResponse authenticateResponse = objFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        authenticateResponse.setToken(this.tokenConverterCloudV20.toToken(sa, null));
        RackerScopeAccess rackerScopeAccess = (RackerScopeAccess) sa;
        Racker racker = userService.getRackerByRackerId(rackerScopeAccess.getRackerId());
        List<TenantRole> roleList = tenantService.getEphemeralRackerTenantRoles(racker.getRackerId());
        authenticateResponse.setUser(userConverterCloudV20.toRackerForAuthenticateResponse(racker, roleList));

        return authenticateResponse;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(UserScopeAccess sa, String tenantId) {
        AuthenticateResponse authenticateResponse = objFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();
        authenticateResponse.setToken(this.tokenConverterCloudV20.toToken(sa, null));

        UserScopeAccess usa = sa;
        EndUser user = (EndUser) userService.getUserByScopeAccess(usa);
        List<TenantRole> roles = tenantService.getTenantRolesForUser(user);
        validator20.validateTenantIdInRoles(tenantId, roles);
        authenticateResponse.setToken(tokenConverterCloudV20.toToken(sa, roles));
        authenticateResponse.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));

        return authenticateResponse;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(ImpersonatedScopeAccess isa, String tenantId) {
        AuthenticateResponse authenticateResponse = objFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();
        authenticateResponse.setToken(this.tokenConverterCloudV20.toToken(isa, null));

        List<TenantRole> roles;
        ScopeAccess impersonatedToken = scopeAccessService.getScopeAccessByAccessToken(isa.getImpersonatingToken());

        if (impersonatedToken == null || impersonatedToken.isAccessTokenExpired(new DateTime())) {
            throw new NotFoundException("Token not found.");
        }

        if (!(impersonatedToken instanceof UserScopeAccess)) {
            // The only type of scope access that can be impersonated is a UserScopeAccess, if we get here then this is probably bad data
            throw new IllegalStateException("Unrecognized type of token being impersonated " + impersonatedToken.getClass().getSimpleName());
        }

        BaseUser impersonator = userService.getUserByScopeAccess(isa);
        UserScopeAccess usaImpersonatedToken = (UserScopeAccess) impersonatedToken;
        EndUser user = identityUserService.getEndUserById(usaImpersonatedToken.getUserRsId());

        roles = tenantService.getTenantRolesForUser(user);
        validator20.validateTenantIdInRoles(tenantId, roles);
        authenticateResponse.setToken(tokenConverterCloudV20.toToken(isa, roles));
        authenticateResponse.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));

        if(!authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null)) {
            // Only service or identity admins can see the core contact ID on a user
            authenticateResponse.getUser().setContactId(null);
        }

        UserForAuthenticateResponse userForAuthenticateResponse = null;
        List<TenantRole> impRoles = this.tenantService.getGlobalRolesForUser(impersonator);
        if (impersonator instanceof User) {
            userForAuthenticateResponse = userConverterCloudV20.toUserForAuthenticateResponse((User)impersonator, impRoles);
        } else if (impersonator instanceof Racker) {
            /**
            * The impersonator section when the impersonator is a Racker only displays the "Racker" role, not all the
            * edir groups to which the user belongs.
            */
            userForAuthenticateResponse = userConverterCloudV20.toRackerForAuthenticateResponse((Racker)impersonator, impRoles);
        } else {
            throw new IllegalStateException("Unrecognized type of user '" + user.getClass().getName() + "'");
        }
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = objFactories.getRackspaceIdentityExtRaxgaV1Factory();
        JAXBElement<UserForAuthenticateResponse> impersonatorJAXBElement = objectFactory.createImpersonator(userForAuthenticateResponse);
        authenticateResponse.getAny().add(impersonatorJAXBElement);

        return authenticateResponse;
    }

    /**
     * Given an authentication request, determines if the request is being made for a specific tenant or not
     *
     * @param authenticationRequest
     * @return
     */
    private boolean isUserRestrictingAuthByTenant(AuthenticationRequest authenticationRequest) {
        String tenantId = authenticationRequest.getTenantId();
        String tenantName = authenticationRequest.getTenantName();
        return StringUtils.isNotBlank(tenantId) || StringUtils.isNotBlank(tenantName);
    }

    /**
     * Throws NotAuthenticatedException if the user does not have a tenant role on the specified tenant. Does not
     * matter if the tenant is enabled or disabled.
     *
     * @param user
     * @param scInfo
     * @param authenticationRequest
     */
    private void verifyUserHasRoleOnSpecifiedTenant(EndUser user, ServiceCatalogInfo scInfo, AuthenticationRequest authenticationRequest) {
        String tenantId = authenticationRequest.getTenantId();
        String tenantName = authenticationRequest.getTenantName();
        if (StringUtils.isNotBlank(tenantId) && scInfo.findUserTenantById(tenantId) == null) {
            throwRestrictTenantErrorMessageForAuthenticationRequest(authenticationRequest, user, tenantId);
        } else if (StringUtils.isNotBlank(tenantName) && scInfo.findUserTenantByName(tenantName) == null) {
            throwRestrictTenantErrorMessageForAuthenticationRequest(authenticationRequest, user, tenantName);
        }
    }

    /**
     * Throws NotAuthenticatedException if the user does not have a tenant role on the specified tenant. Does not
     * matter if the tenant is enabled or disabled.
     *
     * @param user
     * @param scInfo
     * @param authenticationRequest
     */
    private void verifyUserSpecifiedTenantIsEnabled(EndUser user, ServiceCatalogInfo scInfo, AuthenticationRequest authenticationRequest) {
        String tenantId = authenticationRequest.getTenantId();
        String tenantName = authenticationRequest.getTenantName();
        if (StringUtils.isNotBlank(tenantId)) {
            Tenant tenant = scInfo.findUserTenantById(tenantId);
            if (tenant == null || Boolean.FALSE.equals(tenant.getEnabled())) {
                throwRestrictTenantErrorMessageForAuthenticationRequest(authenticationRequest, user, tenantId);
            }
        } else if (StringUtils.isNotBlank(tenantName)) {
            Tenant tenant = scInfo.findUserTenantByName(tenantName);
            if (tenant == null || Boolean.FALSE.equals(tenant.getEnabled())) {
                throwRestrictTenantErrorMessageForAuthenticationRequest(authenticationRequest, user, tenantName);
            }
        }
    }

    /**
     * Throw an error because the user does not have proper access to the tenant. The error message differs based on
     * the authentication method used.
     *
     * @param authenticationRequest
     * @param user
     * @param tenantIdentifier
     */
    private void throwRestrictTenantErrorMessageForAuthenticationRequest(AuthenticationRequest authenticationRequest, EndUser user, String tenantIdentifier) {
        String errorMsg;
        if (authenticationRequest.getToken() != null) {
            errorMsg = String.format("Token doesn't belong to Tenant with Id/Name: '%s'", tenantIdentifier);
        } else {
            errorMsg =  String.format("Tenant with Name/Id: '%s' is not valid for User '%s' (id: '%s')", tenantIdentifier, user.getUsername(), user.getId());
        }
        logger.warn(errorMsg);
        throw new NotAuthenticatedException(errorMsg);
    }

    private boolean isMossoTenant(String tenantId, List<TenantRole> roles) {
        for (TenantRole role : roles) {
            if (role.getName().equals("compute:default")) {
                return role.getTenantIds().contains(tenantId);
            }
        }
        return tenantId.matches("\\d+");
    }

    /**
     * Gets tenant for authentication request. This checks the auth request for the specified tenant and uses the
     * tenant ID over the tenant name if present
     */
    private Tenant getTenantForAuthRequest(AuthenticationRequest authenticationRequest, ServiceCatalogInfo scInfo) {
        Tenant tenant;
        String tenantId = authenticationRequest.getTenantId();
        String tenantName = authenticationRequest.getTenantName();

        if (!StringUtils.isBlank(tenantId)) {
            tenant = scInfo.findUserTenantById(tenantId);
        } else {
            tenant = scInfo.findUserTenantByName(tenantName);
        }

        //fallback to legacy logic if for some reason tenant wasn't in catalog. Don't think is actually possible, but technically
        //legacy code would allow it.
        if (tenant == null) {
            if (StringUtils.isNotBlank(tenantId)) {
                tenant = tenantService.getTenant(tenantId);
            } else {
                tenant = tenantService.getTenantByName(tenantName);
            }
        }

        return tenant;
    }

    private TenantForAuthenticateResponse convertTenantEntityToApi(Tenant tenant) {
        TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
        tenantForAuthenticateResponse.setId(tenant.getTenantId());
        tenantForAuthenticateResponse.setName(tenant.getName());
        return tenantForAuthenticateResponse;
    }

    private void stripEndpoints(List<OpenstackEndpoint> endpoints) {
        for (int i = 0; i < endpoints.size(); i++) {
            for (CloudBaseUrl baseUrl : endpoints.get(i).getBaseUrls()) {
                baseUrl.setAdminUrl(null);
            }
        }
    }

}
