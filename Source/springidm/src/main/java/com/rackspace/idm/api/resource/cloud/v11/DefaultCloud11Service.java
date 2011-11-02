package com.rackspace.idm.api.resource.cloud.v11;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.idm.domain.service.UserGroupService;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.ObjectFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.BaseUrlConflictException;
import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.util.NastFacade;

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

    private final AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();

    @Autowired
    private NastFacade nastFacade;
    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    public DefaultCloud11Service(Configuration config,
                                 ScopeAccessService scopeAccessService, EndpointService endpointService,
                                 UserService userService, AuthConverterCloudV11 authConverterCloudV11,
                                 UserConverterCloudV11 userConverterCloudV11,
                                 EndpointConverterCloudV11 endpointConverterCloudV11,
                                 LdapCloudAdminRepository ldapCloudAdminRepository) {
        this.config = config;
        this.scopeAccessService = scopeAccessService;
        this.endpointService = endpointService;
        this.userService = userService;
        this.authConverterCloudV11 = authConverterCloudV11;
        this.userConverterCloudV11 = userConverterCloudV11;
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
        this.ldapCloudAdminRepository = ldapCloudAdminRepository;
    }

    // Token Methods

    @Override
    public Response.ResponseBuilder revokeToken(HttpServletRequest request,
                                                String tokenId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUser(request);

            ScopeAccess sa = this.scopeAccessService
                    .getScopeAccessByAccessToken(tokenId);

            if (sa == null || !(sa instanceof UserScopeAccess)
                    || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException(String.format("token %s not found",
                        tokenId));
            }

            UserScopeAccess usa = (UserScopeAccess) sa;
            usa.setAccessTokenExpired();
            this.scopeAccessService.updateScopeAccess(usa);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder validateToken(HttpServletRequest request,
                                                  String tokeId, String belongsTo, String type, HttpHeaders httpHeaders)
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

            ScopeAccess sa = this.scopeAccessService
                    .getScopeAccessByAccessToken(tokeId);

            if (sa == null || !(sa instanceof UserScopeAccess)
                    || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
                throw new NotFoundException(String.format("token %s not found",
                        tokeId));
            }

            UserScopeAccess usa = (UserScopeAccess) sa;

            User user = null;

            if (!StringUtils.isBlank(belongsTo)) {
                switch (userType) {
                    case CLOUD:
                        user = this.userService.getUser(belongsTo);
                        break;
                    case MOSSO:
                        user = this.userService.getUserByMossoId(Integer
                                .parseInt(belongsTo));
                        break;
                    case NAST:
                        user = this.userService.getUserByNastId(belongsTo);
                        break;
                }

                if (user == null) {
                    throw new NotAuthorizedException(
                            "Username or api key invalid");
                }

                if (user.isDisabled()) {
                    throw new UserDisabledException(user.getUsername());
                }

                if (!user.getUsername().equals(usa.getUsername())) {
                    throw new NotAuthorizedException(
                            "Username or api key invalid");
                }
            }

            return Response.ok(OBJ_FACTORY
                    .createToken(this.authConverterCloudV11
                            .toCloudV11TokenJaxb(usa)));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Authenticate Methods
    @Override
    public ResponseBuilder adminAuthenticate(HttpServletRequest request,
                                             HttpServletResponse response, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {

            authenticateCloudAdminUser(request);

            if (httpHeaders.getMediaType().isCompatible(
                    MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(response, httpHeaders, body, true);
            } else {
                return authenticateJSON(response, httpHeaders, body, true);
            }
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletRequest request,
                                                 HttpServletResponse response, HttpHeaders httpHeaders, String body)
            throws IOException {

        try {
            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return authenticateXML(response, httpHeaders, body, false);
            } else {
                return authenticateJSON(response, httpHeaders, body, false);
            }
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // User Methods
    @Override
    public Response.ResponseBuilder addBaseURLRef(HttpServletRequest request,
                                                  String userId, HttpHeaders httpHeaders, UriInfo uriInfo,
                                                  BaseURLRef baseUrlRef) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            CloudBaseUrl baseUrl = this.endpointService
                    .getBaseUrlById(baseUrlRef.getId());

            if (baseUrl == null) {
                throw new NotFoundException(String.format(
                        "BaseUrl %s not found", baseUrlRef.getId()));
            }

            this.endpointService.addBaseUrlToUser(baseUrl.getBaseUrlId(),
                    baseUrlRef.isV1Default(), userId);

            return Response
                    .status(Response.Status.CREATED)
                    .header(
                            "Location",
                            uriInfo.getRequestUriBuilder().path(userId).build()
                                    .toString())
                    .entity(OBJ_FACTORY.createBaseURLRef(baseUrlRef));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder createUser(HttpServletRequest request,
                                               HttpHeaders httpHeaders, UriInfo uriInfo,
                                               com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            if (StringUtils.isBlank(user.getId())) {
                String errorMsg = "Expecting username";
                logger.warn(errorMsg);
                throw new BadRequestException(errorMsg);
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

            if (user.getBaseURLRefs() != null
                    && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
                // If BaseUrlRefs were sent in then we're going to add the new
                // list

                // Add new list of baseUrls
                for (BaseURLRef ref : user.getBaseURLRefs().getBaseURLRef()) {
                    this.endpointService.addBaseUrlToUser(ref.getId(),
                            ref.isV1Default(), userDO.getUsername());
                }
            }

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(userDO.getUsername());

            String id = userDO.getId();
            URI uri = uriInfo.getRequestUriBuilder().path(id).build();
            com.rackspacecloud.docs.auth.api.v1.User cloud11User = this.userConverterCloudV11
                    .toCloudV11User(userDO, endpoints);
            return Response.created(uri).entity(
                    OBJ_FACTORY.createUser(cloud11User));

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(
            HttpServletRequest request, String userId, String baseURLId,
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
                return notFoundExceptionResponse(String.format(
                        "BaseUrlId %s not found for user %s", id, userId));
            }

            this.endpointService.removeBaseUrlFromUser(id, userId);

            return Response.noContent();
        } catch (Exception ex) {
            return exceptionResponse(ex);
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
            return exceptionResponse(ex);
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
                return notFoundExceptionResponse(String.format(
                        "BaseUrlId %s not found for user %s", id, userId));
            }

            return Response.ok(OBJ_FACTORY
                    .createBaseURLRef(this.endpointConverterCloudV11
                            .toBaseUrlRef(endpoint)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
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

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY
                    .createBaseURLRefs(this.endpointConverterCloudV11
                            .toBaseUrlRefs(endpoints)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(
            HttpServletRequest request, String userId, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY
                    .createServiceCatalog(this.endpointConverterCloudV11
                            .toServiceCatalog(endpoints)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUser(HttpServletRequest request,
                                            String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11.toCloudV11User(user,
                            endpoints)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(HttpServletRequest request,
                                                   String userId, HttpHeaders httpHeaders) throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11
                            .toCloudV11UserWithOnlyEnabled(user)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(
            HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
            throws IOException {

        try {

            authenticateCloudAdminUserForGetRequests(request);

            User user = this.userService.getUserByMossoId(mossoId);
            if (user == null) {
                throw new NotFoundException(String.format(
                        "User with MossoId %s not found", mossoId));
            }
            return redirect(request, user.getUsername());
        } catch (Exception ex) {
            return exceptionResponse(ex);
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
                throw new NotFoundException(String.format(
                        "User with NastId %s not found", nastId));
            }
            return redirect(request, user.getUsername());
        } catch (Exception ex) {
            return exceptionResponse(ex);
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
    public Response.ResponseBuilder getUserGroups(HttpServletRequest request, String userID, HttpHeaders httpHeaders)
            throws IOException {
        try {
            authenticateCloudAdminUser(request);

            if (org.tuckey.web.filters.urlrewrite.utils.StringUtils.isBlank(userID)) {
                String errMsg = "Expecting userId";
                logger.warn(errMsg);
                throw new BadRequestException(errMsg);
            }
            User user = this.checkAndGetUser(userID);

            Integer mossoId = user.getMossoId();
            if (mossoId == null) {
                String errMsg = "User missing mosso id";
                logger.warn(errMsg);
                throw new NotFoundException(errMsg);
            }

            GroupsList groups = this.userGroupService.getGroupList(mossoId);
            return Response.ok(OBJ_FACTORY.createGroups(groups));

        } catch (Exception e) {
            return exceptionResponse(e);
        }
    }

    @Override
    public Response.ResponseBuilder getUserKey(HttpServletRequest request,
                                               String userId, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            User user = userService.getUser(userId);

            if (user == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11
                            .toCloudV11UserWithOnlyKey(user)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(HttpServletRequest request,
                                                   String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders)
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

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11
                            .toCloudV11UserWithOnlyEnabled(gaUser)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder setUserKey(HttpServletRequest request,
                                               String userId, HttpHeaders httpHeaders, UserWithOnlyKey user)
            throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            gaUser.setApiKey(user.getKey());
            this.userService.updateUser(gaUser, false);

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11
                            .toCloudV11UserWithOnlyKey(gaUser)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder updateUser(HttpServletRequest request,
                                               String userId, HttpHeaders httpHeaders,
                                               com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {

        try {
            authenticateCloudAdminUser(request);

            User gaUser = userService.getUser(userId);

            if (gaUser == null) {
                String errMsg = String.format("User %s not found", userId);
                throw new NotFoundException(errMsg);
            }

            gaUser.setMossoId(user.getMossoId());
            gaUser.setNastId(user.getNastId());
            gaUser.setEnabled(user.isEnabled());

            this.userService.updateUser(gaUser, false);

            if (user.getBaseURLRefs() != null
                    && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
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

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(userId);

            return Response.ok(OBJ_FACTORY
                    .createUser(this.userConverterCloudV11.toCloudV11User(gaUser,
                            endpoints)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }

    }

    // BaseURL Methods
    @Override
    public Response.ResponseBuilder getBaseURLId(HttpServletRequest request,
                                                 int baseURLId, String serviceName, HttpHeaders httpHeaders)
            throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            CloudBaseUrl baseUrl = this.endpointService
                    .getBaseUrlById(baseURLId);

            if (baseUrl == null) {
                throw new NotFoundException(String.format(
                        "BaseUrlId %s not found", baseURLId));
            }

            if (serviceName != null
                    && !serviceName.equals(baseUrl.getService())) {
                throw new NotFoundException(String.format(
                        "BaseUrlId %s not found", baseURLId));
            }

            return Response.ok(OBJ_FACTORY
                    .createBaseURL(this.endpointConverterCloudV11
                            .toBaseUrl(baseUrl)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(HttpServletRequest request,
                                                String serviceName, HttpHeaders httpHeaders) throws IOException {

        try {
            authenticateCloudAdminUserForGetRequests(request);

            List<CloudBaseUrl> baseUrls = this.endpointService.getBaseUrls();

            if (StringUtils.isEmpty(serviceName)) {
                return Response.ok(OBJ_FACTORY
                        .createBaseURLs(this.endpointConverterCloudV11
                                .toBaseUrls(baseUrls)));
            }

            List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
            for (CloudBaseUrl url : baseUrls) {
                if (url.getService().equals(serviceName)) {
                    filteredBaseUrls.add(url);
                }
            }

            if (filteredBaseUrls.size() == 0) {
                return notFoundExceptionResponse("No matching Urls found");
            }
            return Response.ok(OBJ_FACTORY
                    .createBaseURLs(this.endpointConverterCloudV11
                            .toBaseUrls(filteredBaseUrls)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(
            HttpServletRequest request, String serviceName, HttpHeaders httpHeaders)
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

            return Response.ok(OBJ_FACTORY
                    .createBaseURLs(this.endpointConverterCloudV11
                            .toBaseUrls(filteredBaseUrls)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request,
                                      HttpHeaders httpHeaders, BaseURL baseUrl) {

        try {

            authenticateCloudAdminUser(request);

            this.endpointService.addBaseUrl(this.endpointConverterCloudV11
                    .toBaseUrlDO(baseUrl));

            return Response.status(HttpServletResponse.SC_CREATED).header(
                    "Location",
                    request.getContextPath() + "/baseUrls/" + baseUrl.getId());

        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    // Migration Methods
    @Override
    public Response.ResponseBuilder all(HttpServletRequest request,
                                        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder migrate(HttpServletRequest request,
                                            String user, HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder unmigrate(HttpServletRequest request,
                                              String user, HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    // Private Methods
    private Response.ResponseBuilder adminAuthenticateResponse(
            JAXBElement<? extends Credentials> cred, HttpHeaders httpHeaders,
            HttpServletResponse response, String body) throws IOException {

        if (cred.getValue() instanceof UserCredentials) {
            handleRedirect(response, "cloud/auth");
        }

        User user = null;
        UserScopeAccess usa = null;

        try {

            if (cred.getValue() instanceof MossoCredentials) {
                MossoCredentials mossoCreds = (MossoCredentials) cred
                        .getValue();
                int mossoId = mossoCreds.getMossoId();
                String apiKey = mossoCreds.getKey();
                user = this.userService.getUserByMossoId(mossoId);
                if (user == null) {
                    return notFoundExceptionResponse(String.format(
                            "User with MossoId %s not found", mossoId));
                }
                usa = this.scopeAccessService
                        .getUserScopeAccessForClientIdByMossoIdAndApiCredentials(
                                mossoId, apiKey, getCloudAuthClientId());
            } else if (cred.getValue() instanceof NastCredentials) {
                NastCredentials nastCreds = (NastCredentials) cred.getValue();
                String nastId = nastCreds.getNastId();
                String apiKey = nastCreds.getKey();
                user = this.userService.getUserByNastId(nastId);
                if (user == null) {
                    return notFoundExceptionResponse(String.format(
                            "User with NastId %s not found", nastId));
                }
                usa = this.scopeAccessService
                        .getUserScopeAccessForClientIdByNastIdAndApiCredentials(
                                nastId, apiKey, getCloudAuthClientId());
            } else {
                PasswordCredentials passCreds = (PasswordCredentials) cred
                        .getValue();
                String username = passCreds.getUsername();
                String password = passCreds.getPassword();
                if (StringUtils.isBlank(username)) {
                    return badRequestExceptionResponse("Expecting username");
                }
                if (StringUtils.isBlank(password)) {
                    return badRequestExceptionResponse("Expecting password");
                }
                user = this.userService.getUser(username);
                if (user == null) {
                    String errMsg = String
                            .format("User %s not found", username);
                    throw new NotFoundException(errMsg);
                }
                usa = this.scopeAccessService
                        .getUserScopeAccessForClientIdByUsernameAndPassword(
                                username, password, getCloudAuthClientId());
            }

            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(user.getUsername());
            return Response.ok(OBJ_FACTORY
                    .createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(
                            usa, endpoints)));
        } catch (NotAuthenticatedException nae) {
            return notAuthenticatedExceptionResponse(user.getUsername());
        } catch (UserDisabledException ude) {
            return userDisabledExceptionResponse(user.getUsername());
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    private Response.ResponseBuilder authenticateJSON(
            HttpServletResponse response, HttpHeaders httpHeaders, String body,
            boolean isAdmin) throws IOException {
        JAXBElement<? extends Credentials> cred = null;

        cred = unmarshallCredentialsFromJSON(body);

        if (isAdmin) {
            adminAuthenticateResponse(cred, httpHeaders, response, body);
        }
        return authenticateResponse(cred, httpHeaders, response, body);
    }

    Response.ResponseBuilder authenticateResponse(
            JAXBElement<? extends Credentials> cred, HttpHeaders httpHeaders,
            HttpServletResponse response, String body) throws IOException {

        try {
            if (!(cred.getValue() instanceof UserCredentials)) {
                handleRedirect(response, "cloud/auth-admin");
            }

            UserCredentials userCreds = (UserCredentials) cred.getValue();

            String username = userCreds.getUsername();
            String apiKey = userCreds.getKey();

            if (username == null) {
                return badRequestExceptionResponse("username cannot be null");
            }

            User user = this.userService.getUser(username);

            if (user == null) {
                String errMsg = String.format("User %s not found", username);
                throw new NotFoundException(errMsg);
            }

            UserScopeAccess usa = this.scopeAccessService
                    .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
                            username, apiKey, getCloudAuthClientId());
            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(username);
            return Response.ok(OBJ_FACTORY
                    .createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(
                            usa, endpoints)));
        } catch (Exception ex) {
            return exceptionResponse(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Response.ResponseBuilder authenticateXML(
            HttpServletResponse response, HttpHeaders httpHeaders, String body,
            boolean isAdmin) throws IOException {
        JAXBElement<? extends Credentials> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller
                    .unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (isAdmin) {
            return adminAuthenticateResponse(cred, httpHeaders, response, body);
        }
        return authenticateResponse(cred, httpHeaders, response, body);
    }

    private Response.ResponseBuilder badRequestExceptionResponse(String message) {
        BadRequestFault fault = OBJ_FACTORY.createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
                OBJ_FACTORY.createBadRequest(fault));
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

    private Response.ResponseBuilder notAuthenticatedExceptionResponse(
            String username) {
        String errMsg = String.format("User %s not authenticated", username);
        UnauthorizedFault fault = OBJ_FACTORY.createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED).entity(
                OBJ_FACTORY.createUnauthorized(fault));
    }

    private Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORY.createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                OBJ_FACTORY.createItemNotFound(fault));
    }

    private Response.ResponseBuilder usernameConflictExceptionResponse(
            String message) {
        UsernameConflictFault fault = OBJ_FACTORY.createUsernameConflictFault();
        fault.setCode(HttpServletResponse.SC_CONFLICT);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                OBJ_FACTORY.createUsernameConflict(fault));
    }

    private Response.ResponseBuilder redirect(HttpServletRequest request,
                                              String id) {

        return Response.status(Response.Status.MOVED_PERMANENTLY).header(
                "Location", request.getContextPath() + "/users/" + id);
    }

    private Response.ResponseBuilder serviceExceptionResponse() {
        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .entity(OBJ_FACTORY.createAuthFault(fault));
    }

    private JAXBElement<? extends Credentials> unmarshallCredentialsFromJSON(
            String jsonBody) {

        JSONParser parser = new JSONParser();
        JAXBElement<? extends Credentials> creds = null;

        try {
            JSONObject obj = (JSONObject) parser.parse(jsonBody);
            if (obj.containsKey(JSONConstants.CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                        JSONConstants.CREDENTIALS).toString());
                UserCredentials userCreds = new UserCredentials();
                userCreds.setKey(obj3.get(JSONConstants.KEY).toString());
                userCreds.setUsername(obj3.get(JSONConstants.USERNAME)
                        .toString());
                creds = OBJ_FACTORY.createCredentials(userCreds);

            } else if (obj.containsKey(JSONConstants.MOSSO_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                        JSONConstants.MOSSO_CREDENTIALS).toString());
                MossoCredentials mossoCreds = new MossoCredentials();
                mossoCreds.setKey(obj3.get(JSONConstants.KEY).toString());
                mossoCreds.setMossoId(Integer.parseInt(obj3.get(
                        JSONConstants.MOSSO_ID).toString()));
                creds = OBJ_FACTORY.createMossoCredentials(mossoCreds);

            } else if (obj.containsKey(JSONConstants.NAST_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                        JSONConstants.NAST_CREDENTIALS).toString());
                NastCredentials nastCreds = new NastCredentials();
                nastCreds.setKey(obj3.get(JSONConstants.KEY).toString());
                nastCreds.setNastId(obj3.get(JSONConstants.NAST_ID).toString());
                creds = OBJ_FACTORY.createNastCredentials(nastCreds);

            } else if (obj.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                        JSONConstants.PASSWORD_CREDENTIALS).toString());
                PasswordCredentials passwordCreds = new PasswordCredentials();
                passwordCreds.setUsername(obj3.get(JSONConstants.USERNAME)
                        .toString());
                passwordCreds.setPassword(obj3.get(JSONConstants.PASSWORD)
                        .toString());
                creds = OBJ_FACTORY.createPasswordCredentials(passwordCreds);

            }
        } catch (ParseException e) {
            throw new BadRequestException("malformed JSON");
        }
        return creds;
    }

    private Response.ResponseBuilder userDisabledExceptionResponse(
            String username) {
        String errMsg = String.format("User %s is disabled", username);
        UserDisabledFault fault = OBJ_FACTORY.createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_FORBIDDEN).entity(
                OBJ_FACTORY.createUserDisabled(fault));
    }

    private Response.ResponseBuilder exceptionResponse(Exception ex) {
        if (ex instanceof NotFoundException) {
            return notFoundExceptionResponse(ex.getMessage());
        }
        if (ex instanceof UserDisabledException) {
            return userDisabledExceptionResponse(ex.getMessage());
        }
        if (ex instanceof DuplicateUsernameException) {
            return usernameConflictExceptionResponse(ex.getMessage());
        }
        if (ex instanceof NotAuthenticatedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        }
        if (ex instanceof BadRequestException) {
            return badRequestExceptionResponse(ex.getMessage());
        }
        if (ex instanceof CloudAdminAuthorizationException) {
            return methodNotAllowedExceptionRespone(ex.getMessage());
        }
        if (ex instanceof NotAuthorizedException) {
            return notAuthenticatedExceptionResponse(ex.getMessage());
        }
        if (ex instanceof NumberFormatException) {
            return badRequestExceptionResponse("baseURLId not an integer");
        }
        if (ex instanceof BaseUrlConflictException) {
            return badRequestExceptionResponse(ex.getMessage());
        }

        return serviceExceptionResponse();
    }

    private ResponseBuilder methodNotAllowedExceptionRespone(String message) {
        BadRequestFault fault = OBJ_FACTORY.createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
                .entity(OBJ_FACTORY.createBadRequest(fault));
    }

    public void setNastFacade(NastFacade nastFacade) {
        this.nastFacade = nastFacade;
    }

    private void authenticateCloudAdminUserForGetRequests(
            HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = null;
        try {
            stringStringMap = authHeaderHelper.parseBasicParams(authHeader);
        } catch (CloudAdminAuthorizationException e) {
            throw new NotAuthorizedException(
                    "Cloud admin user authorization Failed.");
        }
        if (stringStringMap == null) {
            throw new NotAuthorizedException(
                    "Cloud admin user authorization Failed.");
        } else {
            boolean authenticated = ldapCloudAdminRepository.authenticate(
                    stringStringMap.get("username"),
                    stringStringMap.get("password"));
            if (!authenticated) {
                throw new NotAuthorizedException(
                        "Cloud admin user authorization Failed.");
            }
        }
    }

    private void authenticateCloudAdminUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Map<String, String> stringStringMap = authHeaderHelper
                .parseBasicParams(authHeader);
        if (stringStringMap == null) {
            throw new CloudAdminAuthorizationException(
                    "Cloud admin user authorization Failed.");
        } else {
            boolean authenticated = ldapCloudAdminRepository.authenticate(
                    stringStringMap.get("username"),
                    stringStringMap.get("password"));
            if (!authenticated) {
                throw new CloudAdminAuthorizationException(
                        "Cloud admin user authorization Failed.");
            }
        }
    }
}
