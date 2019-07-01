package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum;
import com.rackspace.idm.api.resource.cloud.v20.*;
import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.Cloud11AuthorizationLevel;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.Validator;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.Group;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

@Component
public class DefaultCloud11Service implements Cloud11Service {

    private static final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    public static final String USER_S_NOT_FOUND = "User %s not found";
    public static final String USER_NOT_FOUND = "User not found: ";
    public static final String MFA_USER_AUTH_FORBIDDEN_MESSAGE = "Can not authenticate with credentials provided. This account has multi-factor authentication enabled and you must use version 2.0+ to authenticate.";

    @Autowired
    private AuthConverterCloudV11 authConverterCloudV11;

    @Autowired
    private Configuration config;

    @Autowired
    @Setter
    private IdentityConfig identityConfig;

    @Autowired
    private EndpointConverterCloudV11 endpointConverterCloudV11;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private UserConverterCloudV11 userConverterCloudV11;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private AuthenticateResponseService authenticateResponseService;

    @Autowired
    private DomainService domainService;

    private org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
    private org.openstack.docs.identity.api.v2.ObjectFactory v2ObjectFactory = new org.openstack.docs.identity.api.v2.ObjectFactory();


    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Map<String, JAXBElement<Extension>> extensionMap;
    private JAXBElement<Extensions> currentExtensions;

    @Autowired
    private CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();

    @Autowired
    private CredentialUnmarshaller credentialUnmarshaller;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CloudExceptionResponse cloudExceptionResponse;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private GroupService cloudGroupService;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private CredentialValidator credentialValidator;

    @Autowired
    private Validator validator;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Setter
    @Autowired
    private AuthWithApiKeyCredentials authWithApiKeyCredentials;

    @Setter
    @Autowired
    private AuthWithPasswordCredentials authWithPasswordCredentials;

    @Autowired
    private ApplicationService applicationService;

    @Setter
    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    private static final Class JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH = VersionChoice.class;
    private static final JAXBContext JAXBCONTEXT_VERSION_CHOICE;

