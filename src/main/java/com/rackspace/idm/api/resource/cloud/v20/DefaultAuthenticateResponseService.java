package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.AuthConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.TokenConverterCloudV20;
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.security.AuthenticationContext;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.rackspace.idm.GlobalConstants.X_PASSWORD_EXPIRATION;

@Component
public class DefaultAuthenticateResponseService implements AuthenticateResponseService {

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
    private JAXBObjectFactories jaxbObjectFactories;

    @Autowired
    private UserConverterCloudV20 userConverterCloudV20;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private AuthenticationContext authenticationContext;

    @Autowired
    private DomainService domainService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Response.ResponseBuilder buildAuthResponseForAuthenticate(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest) {
        return buildAuthResponseForAuthenticateInternal(authResponseTuple, authenticationRequest, false);
    }

    @Override
    public Response.ResponseBuilder buildAuthResponseForAuthenticateApplyRcn(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest) {
        return buildAuthResponseForAuthenticateInternal(authResponseTuple, authenticationRequest, true);
    }

    private Response.ResponseBuilder buildAuthResponseForAuthenticateInternal(AuthResponseTuple authResponseTuple, AuthenticationRequest authenticationRequest, boolean applyRcnRoles) {

        // Load the domain as the first step in building the auth response.
        // Any subsequent use of the domain should be through the cached version in the authenticationContext
        if (authResponseTuple.getUser() != null && StringUtils.isNotEmpty(authResponseTuple.getUser().getDomainId())
                && (authenticationContext.getDomain() == null || !authenticationContext.getDomain().getDomainId().equals(authResponseTuple.getUser().getDomainId()))) {
            Domain domain = domainService.getDomain(authResponseTuple.getUser().getDomainId());
            authenticationContext.setDomain(domain);
            if (domain == null) {
                logger.error("User with ID {} references domain with ID {} but the domain does not exist.",
                        authResponseTuple.getUser().getId(), authResponseTuple.getUser().getDomainId());
            }
        }

        /**
        * Common case will be a successful auth, so get the service catalog assuming the user has access to specified tenant.
        * The user would have successfully authenticated prior to reaching this point
        */
        ServiceCatalogInfo scInfo;
        if (applyRcnRoles) {
            scInfo = identityUserService.getServiceCatalogInfoApplyRcnRoles(authResponseTuple.getUser());
        } else {
            scInfo = identityUserService.getServiceCatalogInfo(authResponseTuple.getUser());
        }

        boolean restrictingAuthByTenant = isUserRestrictingAuthByTenant(authenticationRequest);
        Tenant tenantInRequest = null;
        if (restrictingAuthByTenant) {
            tenantInRequest = getTenantForAuthRequest(authenticationRequest, scInfo);
        }

        /**
        * If user specified a tenantId/tenantName for auth request, must verify user has access to that tenant (regardless of
        * whether the tenant is disabled or not). The tenant must also belong to the authorized domain of the request.
         */
        if (restrictingAuthByTenant) {
            // Tenant must exist and tenant's domain must match authorization domain
            String authRequestDomain = authenticationRequest.getDomainId();
            if (identityConfig.getRepositoryConfig().shouldVerifyAuthorizationDomains() && (tenantInRequest == null || (StringUtils.isNotBlank(authRequestDomain) && !authRequestDomain.equalsIgnoreCase(tenantInRequest.getDomainId())))) {
                throw new NotAuthorizedException(ErrorCodes.ERROR_CODE_AUTH_INVALID_TENANT_MSG, ErrorCodes.ERROR_CODE_AUTH_INVALID_TENANT);
            }
            // User must have a role on the tenant
            verifyUserHasRoleOnSpecifiedTenant(authResponseTuple.getUser(), scInfo, authenticationRequest);
        }

        // Verify the user is allowed to login [TERMINATOR]
        // Do not filter the service catalog for these users if the token is an impersonation token of that user
        if (authorizationService.restrictUserAuthentication(scInfo)) {
            if (!authResponseTuple.isImpersonation()
                    || !identityConfig.getReloadableConfig().shouldDisplayServiceCatalogForSuspendedUserImpersonationTokens()) {
                //terminator is in effect. All tenants disabled so blank service catalog
                scInfo = new ServiceCatalogInfo(scInfo.getUserTenantRoles(), scInfo.getUserTenants(), Collections.EMPTY_LIST, scInfo.getUserTypeEnum());
            }
        } else if (restrictingAuthByTenant) {
            /**
             * If terminator is in play, doesn't matter if the user specified a disabled tenant. However, if terminator
             * is not in play and user specified tenant, must validate that the tenant the user specified is actually
             * enabled, otherwise throw an error. Exception to this is when terminator is in play and
             * the token provided is an impersonation token for the suspended user. In this case, we do not filter
             * the service catalog and we do not throw an error.
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
            org.openstack.docs.identity.api.v2.Token convertedToken;
            if (authResponseTuple.isImpersonation()) {
                authenticationContext.setIncludeImpersonateInAuthByList(true);
                convertedToken = tokenConverterCloudV20.toToken(authResponseTuple.getImpersonatedScopeAccess(), null);
            } else {
                convertedToken = tokenConverterCloudV20.toToken(authResponseTuple.getUserScopeAccess(), null);
            }

            convertedToken.setTenant(convertTenantEntityToApi(tenantInRequest));

            // Filter the service catalog when the tenant specified is NOT the mosso tenant
            if(!isMossoTenant(tenantInRequest.getTenantId(), scInfo.getUserTenantRoles())) {
                scInfo = scInfo.filterEndpointsByTenant(tenantInRequest);
            }

            auth = authConverterCloudV20.toAuthenticationResponse(authResponseTuple, scInfo);
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

        Response.ResponseBuilder responseBuilder = Response.ok(jaxbObjectFactories.getOpenStackIdentityV2Factory().createAccess(auth).getValue());

        if (tenantInRequest != null) {
            responseBuilder.header(GlobalConstants.X_TENANT_ID, tenantInRequest.getTenantId());
        } else {
            Tenant tenantForHeader = getTenantForAuthResponseTenantHeader(scInfo);
            if (tenantForHeader != null) {
                responseBuilder.header(GlobalConstants.X_TENANT_ID, tenantForHeader.getTenantId());
            }
        }

        if (authenticationContext.getPasswordExpiration() != null) {
            responseBuilder.header(X_PASSWORD_EXPIRATION, authenticationContext.getPasswordExpiration().toDateTimeISO());
        }

        return responseBuilder;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(RackerScopeAccess rackerScopeAccess) {
        AuthenticateResponse authenticateResponse = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();
        authenticateResponse.setToken(this.tokenConverterCloudV20.toToken(rackerScopeAccess, null));
        Racker racker = userService.getRackerByRackerId(rackerScopeAccess.getRackerId());
        List<TenantRole> roleList = tenantService.getEphemeralRackerTenantRoles(racker.getRackerId());
        authenticateResponse.setUser(userConverterCloudV20.toRackerForAuthenticateResponse(racker, roleList));

        return authenticateResponse;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(UserScopeAccess sa, String tenantId) {
        return buildAuthResponseForValidateTokenInternal(sa, tenantId, false);
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateTokenApplyRcnRoles(UserScopeAccess sa, String tenantId) {
       return buildAuthResponseForValidateTokenInternal(sa, tenantId, true);
    }

    private AuthenticateResponse buildAuthResponseForValidateTokenInternal(UserScopeAccess sa, String tenantId, boolean applyRcnRoles) {
        AuthenticateResponse authenticateResponse = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

        // Throws NotFoundException if user can not be retrieved
        EndUser user = (EndUser) userService.getUserByScopeAccess(sa);
        SourcedRoleAssignments sourcedRoleAssignments = tenantService.getSourcedRoleAssignmentsForUser(user);
        List<TenantRole> roles;
        if (applyRcnRoles) {
            roles = sourcedRoleAssignments.asTenantRolesExcludeNoTenants();
            authenticateResponse.setToken(tokenConverterCloudV20.toValidateResponseToken(sa, user, roles));
        }
        else {
            // Adapt to perspective where no tenants on role means it's a domain assigned role
            SourcedRoleAssignmentsLegacyAdapter legacyAdapter = sourcedRoleAssignments.getSourcedRoleAssignmentsLegacyAdapter();
            roles = legacyAdapter.getStandardTenantRoles();
            authenticateResponse.setToken(tokenConverterCloudV20.toToken(sa, roles));
        }
        validator20.validateTenantIdInRoles(tenantId, roles);

        authenticateResponse.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));

        return authenticateResponse;
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateToken(ImpersonatedScopeAccess isa, String tenantId) {
        return buildAuthResponseForValidateImpersonateTokenInternal(isa, tenantId, false);
    }

    @Override
    public AuthenticateResponse buildAuthResponseForValidateTokenApplyRcnRoles(ImpersonatedScopeAccess isa, String tenantId) {
        return buildAuthResponseForValidateImpersonateTokenInternal(isa, tenantId, true);
    }

    private AuthenticateResponse buildAuthResponseForValidateImpersonateTokenInternal(ImpersonatedScopeAccess isa, String tenantId, boolean applyRcnRoles) {
        AuthenticateResponse authenticateResponse = jaxbObjectFactories.getOpenStackIdentityV2Factory().createAuthenticateResponse();

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

        if (applyRcnRoles) {
            SourcedRoleAssignments sourcedRoleAssignments = tenantService.getSourcedRoleAssignmentsForUser(user);
            roles = sourcedRoleAssignments.asTenantRolesExcludeNoTenants();
            authenticateResponse.setToken(tokenConverterCloudV20.toValidateResponseToken(isa, user, roles));
        }
        else {
            /*
             TODO: This portion of validate should be switched to use performant version as appropriate
              This is based on feature flag added in 3.11. This should have been updated then, but was missed.
              However this is not part of current applyRcn story so not changing it now either...
              if (identityConfig.getReloadableConfig().useCachedClientRolesInValidate()) {
                roles = tenantService.getTenantRolesForUserPerformant(user);
              } else {
                roles = tenantService.getTenantRolesForUser(user);
              }
              */
             roles = tenantService.getTenantRolesForUser(user);
             authenticateResponse.setToken(tokenConverterCloudV20.toToken(isa, roles));
        }
        validator20.validateTenantIdInRoles(tenantId, roles);
        authenticateResponse.setUser(userConverterCloudV20.toUserForAuthenticateResponse(user, roles));

        UserForAuthenticateResponse userForAuthenticateResponse = null;

        // NOT applying RCN roles for impersonator, just the user being impersonated
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
        com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory objectFactory = jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory();
        JAXBElement<UserForAuthenticateResponse> impersonatorJAXBElement = objectFactory.createImpersonator(userForAuthenticateResponse);
        authenticateResponse.getAny().add(impersonatorJAXBElement);

        //Phone PIN must not be returned where token being validated is an impersonation token
        authenticateResponse.getUser().setPhonePin(null);

        return authenticateResponse;
    }

