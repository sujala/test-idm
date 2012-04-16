package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.Group;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;

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
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DefaultCloud11Service implements Cloud11Service {

    private static final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    private final AuthConverterCloudV11 authConverterCloudV11;
    private final Configuration config;
    private final EndpointConverterCloudV11 endpointConverterCloudV11;
    private final EndpointService endpointService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScopeAccessService scopeAccessService;
    private final LdapCloudAdminRepository ldapCloudAdminRepository;
    private final UserConverterCloudV11 userConverterCloudV11;
    private final UserService userService;

    @Autowired
    private CloudContractDescriptionBuilder cloudContractDescriptionBuilder;

    private final AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();

    @Autowired
    private NastFacade nastFacade;

    @Autowired
    private GroupService userGroupService;

    @Autowired
    private CredentialUnmarshaller credentialUnmarshaller;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CloudExceptionResponse cloudExceptionResponse;

    @Autowired
    private ApplicationService clientService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private GroupService cloudGroupService;

    @Autowired
    public DefaultCloud11Service(Configuration config,
                                 ScopeAccessService scopeAccessService, EndpointService endpointService,
                                 UserService userService, AuthConverterCloudV11 authConverterCloudV11,
                                 UserConverterCloudV11 userConverterCloudV11,
                                 EndpointConverterCloudV11 endpointConverterCloudV11,
                                 LdapCloudAdminRepository ldapCloudAdminRepository,
                                 CloudExceptionResponse cloudExceptionResponse,
                                 ApplicationService clientService,
                                 TenantService tenantService) {
        this.config = config;
        this.scopeAccessService = scopeAccessService;
        this.endpointService = endpointService;
        this.userService = userService;
        this.authConverterCloudV11 = authConverterCloudV11;
        this.userConverterCloudV11 = userConverterCloudV11;
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
        this.ldapCloudAdminRepository = ldapCloudAdminRepository;
        this.cloudExceptionResponse = cloudExceptionResponse;
        this.clientService = clientService;
        this.tenantService = tenantService;
    }

    public ResponseBuilder getVersion(UriInfo uriInfo) throws JAXBException {
        final String responseXml = cloudContractDescriptionBuilder.buildVersion11Page();
        JAXBContext context = JAXBContext.newInstance("org.openstack.docs.common.api.v1:org.w3._2005.atom");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<VersionChoice> versionChoice = (JAXBElement<VersionChoice>) unmarshaller.unmarshal(new StringReader(responseXml));
        return Response.ok(versionChoice);
    }

    // Token Methods
    @Override
    public Response.ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            ScopeAccess sa = this.scopeAccessService.getScopeAccessByAccessToken(tokenId);

            if (sa == null || !(sa instanceof UserScopeAccess) || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException(String.format("token %s not found", tokenId));
            }

            UserScopeAccess usa = (UserScopeAccess) sa;
            usa.setAccessTokenExpired();
            this.scopeAccessService.updateScopeAccess(usa);

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

            UserType userType = null;

            if (type != null) {
                try {
                    userType = UserType.fromValue(type.trim().toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new BadRequestException("Bad type parameter");
                }
            } else {
                userType = UserType.CLOUD;
            }

            ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(tokeId);

            if (sa == null || !(sa instanceof UserScopeAccess) || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException(String.format("token %s not found", tokeId));
            }

            UserScopeAccess usa = (UserScopeAccess) sa;

            User user = null;

            if (!StringUtils.isBlank(belongsTo)) {
                switch (userType) {
                    case CLOUD:
                        user = this.userService.getUser(belongsTo);
                        break;
                    case MOSSO:
                        user = this.userService.getUserByMossoId(Integer.parseInt(belongsTo));
                        break;
                    case NAST:
                        user = this.userService.getUserByNastId(belongsTo);
                        break;
                }

                if (user == null) {
                    throw new NotAuthorizedException("Username or api key invalid");
                }

                if (user.isDisabled()) {
                    throw new UserDisabledException(user.getUsername());
                }

                if (!user.getUsername().equals(usa.getUsername())) {
                    throw new NotAuthorizedException("Username or api key invalid");
                }
            }

            String requestURL = request.getRequestURL().toString();
            String versionBaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/token/") + 1);
            return Response.ok(OBJ_FACTORY.createToken(this.authConverterCloudV11.toCloudV11TokenJaxb(usa, versionBaseUrl)));

        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    // Authenticate Methods
    @Override
    public ResponseBuilder adminAuthenticate(HttpServletRequest request, HttpServletResponse response, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {
            authenticateCloudAdminUser(request);
            if (httpHeaders.getMediaType() != null && httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(response, httpHeaders, body, true);
            } else {
                return authenticateJSON(response, httpHeaders, body, true);
            }
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletRequest request, HttpServletResponse response, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {
            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(response, httpHeaders, body, false);
            } else {
                return authenticateJSON(response, httpHeaders, body, false);
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
            authenticateCloudAdminUser(request);

            User user = userService.getUser(userId);
            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlRef.getId());

            if (baseUrl == null) {
                throw new NotFoundException(String.format("BaseUrl %s not found", baseUrlRef.getId()));
            }

            if (!baseUrl.getEnabled()) {
                throw new BadRequestException(String.format("Attempted to add a disabled BaseURL!"));
            }

            this.endpointService.addBaseUrlToUser(baseUrl.getBaseUrlId(), baseUrlRef.isV1Default(), userId);

            return Response
                    .status(Response.Status.CREATED)
                    .header("Location", uriInfo.getRequestUriBuilder().path(userId).build().toString())
                    .entity(OBJ_FACTORY.createBaseURLRef(baseUrlRef));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                               com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            if (StringUtils.isBlank(user.getId())) {
                String errorMsg = "Expecting username";
                logger.warn(errorMsg);
                throw new BadRequestException(errorMsg);
            }
            if (user.getMossoId() != null) {
                validateMossoId(user.getMossoId());
            }
            if (user.getMossoId() == null) {
                String errorMsg = "Expecting mossoId";
                logger.warn(errorMsg);
                throw new BadRequestException(errorMsg);
            }

            if (isNastEnabled()) {
                String nastId = nastFacade.addNastUser(user);
                user.setNastId(nastId);
            }
            User userDO = this.userConverterCloudV11.toUserDO(user);
            userDO.setEnabled(true);

            this.userService.addUser(userDO);


            //Add user-admin role
            ClientRole roleId = clientService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
            ClientRole cRole = this.clientService.getClientRoleById(roleId.getId());

            TenantRole role = new TenantRole();
            role.setClientId(cRole.getClientId());
            role.setName(cRole.getName());
            role.setRoleRsId(cRole.getId());
            this.tenantService.addTenantRoleToUser(userDO, role);

            if (user.getBaseURLRefs() != null && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
                // If BaseUrlRefs were sent in then we're going to add the new
                // list

                // Add new list of baseUrls
                for (BaseURLRef ref : user.getBaseURLRefs().getBaseURLRef()) {
                    this.endpointService.addBaseUrlToUser(ref.getId(), ref.isV1Default(), userDO.getUsername());
                }
            }
            for (CloudBaseUrl cloudBaseUrl : endpointService.getDefaultBaseUrls()) {
                endpointService.addBaseUrlToUser(cloudBaseUrl.getBaseUrlId(), cloudBaseUrl.getDef(), userDO.getUsername());
            }

            List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(userDO.getUsername());

            String id = userDO.getId();
            URI uri = uriInfo.getRequestUriBuilder().path(id).build();
            com.rackspacecloud.docs.auth.api.v1.User cloud11User = userConverterCloudV11.toCloudV11User(userDO, endpoints);
            return Response.created(uri).entity(OBJ_FACTORY.createUser(cloud11User));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    public void validateMossoId(Integer mossoId) {
        Users usersByMossoId = userService.getUsersByMossoId(mossoId);
        if (usersByMossoId.getUsers().size() > 0) {
            throw new BadRequestException("User with Mosso Account ID: " + mossoId + " already exists.");
        }
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
                                                     HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            int id = 0;

            id = Integer.parseInt(baseURLId);

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(id);

            if (baseUrl == null) {
                return cloudExceptionResponse.notFoundExceptionResponse(String.format("BaseUrlId %s not found for user %s", id, userId));
            }
            this.endpointService.removeBaseUrlFromUser(id, userId);

            return Response.noContent();
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteUser(HttpServletRequest request,
                                               String userId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            this.userService.softDeleteUser(gaUser);

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
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            int id = 0;

            id = Integer.parseInt(baseURLId);

            CloudEndpoint endpoint = this.endpointService.getEndpointForUser(
                    userId, id);

            if (endpoint == null) {
                return cloudExceptionResponse.notFoundExceptionResponse(String.format("BaseUrlId %s not found for user %s", id, userId));
            }

            return Response.ok(OBJ_FACTORY.createBaseURLRef(this.endpointConverterCloudV11.toBaseUrlRef(endpoint)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(HttpServletRequest request,
                                                   String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY.createBaseURLRefs(this.endpointConverterCloudV11.toBaseUrlRefs(endpoints)));
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
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY.createServiceCatalog(this.endpointConverterCloudV11.toServiceCatalog(endpoints)));
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
                String errMsg = "User not found :" + userId;
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11User(user, endpoints)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyEnabled(user)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = this.userService.getUserByMossoId(mossoId);
            if (user == null) {
                throw new NotFoundException(String.format("User with MossoId %s not found", mossoId));
            }
            return cloudExceptionResponse.redirect(request, user.getUsername());
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

            User user = this.userService.getUserByNastId(nastId);
            if (user == null) {
                throw new NotFoundException(String.format("User with NastId %s not found", nastId));
            }
            return cloudExceptionResponse.redirect(request, user.getUsername());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private User checkAndGetUser(String id) {
        User user = this.userService.getUserById(id);

        if (user == null) {
            String errMsg = String.format("User %s not found", id);
            logger.warn(errMsg);
            throw new NotFoundException("User not found");
        }

        return user;
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
            User user = userService.getUser(userName); //this.checkAndGetUser(userID);
            if(user == null){
                String errMsg = String.format("User '%s' was not found.",userName);
                throw new NotFoundException(errMsg);
            }

            List<com.rackspace.idm.domain.entity.Group> groups = userGroupService.getGroupsForUser(user.getId());
            if(groups.size() == 0){
                com.rackspace.idm.domain.entity.Group defGroup = cloudGroupService.getGroupById(config.getInt("defaultGroupId"));
                groups.add(defGroup);
            }
            GroupsList groupList = new GroupsList();
            for (com.rackspace.idm.domain.entity.Group group : groups) {
                Group g = new Group();
                g.setId(group.getName()); // Name for v1.1
                g.setDescription(group.getDescription());
                groupList.getGroup().add(g);
            }
            return Response.ok(OBJ_FACTORY.createGroups(groupList));

        } catch (NotFoundException e) {
            return cloudExceptionResponse.exceptionResponse(e);
        } catch (NotAuthorizedException e) {
            return cloudExceptionResponse.exceptionResponse(e);
        } catch (Exception e) {
            return cloudExceptionResponse.exceptionResponse(e);
        }
    }

    @Override
    public Response.ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyKey(user)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(HttpServletRequest request, String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            gaUser.setEnabled(user.isEnabled());

            this.userService.updateUser(gaUser, false);

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyEnabled(gaUser)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UserWithOnlyKey user)
            throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            gaUser.setApiKey(user.getKey());
            this.userService.updateUser(gaUser, false);

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11UserWithOnlyKey(gaUser)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                               com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {

        try {
            authenticateCloudAdminUser(request);
            userValidator.validate(user);
            if (!user.getId().equals(userId) && !user.getId().equals("")) { //ToDO: Move to user validator?
                throw new BadRequestException("User Id does not match.");
            }
            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = "User not found: " + userId;
                throw new NotFoundException(errMsg);
            }

            gaUser.setMossoId(user.getMossoId());
            gaUser.setNastId(user.getNastId());
            gaUser.setEnabled(user.isEnabled());

            this.userService.updateUser(gaUser, false);

            if (user.getBaseURLRefs() != null && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
                // If BaseUrlRefs were sent in then we're going to clear out the
                // old
                // endpoints
                // and then re-add the new list

                // Delete all old baseUrls
                List<CloudEndpoint> current = this.endpointService
                        .getEndpointsForUser(userId);
                for (CloudEndpoint point : current) {
                    this.endpointService.removeBaseUrlFromUser(point
                            .getBaseUrl().getBaseUrlId(), userId);
                }

                // Add new list of baseUrls
                for (BaseURLRef ref : user.getBaseURLRefs().getBaseURLRef()) {
                    this.endpointService.addBaseUrlToUser(ref.getId(),
                            ref.isV1Default(), userId);
                }
            }

            List<CloudEndpoint> endpoints = this.endpointService.getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11.toCloudV11User(gaUser, endpoints)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }

    }

    // BaseURL Methods
    @Override
    public Response.ResponseBuilder getBaseURLId(HttpServletRequest request, int baseURLId, String serviceName, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseURLId);

            if (baseUrl == null) {
                throw new NotFoundException(String.format("BaseUrlId %s not found", baseURLId));
            }

            if (serviceName != null && !serviceName.equals(baseUrl.getServiceName())) {
                throw new NotFoundException(String.format("BaseUrlId %s not found", baseURLId));
            }

            return Response.ok(OBJ_FACTORY.createBaseURL(this.endpointConverterCloudV11.toBaseUrl(baseUrl)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            List<CloudBaseUrl> baseUrls = this.endpointService.getBaseUrls();

            if (StringUtils.isEmpty(serviceName)) {
                return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(baseUrls)));
            }

            List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
            for (CloudBaseUrl url : baseUrls) {
                String service = url.getServiceName();
                if (service != null && service.equals(serviceName)) {
                    filteredBaseUrls.add(url);
                }
            }

            if (filteredBaseUrls.size() == 0) {
                String errMsg = String.format("Service: '%s' not found.", serviceName);
                return cloudExceptionResponse.notFoundExceptionResponse(errMsg);
            }
            return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(filteredBaseUrls)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            List<CloudBaseUrl> baseUrls = this.endpointService.getBaseUrls();

            List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
            for (CloudBaseUrl url : baseUrls) {
                if (url.getEnabled()) {
                    filteredBaseUrls.add(url);
                }
            }

            return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(filteredBaseUrls)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request, HttpHeaders httpHeaders, BaseURL baseUrl) {

        try {
            authenticateCloudAdminUser(request);
            this.endpointService.addBaseUrl(this.endpointConverterCloudV11.toBaseUrlDO(baseUrl));
            return Response.status(HttpServletResponse.SC_CREATED).header("Location", request.getContextPath() + "/baseUrls/" + baseUrl.getId());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder extensions(HttpHeaders httpHeaders) throws IOException {
        //TODO
        throw new IOException("Not Implemented");
    }

    // Migration Methods
    @Override
    public Response.ResponseBuilder all(HttpServletRequest request, HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder migrate(HttpServletRequest request, String user, HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder unmigrate(HttpServletRequest request, String user, HttpHeaders httpHeaders, String body)
            throws IOException {
        throw new IOException("Not Implemented");
    }

    // Private Methods
    private Response.ResponseBuilder adminAuthenticateResponse(JAXBElement<? extends Credentials> cred, HttpServletResponse response)
            throws IOException {
        if (cred.getValue() instanceof UserCredentials) {
            handleRedirect(response, "cloud/auth");
        }

        User user = null;
        UserScopeAccess usa = null;

        try {
            if (cred.getValue() instanceof MossoCredentials) {
                MossoCredentials mossoCreds = (MossoCredentials) cred.getValue();
                int mossoId = mossoCreds.getMossoId();
                String apiKey = mossoCreds.getKey();
                if (apiKey == null || apiKey.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting apiKey");
                }
                user = this.userService.getUserByMossoId(mossoId);
                if (user == null) {
                    return cloudExceptionResponse.notFoundExceptionResponse(String.format("User with MossoId %s not found", mossoId));
                }
                if (user.isDisabled()) {
                    throw new UserDisabledException(user.getMossoId().toString());
                }
                usa = scopeAccessService.getUserScopeAccessForClientIdByMossoIdAndApiCredentials(mossoId, apiKey, getCloudAuthClientId());
            } else if (cred.getValue() instanceof NastCredentials) {
                NastCredentials nastCreds = (NastCredentials) cred.getValue();
                String nastId = nastCreds.getNastId();
                String apiKey = nastCreds.getKey();
                user = this.userService.getUserByNastId(nastId);
                if (user == null) {
                    return cloudExceptionResponse.notFoundExceptionResponse(String.format("User with NastId %s not found", nastId));
                }
                if (user.isDisabled()) {
                    throw new UserDisabledException(user.getNastId());
                }
                usa = scopeAccessService.getUserScopeAccessForClientIdByNastIdAndApiCredentials(nastId, apiKey, getCloudAuthClientId());
            } else {
                PasswordCredentials passCreds = (PasswordCredentials) cred.getValue();
                String username = passCreds.getUsername();
                String password = passCreds.getPassword();
                if (StringUtils.isBlank(password)) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting password");
                }
                if (StringUtils.isBlank(username)) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting username");
                }
                user = this.userService.getUser(username);
                if (user == null) {
                    String errMsg = "User account exists externally, but not in the AUTH database.";
                    throw new NotAuthorizedException(errMsg);
                }
                if (user.isDisabled()) {
                    throw new UserDisabledException("User " + username + " is not enabled.");
                }
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, getCloudAuthClientId());
            }
            List<CloudEndpoint> endpoints = endpointService.getEndpointsForUser(user.getUsername());
            return Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(usa, endpoints)));
        } catch (NotAuthenticatedException nae) {
            return cloudExceptionResponse.notAuthenticatedExceptionResponse(user.getUsername());
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private Response.ResponseBuilder authenticateJSON(HttpServletResponse response, HttpHeaders httpHeaders, String body,
                                                      boolean isAdmin) throws IOException {

        JAXBElement<? extends Credentials> cred = null;

        cred = credentialUnmarshaller.unmarshallCredentialsFromJSON(body);

        if (isAdmin) {
            return adminAuthenticateResponse(cred, response);
        }
        return authenticateResponse(cred, response);
    }

    @SuppressWarnings("unchecked")
    private Response.ResponseBuilder authenticateXML(HttpServletResponse response, HttpHeaders httpHeaders, String body,
                                                     boolean isAdmin) throws IOException {

        JAXBElement<? extends Credentials> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller.unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (isAdmin) {
            return adminAuthenticateResponse(cred, response);
        }
        return authenticateResponse(cred, response);
    }

    Response.ResponseBuilder authenticateResponse(JAXBElement<? extends Credentials> cred, HttpServletResponse response) throws IOException {

        try {
            Credentials value = cred.getValue();
            String username = null;
            User user = null;
            UserScopeAccess usa = null;
            String cloudAuthClientId = getCloudAuthClientId();
            if (value instanceof UserCredentials) {
                UserCredentials userCreds = (UserCredentials) value;
                username = userCreds.getUsername();
                String apiKey = userCreds.getKey();
                if (apiKey == null || apiKey.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting apiKey");
                }
                if (username == null || username.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting username");
                }
                user = userService.getUser(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, apiKey, cloudAuthClientId);
            } else if (value instanceof PasswordCredentials) {
                username = ((PasswordCredentials) value).getUsername();
                String password = ((PasswordCredentials) value).getPassword();
                if (password == null || password.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting password");
                }
                if (username == null || username.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting username");
                }
                user = userService.getUser(username);
                usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, cloudAuthClientId);
            } else if (value instanceof MossoCredentials) {
                Integer mossoId = ((MossoCredentials) value).getMossoId();
                String key = ((MossoCredentials) value).getKey();
                if (mossoId == null) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting mosso id");
                }
                user = userService.getUserByMossoId(mossoId);
                usa = scopeAccessService.getUserScopeAccessForClientIdByMossoIdAndApiCredentials(mossoId, key, cloudAuthClientId);
            } else if (value instanceof NastCredentials) {
                String nastId = ((NastCredentials) value).getNastId();
                String key = ((NastCredentials) value).getKey();
                if (nastId == null || nastId.length() == 0) {
                    return cloudExceptionResponse.badRequestExceptionResponse("Expecting nast id");
                }
                user = userService.getUserByNastId(nastId);
                usa = scopeAccessService.getUserScopeAccessForClientIdByNastIdAndApiCredentials(nastId, key, cloudAuthClientId);
            }

            if (user == null) {
                String errMsg = String.format("User %s not found", username);
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = endpointService.getEndpointsForUser(username);
            return Response.ok(OBJ_FACTORY.createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(usa, endpoints)));
        } catch (Exception ex) {
            return cloudExceptionResponse.exceptionResponse(ex);
        }
    }

    private boolean isNastEnabled() {
        return config.getBoolean("nast.xmlrpc.enabled");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private void handleRedirect(HttpServletResponse response, String path) {
        try {
            response.sendRedirect(path);
        } catch (IOException e) {
            logger.error("Error in redirecting the " + path + " calls");
        }
    }


    public void setNastFacade(NastFacade nastFacade) {
        this.nastFacade = nastFacade;
    }

    void authenticateCloudAdminUserForGetRequests(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = null;
        try {
            stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        } catch (CloudAdminAuthorizationException e) {
            throw new NotAuthorizedException("You are not authorized to access this resource.");
        }
        if (stringStringMap == null) {
            throw new NotAuthorizedException("You are not authorized to access this resource.");
        } else {

            UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(
                    stringStringMap.get("username"), stringStringMap.get("password"), getCloudAuthClientId());
            boolean authenticated = authorizationService.authorizeCloudIdentityAdmin(usa);

            if (!authenticated) {
                throw new NotAuthorizedException("You are not authorized to access this resource.");
            }
        }
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    void authenticateCloudAdminUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        if (stringStringMap == null) {
            throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
        } else {
            UserScopeAccess usa = scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(
                    stringStringMap.get("username"), stringStringMap.get("password"), getCloudAuthClientId());
            boolean authenticated = authorizationService.authorizeCloudIdentityAdmin(usa);
            //boolean authenticated = ldapCloudAdminRepository.authenticate(stringStringMap.get("username"), stringStringMap.get("password"));
            if (!authenticated) {
                throw new CloudAdminAuthorizationException("Cloud admin user authorization Failed.");
            }
        }
    }

    public void setCredentialUnmarshaller(CredentialUnmarshaller credentialUnmarshaller) {
        this.credentialUnmarshaller = credentialUnmarshaller;
    }

    public void setUserValidator(UserValidator userValidator) {
        this.userValidator = userValidator;
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }
}