package com.rackspace.idm.api.resource.cloud.v11;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.JAXBContextResolver;
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
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.AuthFault;
import com.rackspacecloud.docs.auth.api.v1.BadRequestFault;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.ItemNotFoundFault;
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserDisabledFault;
import com.rackspacecloud.docs.auth.api.v1.UserType;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;

@Component
public class DefaultCloud11Service implements Cloud11Service {

    private static final com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();
    private final AuthConverterCloudV11 authConverterCloudV11;
    private final Configuration config;
    private final EndpointConverterCloudV11 endpointConverterCloudV11;
    private final EndpointService endpointService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScopeAccessService scopeAccessService;

    private final UserConverterCloudV11 userConverterCloudV11;
    private final UserService userService;

    @Autowired
    public DefaultCloud11Service(Configuration config,
        ScopeAccessService scopeAccessService, EndpointService endpointService,
        UserService userService, AuthConverterCloudV11 authConverterCloudV11,
        UserConverterCloudV11 userConverterCloudV11,
        EndpointConverterCloudV11 endpointConverterCloudV11) {
        this.config = config;
        this.scopeAccessService = scopeAccessService;
        this.endpointService = endpointService;
        this.userService = userService;
        this.authConverterCloudV11 = authConverterCloudV11;
        this.userConverterCloudV11 = userConverterCloudV11;
        this.endpointConverterCloudV11 = endpointConverterCloudV11;
    }

    // Token Methods

    @Override
    public Response.ResponseBuilder revokeToken(String tokenId,
        HttpHeaders httpHeaders) throws IOException {
        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenId);

        if (sa == null || !(sa instanceof UserScopeAccess)
            || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
            return notFoundExceptionResponse(String.format(
                "token %s not found", tokenId));
        }

        UserScopeAccess usa = (UserScopeAccess) sa;
        usa.setAccessTokenExpired();
        this.scopeAccessService.updateScopeAccess(usa);