    static {
        try {
            JAXBCONTEXT_VERSION_CHOICE = JAXBContext.newInstance(JAXBCONTEXT_VERSION_CHOICE_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new IdmException("Error initializing JAXBContext for versionchoice", e);
        }
    }

    public ResponseBuilder getVersion(UriInfo uriInfo) throws JAXBException {
        JAXBContext context = JAXBCONTEXT_VERSION_CHOICE;

        final String responseXml = cloudContractDescriptionBuilder.buildVersion11Page();
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoice> versionChoice = (JAXBElement<VersionChoice>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice.getValue());
    }

    @Override
    public Response.ResponseBuilder validateToken(HttpServletRequest request, String tokeId, String belongsTo, String type, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            String versionBaseUrl = config.getString("cloud.user.ref.string") + "v1.1/";

            UserType userType = null;

            if (type != null) {
                try {
                    userType = UserType.fromValue(type.trim().toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new BadRequestException("Bad type parameter", iae);
                }
            } else {
                userType = UserType.CLOUD;
            }

            ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(tokeId);

            //can not currently validate any tokens that contain a scope
            if (sa == null || StringUtils.isNotBlank(sa.getScope())) {
                throw new NotFoundException("Token not found.");
            }

            if (sa instanceof ImpersonatedScopeAccess){
                //create a dynamic non-persisted scope access for the user being impersonated based on data in the impersonated token
                UserScopeAccess usa = getUserFromImpersonatedScopeAccess((ImpersonatedScopeAccess) sa);
                if(usa == null || usa.isAccessTokenExpired(new DateTime())) {
                    throw new NotFoundException("Token not found");
                }

                EndUser user = identityUserService.getEndUserById(usa.getUserRsId());
                if (user == null) {
                    logger.debug(String.format("User being impersonated with rsId = '%s' was not found", usa.getUserRsId()));
                    throw new NotFoundException("Token not found");
                }

                return Response.ok(OBJ_FACTORY.createToken(this.authConverterCloudV11.toCloudV11TokenJaxb(usa, versionBaseUrl, user)).getValue());
            }

            if (!(sa instanceof UserScopeAccess) || sa.isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException("Token not found");
            }

            UserScopeAccess usa = (UserScopeAccess) sa;

            EndUser tokenUser = identityUserService.getEndUserById(usa.getUserRsId());
            userService.checkUserDisabled(tokenUser);

            EndUser user = null;

            if (!validator.isBlank(belongsTo)) {
                switch (userType) {
                    case CLOUD:
                        user = this.userService.getUser(belongsTo);
                        break;
                    case MOSSO:
                        user = this.userService.getUserByTenantId(belongsTo);
                        break;
                    case NAST:
                        user = this.userService.getUserByTenantId(belongsTo);
                        break;
                }

                if (user == null) {
                    throw new NotAuthorizedException("Username or api key invalid");
                }

                if (user.isDisabled()) {
                    throw new UserDisabledException(user.getUsername());
                }

                if (!user.getId().equals(usa.getUserRsId())) {
                    throw new NotAuthorizedException("Username or api key invalid");
                }
            } else {
                user = tokenUser;
            }

            return Response.ok(OBJ_FACTORY.createToken(this.authConverterCloudV11.toCloudV11TokenJaxb(usa, versionBaseUrl, user)).getValue());

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private UserScopeAccess getUserFromImpersonatedScopeAccess(ImpersonatedScopeAccess scopeAccess) {
        UserScopeAccess usa = new UserScopeAccess();
        usa.setAccessTokenString(scopeAccess.getAccessTokenString());
        usa.setAccessTokenExp(scopeAccess.getAccessTokenExp());
        usa.setUserRsId(scopeAccess.getRsImpersonatingRsId());
        usa.setCreateTimestamp(scopeAccess.getCreateTimestamp());
        return usa;
    }

    // Authenticate Methods
    @Override
    public ResponseBuilder adminAuthenticate(HttpServletRequest request, UriInfo uriInfo, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);
            if (httpHeaders.getMediaType() != null && httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(uriInfo, body, true);
            } else {
                return authenticateJSON(uriInfo, body, true);
            }
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletRequest request, UriInfo uriInfo, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {
            if (httpHeaders.getMediaType() != null && httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(uriInfo, body, false);
            } else {
                return authenticateJSON(uriInfo, body, false);
            }
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            return Response.ok(getJAXBElementUserWithEndpoints(user).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private JAXBElement<com.rackspacecloud.docs.auth.api.v1.User> getJAXBElementUserWithEndpoints(User user) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
        return OBJ_FACTORY.createUser(this.userConverterCloudV11.openstackToCloudV11User(user, endpoints));
    }

    private JAXBElement<com.rackspacecloud.docs.auth.api.v1.User> getJAXBElementUserEnabledWithEndpoints(User user) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
        return OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyEnabled(user,endpoints));
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = null;

            if (identityConfig.getReloadableConfig().isUserAdminLookUpByDomain()) {
                user = userService.getUserAdminByTenantId(String.valueOf(mossoId));
            }

            // Fallback to current mechanism if user-admin lookup by domain feature is disabled, the user was not found,
            // or no user-admin was set on the domain.
            if (user == null) {
                user = this.userService.getUserByTenantId(String.valueOf(mossoId));
            }

            if (user == null) {
                throw new NotFoundException(String.format("User with MossoId %s not found", mossoId));
            }

            String newLocation = "/v1.1/users/" + user.getUsername();

            return Response
                .status(HttpServletResponse.SC_MOVED_PERMANENTLY)
                .header("Location", newLocation)
                .entity(getJAXBElementUserWithEndpoints(user).getValue());

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUserFromNastId(
            HttpServletRequest request, String nastId, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = null;

            if (identityConfig.getReloadableConfig().isUserAdminLookUpByDomain()) {
                user = userService.getUserAdminByTenantId(nastId);
            }

            // Fallback to current mechanism if user-admin lookup by domain feature is disabled, the user was not found,
            // or no user-admin was set on the domain.
            if (user == null) {
                user = this.userService.getUserByTenantId(nastId);
            }

            if (user == null) {
                throw new NotFoundException(String.format("User with NastId %s not found", nastId));
            }

            String newLocation = "/v1.1/users/" + user.getUsername();

            return Response
                .status(HttpServletResponse.SC_MOVED_PERMANENTLY)
                .header("Location", newLocation)
                .entity(getJAXBElementUserWithEndpoints(user).getValue());

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private Group getGroup(com.rackspace.idm.domain.entity.Group group) {
        Group g = new Group();
        g.setId(group.getName()); // Name for v1.1
        g.setDescription(group.getDescription());
        return g;
    }

    private JAXBElement<com.rackspacecloud.docs.auth.api.v1.User> getJAXBElementUserKeyWithEndpoints(User user) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
        return OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyKey(user,endpoints));
    }

    @Override
    public ResponseBuilder extensions(HttpHeaders httpHeaders) throws IOException {
        try {
            if (currentExtensions == null) {
                JAXBContext jaxbContext = JAXBContextResolver.get();
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                InputStream is = org.tuckey.web.filters.urlrewrite.utils.StringUtils.class.getResourceAsStream("/extensions_v11.xml");
                StreamSource ss = new StreamSource(is);

                currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
            }
            return Response.ok(currentExtensions.getValue());
        } catch (Exception e) {
            // Return 500 error. Is WEB-IN/extensions.xml malformed?
            return cloudExceptionResponse.exceptionResponse(e);
        }
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException {
        try {
            if (validator.isBlank(alias)) {
                throw new BadRequestException("Invalid extension alias '" + alias + "'.");
            }

            final String normalizedAlias = alias.trim().toUpperCase();

            if (extensionMap == null) {
                extensionMap = new HashMap<String, JAXBElement<Extension>>();


                if (currentExtensions == null) {
                    JAXBContext jaxbContext = JAXBContextResolver.get();
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    InputStream is = org.tuckey.web.filters.urlrewrite.utils.StringUtils.class.getResourceAsStream("/extensions_v11.xml");
                    StreamSource ss = new StreamSource(is);
                    currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
                }

                Extensions exts = currentExtensions.getValue();

                for (Extension e : exts.getExtension()) {
                    extensionMap.put(e.getAlias().trim().toUpperCase(), objectFactory.createExtension(e));
                }

            }

            if (!extensionMap.containsKey(normalizedAlias)) {
                throw new NotFoundException("Extension with alias '" + normalizedAlias + "' is not available.");
            }

            return Response.ok(extensionMap.get(normalizedAlias).getValue());
        } catch (Exception e) {
            // Return 500 error. Is WEB-IN/extensions.xml malformed?
            return cloudExceptionResponse.exceptionResponse(e);
        }
    }

    // Private Methods
    Response.ResponseBuilder adminAuthenticateResponse(UriInfo uriInfo, JAXBElement<? extends Credentials> cred)
            throws IOException {
        if (cred.getValue() instanceof UserCredentials) {
            return handleRedirect("v1.1/auth");
        }

        /*
        For mosso, nast, and password credentials this validate call will verify the user exists and is not disabled.
        If the user does not exists or is disabled, an exception will be thrown.
         */
        credentialValidator.validateCredential(cred.getValue(), userService);
        V11AuthResponseTuple v11AuthResponseTuple;
        String tenantId = null;
        if (cred.getValue() instanceof MossoCredentials || cred.getValue() instanceof  NastCredentials) {
            String apiKey;
            if(cred.getValue() instanceof MossoCredentials) {
                MossoCredentials mossoCreds = (MossoCredentials) cred.getValue();
                tenantId = String.valueOf(mossoCreds.getMossoId());
                apiKey = mossoCreds.getKey();
            } else {
                NastCredentials nastCreds = (NastCredentials) cred.getValue();
                tenantId = nastCreds.getNastId();
                apiKey = nastCreds.getKey();
            }

            User user = this.userService.getUserByTenantId(tenantId);
            // Need to set the username in the auth context b/c it is not exposed in the request
            requestContextHolder.getAuthenticationContext().setUsername(user.getUsername());
            v11AuthResponseTuple = innerAPIAuth(user.getUsername(), apiKey);
        } else {
            PasswordCredentials passCreds = (PasswordCredentials) cred.getValue();
            String username = passCreds.getUsername();
            String password = passCreds.getPassword();

            v11AuthResponseTuple = innerPwdAuth(username, password);
        }

        List<OpenstackEndpoint> endpoints = v11AuthResponseTuple.serviceCatalogInfo.getUserEndpoints();

        //TODO Hiding admin urls to keep old functionality - Need to revisit
        hideAdminUrls(endpoints);

        Response.ResponseBuilder responseBuilder = Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(v11AuthResponseTuple.userScopeAccess, endpoints)).getValue());

        if (identityConfig.getReloadableConfig().shouldIncludeTenantInV11AuthResponse()) {
            if (tenantId != null) {
                responseBuilder.header(GlobalConstants.X_TENANT_ID, tenantId);
            } else {
                Tenant tenantForHeader = authenticateResponseService.getTenantForAuthResponseTenantHeader(v11AuthResponseTuple.serviceCatalogInfo);
                if (tenantForHeader != null) {
                    responseBuilder.header(GlobalConstants.X_TENANT_ID, tenantForHeader.getTenantId());
                }
            }
        }

        return responseBuilder;
    }

    private V11AuthResponseTuple innerAPIAuth(String username, String key) {
        UserAuthenticationResult result = authWithApiKeyCredentials.authenticate(username, key);
        return generateV11AuthResponseTupleFromUserAuthResult(result);
    }

    private V11AuthResponseTuple innerPwdAuth(String username, String password) {
        //convert to v2.0 pwd auth request
        AuthenticationRequest pwdRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername pwdCred = new PasswordCredentialsRequiredUsername();
        pwdCred.setUsername(username);
        pwdCred.setPassword(password);
        pwdRequest.setCredential(v2ObjectFactory.createPasswordCredentials(pwdCred));

        UserAuthenticationResult result = authWithPasswordCredentials.authenticate(pwdRequest);
        return generateV11AuthResponseTupleFromUserAuthResult(result);
    }

    private V11AuthResponseTuple generateV11AuthResponseTupleFromUserAuthResult(UserAuthenticationResult userAuthenticationResult) {
        ServiceCatalogInfo scInfo = scopeAccessService.getServiceCatalogInfo(userAuthenticationResult.getUser());

        //verify the user is allowed to login
        if (authorizationService.restrictUserAuthentication(scInfo)) {
            throw new ForbiddenException(GlobalConstants.ALL_TENANTS_DISABLED_ERROR_MESSAGE);
        }

        //create the scope access (if necessary)
        AuthResponseTuple authResponseTuple = scopeAccessService.createScopeAccessForUserAuthenticationResult(userAuthenticationResult);
        UserScopeAccess usa = authResponseTuple.getUserScopeAccess();

        return new V11AuthResponseTuple(userAuthenticationResult, scInfo, usa);
    }

    Response.ResponseBuilder authenticateJSON(UriInfo uriInfo, String body,
                                              boolean isAdmin) throws IOException {

        JAXBElement<? extends Credentials> cred = null;

        cred = credentialUnmarshaller.unmarshallCredentialsFromJSON(body);
        requestContextHolder.getAuthenticationContext().populateAuthRequestData(cred.getValue());

        if (isAdmin) {
            return adminAuthenticateResponse(uriInfo, cred);
        }
        return authenticateResponse(uriInfo, cred);
    }

    @SuppressWarnings("unchecked")
    Response.ResponseBuilder authenticateXML(UriInfo uriInfo, String body,
                                             boolean isAdmin) throws IOException {
        JAXBElement<? extends Credentials> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller.unmarshal(new StringReader(body));
            requestContextHolder.getAuthenticationContext().populateAuthRequestData(cred.getValue());
        } catch (JAXBException e) {
            throw new BadRequestException("Invalid XML", e);
        }
        if (isAdmin) {
            return adminAuthenticateResponse(uriInfo, cred);
        }
        return authenticateResponse(uriInfo, cred);
    }