    @Override
    public Tenant getTenantForAuthResponseTenantHeader(ServiceCatalogInfo scInfo) {
        Validate.isTrue(scInfo != null && scInfo.getUserTenants() != null);

        if (scInfo.getUserTenants().size() == 1) {
            // 1. If user has a single tenant id then use that value
            return scInfo.getUserTenants().get(0);
        } else if (scInfo.getUserTenants().size() > 1) {
            final String mossoTenantId = tenantService.getMossoIdFromTenantRoles(scInfo.getUserTenantRoles());
            if (mossoTenantId != null) {
                // 2. If user has multiple tenant ids then use the mosso tenant id
                return CollectionUtils.find(scInfo.getUserTenants(), new Predicate<Tenant>() {
                    @Override
                    public boolean evaluate(Tenant tenant) {
                        return tenant != null && StringUtils.isNotBlank(tenant.getTenantId()) && tenant.getTenantId().equalsIgnoreCase(mossoTenantId);
                    }
                });
            } else {
                // 3. If user has multiple tenant ids and none are mosso pick a unique tenant
                // Sort the tenants so that the same tenant is returned across repeated calls
                List<Tenant> sortedTenants = new ArrayList<>(scInfo.getUserTenants());
                Collections.sort(sortedTenants, new Comparator<Tenant>() {
                    @Override
                    public int compare(Tenant t1, Tenant t2) {
                        return String.CASE_INSENSITIVE_ORDER.compare(t1.getTenantId(), t2.getTenantId());
                    }
                });
                return scInfo.getUserTenants().get(0);
            }
        }

        // 4. If user does not have any tenants then return null
        return null;
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
        throw new NotAuthenticatedException(ErrorCodes.ERROR_CODE_AUTH_INVALID_TENANT_MSG, ErrorCodes.ERROR_CODE_AUTH_INVALID_TENANT);
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