        return Response.noContent();
    }

    @Override
    public Response.ResponseBuilder validateToken(String tokeId,
        String belongsTo, String type, HttpHeaders httpHeaders)
        throws IOException {

        UserType userType = null;

        if (type != null) {
            try {
                userType = UserType.fromValue(type.trim().toUpperCase());
            } catch (IllegalArgumentException iae) {
                return badRequestExceptionResponse("Bad type parameter");
            }
        } else {
            userType = UserType.CLOUD;
        }

        ScopeAccess sa = this.scopeAccessService
        .getScopeAccessByAccessToken(tokeId);

        if (sa == null || !(sa instanceof UserScopeAccess)
            || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
            return notFoundExceptionResponse(String.format(
                "token %s not found", tokeId));
        }

        UserScopeAccess usa = (UserScopeAccess) sa;

        if (!StringUtils.isBlank(belongsTo)
            && !belongsTo.equals(usa.getUsername())) {
            return notFoundExceptionResponse(String.format(
                "token %s not found", tokeId));
        }

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
                return notAuthenticatedExceptionResponse("Username or api key invalid");
            }

            if (user.isDisabled()) {
                return userDisabledExceptionResponse(user.getUsername());
            }
            
            if (!user.getUsername().equals(usa.getUsername())) {
                return notAuthenticatedExceptionResponse("Username or api key invalid");
            }
        }

        return Response.ok(OBJ_FACTORY.createToken(this.authConverterCloudV11
            .toCloudV11TokenJaxb(usa)));
    }

    // Authenticate Methods
    @Override
    public ResponseBuilder adminAuthenticate(HttpServletResponse response,
        HttpHeaders httpHeaders, String body) throws IOException {
        if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            return authenticateXML(response, httpHeaders, body, true);
        } else {
            return authenticateJSON(response, httpHeaders, body, true);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletResponse response,
        HttpHeaders httpHeaders, String body) throws IOException {
        if (httpHeaders.getMediaType().isCompatible(
            MediaType.APPLICATION_XML_TYPE)) {
            return authenticateXML(response, httpHeaders, body, false);
        } else {
            return authenticateJSON(response, httpHeaders, body, false);
        }
    }

    // User Methods
    @Override
    public Response.ResponseBuilder addBaseURLRef(String userId,
        HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef)
        throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseUrlRef
            .getId());

        if (baseUrl == null) {
            return notFoundExceptionResponse(String.format(
                "BaseUrl %s not found", baseUrlRef.getId()));
        }

        this.endpointService.addBaseUrlToUser(baseUrl.getBaseUrlId(),
            baseUrlRef.isV1Default(), userId);

        return Response
            .status(Response.Status.CREATED)
            .header("Location",
                uriInfo.getRequestUriBuilder().path(userId).build().toString())
            .entity(OBJ_FACTORY.createBaseURLRef(baseUrlRef));
    }

    @Override
    public Response.ResponseBuilder createUser(HttpHeaders httpHeaders,
        com.rackspacecloud.docs.auth.api.v1.User user) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(String userId,
        String baseURLId, HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        int id = 0;

        try {
            id = Integer.parseInt(baseURLId);
        } catch (NumberFormatException nfe) {
            return badRequestExceptionResponse("baseURLId not an integer");
        }

        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(id);

        if (baseUrl == null) {
            return notFoundExceptionResponse(String.format(
                "BaseUrlId %s not found for user %s", id, userId));
        }

        this.endpointService.removeBaseUrlFromUser(id, userId);

        return Response.noContent();
    }

    @Override
    public Response.ResponseBuilder deleteUser(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User gaUser = userService.getUser(userId);

        if (gaUser == null) {
            return userNotFoundExceptionResponse(userId);
        }

        this.userService.softDeleteUser(gaUser);

        return Response.noContent();
    }

    @Override
    public Response.ResponseBuilder getBaseURLRef(String userId,
        String baseURLId, HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        int id = 0;

        try {
            id = Integer.parseInt(baseURLId);
        } catch (NumberFormatException nfe) {
            return badRequestExceptionResponse("baseURLId not an integer");
        }

        CloudEndpoint endpoint = this.endpointService.getEndpointForUser(
            userId, id);

        if (endpoint == null) {
            return notFoundExceptionResponse(String.format(
                "BaseUrlId %s not found for user %s", id, userId));
        }

        return Response.ok(OBJ_FACTORY
            .createBaseURLRef(this.endpointConverterCloudV11
                .toBaseUrlRef(endpoint)));
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(userId);

        return Response.ok(OBJ_FACTORY
            .createBaseURLRefs(this.endpointConverterCloudV11
                .toBaseUrlRefs(endpoints)));
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User gaUser = userService.getUser(userId);

        if (gaUser == null) {
            return userNotFoundExceptionResponse(userId);
        }

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(userId);

        return Response.ok(OBJ_FACTORY
            .createServiceCatalog(this.endpointConverterCloudV11
                .toServiceCatalog(endpoints)));
    }

    @Override
    public Response.ResponseBuilder getUser(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(userId);

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11User(user, endpoints)));
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11UserWithOnlyEnabled(user)));
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(
        HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
        throws IOException {
        User user = this.userService.getUserByMossoId(mossoId);
        if (user == null) {
            return notFoundExceptionResponse(String.format(
                "User with MossoId %s not found", mossoId));
        }
        return redirect(request, user.getUsername());
    }

    @Override
    public Response.ResponseBuilder getUserFromNastId(
        HttpServletRequest request, String nastId, HttpHeaders httpHeaders)
        throws IOException {
        User user = this.userService.getUserByNastId(nastId);
        if (user == null) {
            return notFoundExceptionResponse(String.format(
                "User with NastId %s not found", nastId));
        }
        return redirect(request, user.getUsername());
    }

    @Override
    public Response.ResponseBuilder getUserGroups(String userID,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder getUserKey(String userId,
        HttpHeaders httpHeaders) throws IOException {
        User user = userService.getUser(userId);

        if (user == null) {
            return userNotFoundExceptionResponse(userId);
        }

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11UserWithOnlyKey(user)));
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(String userId,
        UserWithOnlyEnabled user, HttpHeaders httpHeaders) throws IOException {
        User gaUser = userService.getUser(userId);

        if (gaUser == null) {
            return userNotFoundExceptionResponse(userId);
        }

        gaUser.setEnabled(user.isEnabled());

        this.userService.updateUser(gaUser, false);

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11UserWithOnlyEnabled(gaUser)));
    }

    @Override
    public Response.ResponseBuilder setUserKey(String userId,
        HttpHeaders httpHeaders, UserWithOnlyKey user) throws IOException {
        User gaUser = userService.getUser(userId);

        if (gaUser == null) {
            return userNotFoundExceptionResponse(userId);
        }

        gaUser.setApiKey(user.getKey());
        this.userService.updateUser(gaUser, false);

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11UserWithOnlyKey(gaUser)));
    }

    @Override
    public Response.ResponseBuilder updateUser(String userId,
        HttpHeaders httpHeaders, com.rackspacecloud.docs.auth.api.v1.User user)
        throws IOException {

        User gaUser = userService.getUser(userId);

        if (gaUser == null) {
            return userNotFoundExceptionResponse(userId);
        }

        gaUser.setMossoId(user.getMossoId());
        gaUser.setNastId(user.getNastId());
        gaUser.setEnabled(user.isEnabled());

        this.userService.updateUser(gaUser, false);

        if (user.getBaseURLRefs() != null
            && user.getBaseURLRefs().getBaseURLRef().size() > 0) {
            // If BaseUrlRefs were sent in then we're going to clear out the old
            // endpoints
            // and then re-add the new list

            // Delete all old baseUrls
            List<CloudEndpoint> current = this.endpointService
                .getEndpointsForUser(userId);
            for (CloudEndpoint point : current) {
                this.endpointService.removeBaseUrlFromUser(point.getBaseUrl()
                    .getBaseUrlId(), userId);
            }

            // Add new list of baseUrls
            for (BaseURLRef ref : user.getBaseURLRefs().getBaseURLRef()) {
                this.endpointService.addBaseUrlToUser(ref.getId(),
                    ref.isV1Default(), userId);
            }
        }

        List<CloudEndpoint> endpoints = this.endpointService
            .getEndpointsForUser(userId);

        return Response.ok(OBJ_FACTORY.createUser(this.userConverterCloudV11
            .toCloudV11User(gaUser, endpoints)));

    }

    // BaseURL Methods
    @Override
    public Response.ResponseBuilder getBaseURLId(int baseURLId,
        String serviceName, HttpHeaders httpHeaders) throws IOException {
        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseURLId);

        if (baseUrl == null ) {
            return notFoundExceptionResponse(String.format(
                "BaseUrlId %s not found", baseURLId));
        }

        if (serviceName!=null && !serviceName.equals(baseUrl.getService())) {
            return notFoundExceptionResponse(String.format(
                "BaseUrlId %s not found", baseURLId));
        }

        return Response.ok(OBJ_FACTORY
            .createBaseURL(this.endpointConverterCloudV11.toBaseUrl(baseUrl)));
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
        List<CloudBaseUrl> baseUrls = this.endpointService.getBaseUrls();

        if (StringUtils.isEmpty(serviceName)) {
            return Response.ok(OBJ_FACTORY.createBaseURLs(this.endpointConverterCloudV11.toBaseUrls(baseUrls)));
        }

        List<CloudBaseUrl> filteredBaseUrls = new ArrayList<CloudBaseUrl>();
        for (CloudBaseUrl url : baseUrls) {
            if (url.getService().equals(serviceName)) {
                filteredBaseUrls.add(url);
            }
        }

        if(filteredBaseUrls.size()==0){
            return notFoundExceptionResponse("No matching Urls found");
        }
        return Response.ok(OBJ_FACTORY
            .createBaseURLs(this.endpointConverterCloudV11
                .toBaseUrls(filteredBaseUrls)));
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
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
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request,
        HttpHeaders httpHeaders, BaseURL baseUrl) {
        try {
            this.endpointService.addBaseUrl(this.endpointConverterCloudV11
                .toBaseUrlDO(baseUrl));

            return Response.status(HttpServletResponse.SC_CREATED).header(
                "Location",
                request.getContextPath() + "/baseUrls/" + baseUrl.getId());
        } catch (BaseUrlConflictException bce) {
            return badRequestExceptionResponse(String.format(
                "BaseUrl with id %s already exists", baseUrl.getId()));
        } catch (Exception ex) {
            return serviceExceptionResponse();
        }
    }

    // Migration Methods
    @Override
    public Response.ResponseBuilder all(HttpHeaders httpHeaders, String body)
        throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder migrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder unmigrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
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
                if(StringUtils.isBlank(username)){
                    return badRequestExceptionResponse("Expecting username");
                }
                if(StringUtils.isBlank(password)){
                    return badRequestExceptionResponse("Expecting password");
                }
                user = this.userService.getUser(username);
                if (user == null) {
                    return userNotFoundExceptionResponse(username);
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
            return serviceExceptionResponse();
        }
    }

    private Response.ResponseBuilder authenticateJSON(
        HttpServletResponse response, HttpHeaders httpHeaders, String body,
        boolean isAdmin) throws IOException {
        JAXBElement<? extends Credentials> cred = null;
        try {
            cred = unmarshallCredentialsFromJSON(body);
        } catch (BadRequestException bre) {
            return badRequestExceptionResponse(bre.getMessage());
        }
        if (isAdmin) {
            adminAuthenticateResponse(cred, httpHeaders, response, body);
        }
        return authenticateResponse(cred, httpHeaders, response, body);
    }

    private Response.ResponseBuilder authenticateResponse(
        JAXBElement<? extends Credentials> cred, HttpHeaders httpHeaders,
        HttpServletResponse response, String body) throws IOException {

        if (!(cred.getValue() instanceof UserCredentials)) {
            handleRedirect(response, "cloud/auth-admin");
        }

        UserCredentials userCreds = (UserCredentials) cred.getValue();

        String username = userCreds.getUsername();
        String apiKey = userCreds.getKey();

        User user = this.userService.getUser(username);

        if (user == null) {
            return userNotFoundExceptionResponse(username);
        }

        try {
            UserScopeAccess usa = this.scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(
                    username, apiKey, getCloudAuthClientId());
            List<CloudEndpoint> endpoints = this.endpointService
                .getEndpointsForUser(username);
            return Response.ok(OBJ_FACTORY
                .createAuth(this.authConverterCloudV11.toCloudv11AuthDataJaxb(
                    usa, endpoints)));
        } catch (NotAuthenticatedException nae) {
            return notAuthenticatedExceptionResponse(username);
        } catch (UserDisabledException ude) {
            return userDisabledExceptionResponse(username);
        } catch (Exception ex) {
            return serviceExceptionResponse();
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

            if (obj.containsKey("credentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "credentials").toString());
                UserCredentials userCreds = new UserCredentials();
                userCreds.setKey(obj3.get("key").toString());
                userCreds.setUsername(obj3.get("username").toString());
                creds = OBJ_FACTORY.createCredentials(userCreds);

            } else if (obj.containsKey("mossoCredentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "mossoCredentials").toString());
                MossoCredentials mossoCreds = new MossoCredentials();
                mossoCreds.setKey(obj3.get("key").toString());
                mossoCreds.setMossoId(Integer.parseInt(obj3.get("mossoId")
                    .toString()));
                creds = OBJ_FACTORY.createMossoCredentials(mossoCreds);

            } else if (obj.containsKey("nastCredentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "nastCredentials").toString());
                NastCredentials nastCreds = new NastCredentials();
                nastCreds.setKey(obj3.get("key").toString());
                nastCreds.setNastId(obj3.get("nastId").toString());
                creds = OBJ_FACTORY.createNastCredentials(nastCreds);

            } else if (obj.containsKey("passwordCredentials")) {
                JSONObject obj3 = (JSONObject) parser.parse(obj.get(
                    "passwordCredentials").toString());
                PasswordCredentials passwordCreds = new PasswordCredentials();
                passwordCreds.setUsername(obj3.get("username").toString());
                passwordCreds.setPassword(obj3.get("username").toString());
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

    private Response.ResponseBuilder userNotFoundExceptionResponse(
        String username) {
        String errMsg = String.format("User %s not found", username);
        ItemNotFoundFault fault = OBJ_FACTORY.createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORY.createItemNotFound(fault));
    }
}