    Response.ResponseBuilder authenticateResponse(UriInfo uriInfo, JAXBElement<? extends Credentials> cred) throws IOException {

        if (cred.getValue() instanceof PasswordCredentials
                || cred.getValue() instanceof MossoCredentials
                || cred.getValue() instanceof NastCredentials) {
            return handleRedirect("v1.1/auth-admin");
        }

        try {
            Credentials value = cred.getValue();
            String username = null;
            credentialValidator.validateCredential(value, userService);
            V11AuthResponseTuple v11AuthResponseTuple = null;

            if (value instanceof UserCredentials) {
                UserCredentials userCreds = (UserCredentials) value;
                username = userCreds.getUsername();
                String apiKey = userCreds.getKey();
                v11AuthResponseTuple = innerAPIAuth(username, apiKey);
            }

            if (v11AuthResponseTuple == null || v11AuthResponseTuple.userAuthenticationResult == null || v11AuthResponseTuple.userAuthenticationResult.getUser() == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, username);
                throw new NotFoundException(errMsg);
            }

            List<OpenstackEndpoint> endpoints = v11AuthResponseTuple.serviceCatalogInfo.getUserEndpoints();

            //TODO Hiding admin urls to keep old functionality - Need to revisit
            hideAdminUrls(endpoints);

            Response.ResponseBuilder responseBuilder = Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(v11AuthResponseTuple.userScopeAccess, endpoints)).getValue());

            if (identityConfig.getReloadableConfig().shouldIncludeTenantInV11AuthResponse()) {
                Tenant tenantForHeader = authenticateResponseService.getTenantForAuthResponseTenantHeader(v11AuthResponseTuple.serviceCatalogInfo);
                if (tenantForHeader != null) {
                    responseBuilder.header(GlobalConstants.X_TENANT_ID, tenantForHeader.getTenantId());
                }
            }

            return responseBuilder;
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private void hideAdminUrls(List<OpenstackEndpoint> endpoints) {
        if (CollectionUtils.isNotEmpty(endpoints)) {
            for(OpenstackEndpoint endpoint : endpoints) {
                for(CloudBaseUrl baseUrl : endpoint.getBaseUrls()){
                    baseUrl.setAdminUrl(null);
                }
            }
        }
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private ResponseBuilder handleRedirect(String path) {
        try {
            Response.ResponseBuilder builder = Response.status(302);
            builder.header("Location", config.getString("ga.endpoint") + path);
            return builder;
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     For some reason v1.1 throws a NotAuthorizedException on get requests, but a CloudAdminAuthorizationException
     on Post/Puts. Need to swtich to the NotAuthorizedException.
     */
    void authenticateCloudAdminUserForGetRequests(HttpServletRequest request) {
        String msg = "You are not authorized to access this resource.";
        ScopeAccess token;
        try {
            authenticateAndAuthorizeCloudAdminUser(request);
        } catch (CloudAdminAuthorizationException e) {
            throw new NotAuthorizedException(msg, e);
        }
    }

    /**
     * Returns a token (scopeaccess) if the user is authenticated and authorized to make the request. Returns "null"
     * if the user either fails authentication, or is not authorized.
     *
     * @param request
     * @throws CloudAdminAuthorizationException If auth header is not formatted correctly or authorization for the user failed
     * @throws NotAuthorizedException If the supplied username/password is not valid for the user
     * @return
     */
    ScopeAccess authenticateAndAuthorizeCloudAdminUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Will throw a CloudAdminAuthorizationException if is null or
        Map<String, String> stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        if (stringStringMap == null) {
            throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
        }

        String adminUsername = stringStringMap.get("username");
        String adminPassword = stringStringMap.get("password");

        // Will throw a NotAuthenticatedException if authentication fails
        UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(adminUsername, adminPassword, getCloudAuthClientId());

        boolean authorized;
        // Populate the request/security contexts
        RequestContext requestContext = requestContextHolder.getRequestContext();
        requestContext.getSecurityContext().setCallerTokens(usa, usa);

        IdentityApi identityApi = requestContext.getIdentityApi();
        Cloud11AuthorizationLevel authorizationLevel = Cloud11AuthorizationLevel.LEGACY;
        if (identityApi != null) {
            authorizationLevel = identityConfig.getRepositoryConfig().getAuthorizationLevelForService(identityApi.name());
        }
        if (authorizationLevel == Cloud11AuthorizationLevel.LEGACY) {
            authorized = authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.IDENTITY_ADMIN);
        } else if (authorizationLevel == Cloud11AuthorizationLevel.ROLE) {
            logger.debug("Authorizing via dynamic roles");
            String authorizedRoleName = calculateAuthorizationRoleNameForService(identityApi);
            authorized = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(authorizedRoleName);
        } else {
            BaseUser caller = requestContext.getEffectiveCaller();
            logger.warn(String.format("User '%s' attempted to call '%s'. Access to this service has been forbidden for all users", caller == null ? "<UNKNOWN>" : caller.getUsername(), identityApi.name()));
            authorized = false;
        }

        if (!authorized) {
            throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
        }

        return usa;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    private String calculateAuthorizationRoleNameForService(IdentityApi identityApi) {
        String authorizedRoleName = null;
        if (StringUtils.isNotBlank(identityApi.name())) {
            authorizedRoleName = identityApi.name().replaceAll("\\s+", "_").replaceAll("\\.", "_").toLowerCase();
            return String.format("identity:%s", authorizedRoleName);
        }
        return authorizedRoleName;
    }

    public void setCredentialUnmarshaller(CredentialUnmarshaller credentialUnmarshaller) {
        this.credentialUnmarshaller = credentialUnmarshaller;
    }

    private String getCloudAuthUserAdminRole() {
        return IdentityUserTypeEnum.USER_ADMIN.getRoleName();
    }

    public void setCloudGroupService(GroupService cloudGroupService) {
        this.cloudGroupService = cloudGroupService;
    }

    public void setAtomHopperClient(AtomHopperClient atomHopperClient) {
        this.atomHopperClient = atomHopperClient;
    }

    public void setCredentialValidator(CredentialValidator credentialValidator) {
        this.credentialValidator = credentialValidator;
    }

    public void setCloudContractDescriptionBuilder(CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
    }

    public void setAuthHeaderHelper(AuthHeaderHelper authHeaderHelper) {
        this.authHeaderHelper = authHeaderHelper;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public void setEndpointService(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAuthConverterCloudv11(AuthConverterCloudV11 authConverterCloudv11) {
        this.authConverterCloudV11 = authConverterCloudv11;
    }

    public void setUserConverterCloudV11(UserConverterCloudV11 userConverterCloudV11) {
        this.userConverterCloudV11 = userConverterCloudV11;
    }

    public UserConverterCloudV11 getUserConverterCloudV11() {
        return userConverterCloudV11;
    }

    public void setEndpointConverterCloudV11(EndpointConverterCloudV11 endpointConverterCloudV11) {
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
    }

    public void setCloudExceptionResponse(CloudExceptionResponse cloudExceptionResponse) {
        this.cloudExceptionResponse = cloudExceptionResponse;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    private static class V11AuthResponseTuple {
        UserAuthenticationResult userAuthenticationResult;
        ServiceCatalogInfo serviceCatalogInfo;
        UserScopeAccess userScopeAccess;

        public V11AuthResponseTuple(UserAuthenticationResult userAuthenticationResult, ServiceCatalogInfo serviceCatalogInfo, UserScopeAccess userScopeAccess) {
            this.userAuthenticationResult = userAuthenticationResult;
            this.serviceCatalogInfo = serviceCatalogInfo;
            this.userScopeAccess = userScopeAccess;
        }
    }
}
