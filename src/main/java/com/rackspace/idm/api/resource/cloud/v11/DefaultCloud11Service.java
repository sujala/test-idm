package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.Validator;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.Group;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
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

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private CredentialValidator credentialValidator;

    @Autowired
    private Validator validator;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private AuthWithApiKeyCredentials authWithApiKeyCredentials;

    @Autowired
    private ApplicationService applicationService;

    public ResponseBuilder getVersion(UriInfo uriInfo) throws JAXBException {
        final String responseXml = cloudContractDescriptionBuilder.buildVersion11Page();
        JAXBContext context = JAXBContext.newInstance(VersionChoice.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoice> versionChoice = (JAXBElement<VersionChoice>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice.getValue());
    }

    // Token Methods
    @Override
    public Response.ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(tokenId);

            if (!(sa instanceof UserScopeAccess) || sa.isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException(String.format("token %s not found", tokenId));
            }

            UserScopeAccess usa = (UserScopeAccess) sa;
            usa.setAccessTokenExpired();
            this.scopeAccessService.updateScopeAccess(usa);
            User user = (User) userService.getUserByScopeAccess(sa);
            atomHopperClient.asyncTokenPost(user, tokenId);

            return Response.noContent();
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
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

            if (scopeAccessService.isSetupMfaScopedToken(sa)) {
                throw new NotFoundException("Token not found.");
            }

            if (sa instanceof ImpersonatedScopeAccess){
                UserScopeAccess usa = getUserFromImpersonatedScopeAccess((ImpersonatedScopeAccess) sa);
                if(usa == null || usa.isAccessTokenExpired(new DateTime())) {
                    throw new NotFoundException("Token not found");
                }
                EndUser user = identityUserService.getEndUserById(usa.getUserRsId());
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
        usa.setUserRsId(scopeAccess.getImpersonatingRsId());
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

    // User Methods
    @Override
    public Response.ResponseBuilder addBaseURLRef(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                                  UriInfo uriInfo, BaseURLRef baseUrlRef) throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            User user = userService.getUser(userId);
            if (user == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, userId);
                throw new NotFoundException(errMsg);
            }

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(String.valueOf(baseUrlRef.getId()));

            if (baseUrl == null) {
                throw new NotFoundException(String.format("BaseUrl %s not found", baseUrlRef.getId()));
            }

            if (!baseUrl.getEnabled()) {
                throw new BadRequestException(String.format("Attempted to add a disabled BaseURL!"));
            }

            String tenantId;
            if (baseUrl.getBaseUrlType() != null && baseUrl.getBaseUrlType().equals("NAST")) {
                tenantId = user.getNastId();
            }
            else {
                tenantId = String.valueOf(user.getMossoId());
            }

            Tenant tenant = this.tenantService.getTenant(tenantId);

            if(tenant == null){
                throw new NotFoundException(String.format("Tenant %s on user does not exist.", tenantId));
            }

            // Check for existing BaseUrl
            for (String bId : tenant.getBaseUrlIds()) {
                if (bId.equals(String.valueOf(baseUrl.getBaseUrlId()))) {
                    throw new BadRequestException("Attempt to add existing BaseURL!");
                }
            }
            tenant.getBaseUrlIds().add(String.valueOf(baseUrl.getBaseUrlId()));

            String baseUrlRefId = String.valueOf(baseUrlRef.getId());

            replaceAddV1Default(baseUrlRef, tenant, baseUrlRefId);
            this.tenantService.updateTenant(tenant);

            return Response
                    .status(Response.Status.CREATED)
                    .header("Location", uriInfo.getRequestUriBuilder().path(baseUrlRefId).build())
                    .entity(OBJ_FACTORY.createBaseURLRef(baseUrlRef).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private void replaceAddV1Default(BaseURLRef baseUrlRef, Tenant tenant, String baseUrlRefId) {
        if(baseUrlRef.isV1Default()) {
            if (tenant.getV1Defaults() != null) {
                // Check for existing v1Default for replace by Service Name
                CloudBaseUrl newBaseUrl = endpointService.getBaseUrlById(String.valueOf(baseUrlRef.getId()));

                List<String> removeV1Defaults = new ArrayList<String>();

                for (String v1d : tenant.getV1Defaults()) {
                    CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(String.valueOf(Integer.parseInt(v1d)));
                    if (newBaseUrl.getServiceName().equals(cloudBaseUrl.getServiceName())) {
                        removeV1Defaults.add(v1d);
                    }
                }

                if(!removeV1Defaults.isEmpty()){
                    tenant.getV1Defaults().removeAll(removeV1Defaults);
                }
            }
            tenant.getV1Defaults().add(baseUrlRefId);
        }
    }

    @Override
    public Response.ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                               com.rackspacecloud.docs.auth.api.v1.User userTO) throws IOException {

        try {
            ScopeAccess adminToken = authenticateAndAuthorizeCloudAdminUser(request);
            User caller = (User) userService.getUserByScopeAccess(adminToken);

            validator.validate11User(userTO);

            User user = this.userConverterCloudV11.fromUser(userTO);
            userService.setUserDefaultsBasedOnCaller(user, caller);
            userService.addUser(user);

            List<OpenstackEndpoint> endpointsForUser = scopeAccessService.getOpenstackEndpointsForUser(user);

            com.rackspacecloud.docs.auth.api.v1.User createdUserTO = userConverterCloudV11.toCloudV11User(user, endpointsForUser);

            ResponseBuilder builder = Response.created(uriInfo.getRequestUriBuilder().path(user.getUsername()).build());

            return builder.entity(createdUserTO);

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
                                                     HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, userId);
                throw new NotFoundException(errMsg);
            }

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseURLId);

            if (baseUrl == null) {
                return cloudExceptionResponse.notFoundExceptionResponse(String.format("BaseUrlId %s not found for user %s", baseURLId, userId));
            }

            String tenantId = user.getNastId();
            String tenantId2 = String.valueOf(user.getMossoId());

            Tenant[] tenants = new Tenant[2];
            if(StringUtils.isNotBlank(tenantId)){
                tenants[0] = this.tenantService.getTenant(tenantId);
            }
            if(StringUtils.isNotBlank(tenantId2)){
                tenants[1] = this.tenantService.getTenant(tenantId2);
            }

            boolean found = false;
            for(Tenant tenant : tenants) {
                if(tenant != null){
                    HashSet<String> baseUrlIds = new HashSet<String>(tenant.getBaseUrlIds());
                    for (String currentId : baseUrlIds) {
                        if (currentId.equals(String.valueOf(baseUrl.getBaseUrlId()))){
                            tenant.getBaseUrlIds().remove(String.valueOf(baseUrl.getBaseUrlId()));
                            if(tenant.getV1Defaults().contains(currentId)){
                                tenant.getV1Defaults().remove(String.valueOf(baseUrl.getBaseUrlId()));
                            }
                            this.tenantService.updateTenant(tenant);
                            found = true;
                        }

                    }
                }
            }

            if (!found) {
                throw new NotFoundException(String.format("Attempting to delete nonexistent baseUrl: %s", String.valueOf(baseUrl.getBaseUrlId())));
            }

            return Response.noContent();
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            User retrievedUser = userService.getUser(userId);

            if (retrievedUser == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, userId);
                throw new NotFoundException(errMsg);
            }

            boolean isDefaultUser = authorizationService.isDefaultUser(retrievedUser);
            if (isDefaultUser) {
                throw new BadRequestException("Cannot delete Sub-Users via Auth v1.1. Please use v2.0");
            }
            if (userService.hasSubUsers(retrievedUser.getId())) {
                throw new BadRequestException("Cannot delete a User-Admin with Sub-Users. Please use v2.0 contract to remove Sub-Users then try again");
            }

            this.userService.deleteUser(retrievedUser);

            //AtomHopper
            atomHopperClient.asyncPost(retrievedUser, AtomHopperConstants.DELETED);

            return Response.noContent();
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLRef(HttpServletRequest request,
                                                  String userId, String baseURLId, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, userId);
                throw new NotFoundException(errMsg);
            }

            BaseURLRef baseURLRef = new BaseURLRef();
            List<OpenstackEndpoint> endpointsForUser = scopeAccessService.getOpenstackEndpointsForUser(user);
            for (OpenstackEndpoint openstackEndpoint : endpointsForUser) {
                for (CloudBaseUrl baseUrl : openstackEndpoint.getBaseUrls()) {
                    if (String.valueOf(baseUrl.getBaseUrlId()).equals(baseURLId)) {
                        baseURLRef.setHref(baseUrl.getPublicUrl());
                        baseURLRef.setId(Integer.parseInt(baseUrl.getBaseUrlId()));
                        baseURLRef.setV1Default(baseUrl.getDef());
                    }
                }
            }

            if (baseURLRef.getId() == 0) {
                return cloudExceptionResponse.notFoundExceptionResponse(String.format("BaseUrlId %s not found for user %s", baseURLId, userId));
            }

            return Response.ok(OBJ_FACTORY.createBaseURLRef(baseURLRef).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(HttpServletRequest request,
                                                   String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            List<OpenstackEndpoint> endpointsForUser = scopeAccessService.getOpenstackEndpointsForUser(gaUser);
            JAXBElement<BaseURLRefList> baseURLRefsNew = OBJ_FACTORY.createBaseURLRefs(this.endpointConverterCloudV11.openstackToBaseUrlRefs(endpointsForUser));
            return Response.ok(baseURLRefsNew.getValue());

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(HttpServletRequest request, String userId, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(gaUser);

            hideAdminUrls(endpoints);

            return Response.ok(OBJ_FACTORY.createServiceCatalog(this.endpointConverterCloudV11.toServiceCatalog(endpoints)).getValue());
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

    @Override
    public Response.ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            return Response.ok(getJAXBElementUserEnabledWithEndpoints(user).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
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

            User user = this.userService.getUserByTenantId(String.valueOf(mossoId));
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

            User user = this.userService.getUserByTenantId(nastId);
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

    @Override
    public Response.ResponseBuilder getUserGroups(HttpServletRequest request, String userName, HttpHeaders httpHeaders)
            throws IOException {
        try {
            authenticateCloudAdminUserForGetRequests(request);

            if (org.tuckey.web.filters.urlrewrite.utils.StringUtils.isBlank(userName)) {
                String errMsg = "Expecting userId";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            User user = userService.getUser(userName);
            if (user == null) {
                String errMsg = String.format("User not found :%s", userName);
                throw new NotFoundException(errMsg);
            }

            Iterable<com.rackspace.idm.domain.entity.Group> groups = userService.getGroupsForUser(user.getId());
            GroupsList groupList = new GroupsList();
            if (!groups.iterator().hasNext()) {
                com.rackspace.idm.domain.entity.Group defGroup = cloudGroupService.getGroupById(config.getString("defaultGroupId"));
                groupList.getGroup().add(getGroup(defGroup));
            }
            for (com.rackspace.idm.domain.entity.Group group : groups) {
                groupList.getGroup().add(getGroup(group));
            }
            return Response.ok(OBJ_FACTORY.createGroups(groupList).getValue());

        } catch (NotFoundException e) {
            return cloudExceptionResponse.exceptionResponse(e);
        } catch (NotAuthorizedException e) {
            return cloudExceptionResponse.exceptionResponse(e);
        } catch (Exception e) {
            return cloudExceptionResponse.exceptionResponse(e);
        }
    }

    private Group getGroup(com.rackspace.idm.domain.entity.Group group) {
        Group g = new Group();
        g.setId(group.getName()); // Name for v1.1
        g.setDescription(group.getDescription());
        return g;
    }

    @Override
    public Response.ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            return Response.ok(getJAXBElementUserKeyWithEndpoints(user).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private JAXBElement<com.rackspacecloud.docs.auth.api.v1.User> getJAXBElementUserKeyWithEndpoints(User user) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
        return OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyKey(user,endpoints));
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(HttpServletRequest request, String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateAndAuthorizeCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, userId);
                throw new NotFoundException(errMsg);
            }

            Boolean isDisabled = gaUser.isDisabled();

            gaUser.setEnabled(user.isEnabled());

            this.userService.updateUser(gaUser);
            if (gaUser.isDisabled() && !isDisabled) {
                atomHopperClient.asyncPost(gaUser, AtomHopperConstants.DISABLED);
            } else if (!gaUser.isDisabled() && isDisabled) {
                atomHopperClient.asyncPost(gaUser, AtomHopperConstants.ENABLED);
            }

            return Response.ok(getJAXBElementUserEnabledWithEndpoints(gaUser).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UserWithOnlyKey user)
            throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            gaUser.setApiKey(user.getKey());
            this.userService.updateUser(gaUser);

            return Response.ok(getJAXBElementUserKeyWithEndpoints(gaUser).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                               com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {

        try {
            authenticateAndAuthorizeCloudAdminUser(request);

            validator.isUsernameValid(user.getId());

            if (!StringUtils.equals(user.getId(), userId)) { //ToDO: Move to user validator?
                throw new BadRequestException("User Id does not match.");
            }
            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = USER_NOT_FOUND + userId;
                throw new NotFoundException(errMsg);
            }

            boolean isDisabled = gaUser.isDisabled();

            gaUser.setMossoId(user.getMossoId());
            gaUser.setNastId(user.getNastId());
            if(user.isEnabled() != null) {
                gaUser.setEnabled(user.isEnabled());
            }

            this.userService.updateUser(gaUser);
            gaUser = this.userService.getUser(gaUser.getUsername());

            if (user.getBaseURLRefs() != null && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
                // If BaseUrlRefs were sent in then we're going to clear out the
                // old
                // endpoints
                // and then re-add the new list

                List<OpenstackEndpoint> currentEndpoints = scopeAccessService.getOpenstackEndpointsForUser(gaUser);

                // Delete all old baseUrls
                for (OpenstackEndpoint endpoint : currentEndpoints) {
                    for (CloudBaseUrl baseUrl : endpoint.getBaseUrls()) {
                        userService.removeBaseUrlFromUser(baseUrl.getBaseUrlId(), gaUser);
                    }

                }

                // Add new list of baseUrls
                for (BaseURLRef ref : user.getBaseURLRefs().getBaseURLRef()) {
                    userService.addBaseUrlToUser(String.valueOf(ref.getId()), gaUser);
                }
            }

            if (gaUser.isDisabled() && !isDisabled ) {
                atomHopperClient.asyncPost(gaUser, AtomHopperConstants.DISABLED);
            } else if (!gaUser.isDisabled() && isDisabled) {
                atomHopperClient.asyncPost(gaUser, AtomHopperConstants.ENABLED);
            }

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(gaUser);

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11User(gaUser, endpoints)).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }

    }

    // BaseURL Methods
    @Override
    public Response.ResponseBuilder getBaseURLById(HttpServletRequest request, String baseURLId, String serviceName, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseURLId);

            if (baseUrl == null) {
                throw new NotFoundException(String.format("BaseUrlId %s not found", baseURLId));
            }

            if (serviceName != null && !StringUtils.equals(serviceName, baseUrl.getServiceName())) {
                throw new NotFoundException(String.format("BaseUrlId %s not found", baseURLId));
            }

            return Response.ok(OBJ_FACTORY.createBaseURL(this.endpointConverterCloudV11.toBaseUrl(baseUrl)).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            Iterable<CloudBaseUrl> baseUrls = this.endpointService.getBaseUrls();

            if (StringUtils.isEmpty(serviceName)) {
                return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(baseUrls)).getValue());
            }

            List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
            for (CloudBaseUrl url : baseUrls) {
                String service = url.getServiceName();
                if (StringUtils.equals(service, serviceName)) {
                    filteredBaseUrls.add(url);
                }
            }

            if (filteredBaseUrls.size() == 0) {
                String errMsg = String.format("Service: '%s' not found.", serviceName);
                return cloudExceptionResponse.notFoundExceptionResponse(errMsg);
            }
            return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(filteredBaseUrls)).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
            for (CloudBaseUrl url : this.endpointService.getBaseUrls()) {
                if (url.getEnabled()) {
                    filteredBaseUrls.add(url);
                }
            }

            return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(filteredBaseUrls)).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request, HttpHeaders httpHeaders, BaseURL baseUrl) {
        try {
            validator.validateBaseUrl(baseUrl);
            authenticateAndAuthorizeCloudAdminUser(request);
            final CloudBaseUrl cloudBaseUrl = this.endpointConverterCloudV11.toBaseUrlDO(baseUrl);

            // Keystone V3 compatibility
            addBaseURLKeystoneV3Data(baseUrl, cloudBaseUrl);

            // Save BaseURL
            this.endpointService.addBaseUrl(cloudBaseUrl);
            return Response.status(HttpServletResponse.SC_CREATED).header("Location", request.getContextPath() + "/baseUrls/" + baseUrl.getId());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private void addBaseURLKeystoneV3Data(BaseURL baseUrl, CloudBaseUrl cloudBaseUrl) {
        try {
            if (baseUrl.getServiceName() != null) {
                final Application application = applicationService.getByName(baseUrl.getServiceName());
                if (application != null) {
                    cloudBaseUrl.setClientId(application.getClientId());
                } else {
                    throw new RuntimeException("[K3] There is no application with name '" + baseUrl.getServiceName() + "'.");
                }
            } else {
                throw new RuntimeException("[K3] BaseURL '" + baseUrl.getAdminURL() + "' is not associated to a serviceName.");
            }
            cloudBaseUrl.setInternalUrlId(UUID.randomUUID().toString());
            cloudBaseUrl.setPublicUrlId(UUID.randomUUID().toString());
            cloudBaseUrl.setAdminUrlId(UUID.randomUUID().toString());
        } catch (Exception e) {
            logger.error("[K3] Impossible state.", e);
            if (config.getBoolean("feature.DefaultCloud11Service.addBaseURLKeystoneV3Data.throwError", false)) {
                throw new RuntimeException(e);
            }
        }
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

        User user = null;
        UserScopeAccess usa = null;

        credentialValidator.validateCredential(cred.getValue(), userService);
        if (cred.getValue() instanceof MossoCredentials || cred.getValue() instanceof  NastCredentials) {
            String tenantId = null;
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

            user = this.userService.getUserByTenantId(tenantId);
            usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(user.getUsername(), apiKey, getCloudAuthClientId());
        } else {
            PasswordCredentials passCreds = (PasswordCredentials) cred.getValue();
            String username = passCreds.getUsername();
            String password = passCreds.getPassword();
            usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, getCloudAuthClientId());
        }

        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);
        //TODO Hiding admin urls to keep old functionality - Need to revisit
        hideAdminUrls(endpoints);
        return Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(usa, endpoints)).getValue());
    }

    Response.ResponseBuilder authenticateJSON(UriInfo uriInfo, String body,
                                              boolean isAdmin) throws IOException {

        JAXBElement<? extends Credentials> cred = null;

        cred = credentialUnmarshaller.unmarshallCredentialsFromJSON(body);

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
            User user = null;
            UserScopeAccess usa = null;
            String cloudAuthClientId = getCloudAuthClientId();
            credentialValidator.validateCredential(value, userService);
            if (value instanceof UserCredentials) {
                UserCredentials userCreds = (UserCredentials) value;
                username = userCreds.getUsername();
                String apiKey = userCreds.getKey();
                user = userService.getUser(username);

                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, apiKey, cloudAuthClientId);
            }

            if (user == null) {
                String errMsg = String.format(USER_S_NOT_FOUND, username);
                throw new NotFoundException(errMsg);
            }

            List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForScopeAccess(usa);
            //TODO Hiding admin urls to keep old functionality - Need to revisit
            hideAdminUrls(endpoints);

            return Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(usa, endpoints)).getValue());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private void hideAdminUrls(List<OpenstackEndpoint> endpoints) {
        for(OpenstackEndpoint endpoint : endpoints){
            for(CloudBaseUrl baseUrl : endpoint.getBaseUrls()){
                baseUrl.setAdminUrl(null);
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

    void authenticateCloudAdminUserForGetRequests(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = null;
        try {
            stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        } catch (CloudAdminAuthorizationException e) {
            throw new NotAuthorizedException("You are not authorized to access this resource.", e);
        }
        if (stringStringMap == null) {
            throw new NotAuthorizedException("You are not authorized to access this resource.");
        } else {

            UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(
                    stringStringMap.get("username"), stringStringMap.get("password"), getCloudAuthClientId());
            boolean authenticated = authorizationService.authorizeCloudIdentityAdmin(usa);
            if (!authenticated) {
                authenticated = authorizationService.authorizeCloudServiceAdmin(usa);
            } if (!authenticated) {
                throw new NotAuthorizedException("You are not authorized to access this resource.");
            }
        }
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    ScopeAccess authenticateAndAuthorizeCloudAdminUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        if (stringStringMap == null) {
            throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
        } else {
            UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(
                    stringStringMap.get("username"), stringStringMap.get("password"), getCloudAuthClientId());
            boolean authenticated = authorizationService.authorizeCloudIdentityAdmin(usa) || authorizationService.authorizeCloudServiceAdmin(usa);
            if (!authenticated) {
                throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
            }
        }

        String adminUsername = stringStringMap.get("username");
        String adminPassword = stringStringMap.get("password");
        UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(adminUsername, adminPassword, getCloudAuthClientId());

        boolean authorized = authorizationService.authorizeCloudIdentityAdmin(usa) || authorizationService.authorizeCloudServiceAdmin(usa);
        if (!authorized) {
            throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
        }

        return usa;
    }

    public void setCredentialUnmarshaller(CredentialUnmarshaller credentialUnmarshaller) {
        this.credentialUnmarshaller = credentialUnmarshaller;
    }

    private Boolean getGenerateApiKeyUserForCreate(){
        return config.getBoolean("generate.apiKey.userForCreate");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
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
}
