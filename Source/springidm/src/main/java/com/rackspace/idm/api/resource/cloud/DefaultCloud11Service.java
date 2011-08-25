package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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
import com.rackspace.idm.cloudv11.jaxb.AuthFault;
import com.rackspace.idm.cloudv11.jaxb.BadRequestFault;
import com.rackspace.idm.cloudv11.jaxb.Credentials;
import com.rackspace.idm.cloudv11.jaxb.ItemNotFoundFault;
import com.rackspace.idm.cloudv11.jaxb.MossoCredentials;
import com.rackspace.idm.cloudv11.jaxb.NastCredentials;
import com.rackspace.idm.cloudv11.jaxb.PasswordCredentials;
import com.rackspace.idm.cloudv11.jaxb.UnauthorizedFault;
import com.rackspace.idm.cloudv11.jaxb.UserCredentials;
import com.rackspace.idm.cloudv11.jaxb.UserDisabledFault;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;

@Component
public class DefaultCloud11Service implements Cloud11Service {

    private final Configuration config;
    private final ScopeAccessService scopeAccessService;
    private final EndpointService endpointService;
    private final UserService userService;
    private final AuthConverterCloudV11 authConverterCloudV11;
    private final UserConverterCloudV11 userConverterCloudV11;
    private final EndpointConverterCloudV11 endpointConverterCloudV11;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final com.rackspace.idm.cloudv11.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloudv11.jaxb.ObjectFactory();

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

    @Override
    public Response.ResponseBuilder authenticate(HttpServletResponse response,
        HttpHeaders httpHeaders, String body) throws IOException {
        if (httpHeaders.getRequestHeader("Content-Type").get(0)
            .contains("application/xml")) {
            return authenticateXML(response, httpHeaders, body);
        } else {
            return authenticateJSON(response, httpHeaders, body);
        }
    }

    public Response.ResponseBuilder authenticateJSON(
        HttpServletResponse response, HttpHeaders httpHeaders, String body)
        throws IOException {
        JAXBElement<? extends Credentials> cred = null;
        try {
            cred = unmarshallCredentialsFromJSON(body);
        } catch (BadRequestException bre) {
            return badRequestExceptionResponse(bre.getMessage());
        }
        return authenticateResponse(cred, httpHeaders, response, body);
    }

    @SuppressWarnings("unchecked")
    public Response.ResponseBuilder authenticateXML(
        HttpServletResponse response, HttpHeaders httpHeaders, String body)
        throws IOException {
        JAXBElement<? extends Credentials> cred = null;
        try {
            JAXBContext context = JAXBContext
                .newInstance("com.rackspace.idm.cloudv11.jaxb");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller
                .unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    private Response.ResponseBuilder notFoundExceptionResponse(String message) {
        ItemNotFoundFault fault = OBJ_FACTORY.createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
            OBJ_FACTORY.createItemNotFound(fault));
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

    private Response.ResponseBuilder serviceExceptionResponse() {
        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));
        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            .entity(OBJ_FACTORY.createAuthFault(fault));
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

    private void handleRedirect(HttpServletResponse response, String path) {
        try {
            response.sendRedirect(path);
        } catch (IOException e) {
            logger.error("Error in redirecting the " + path + " calls");
        }
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
            UserScopeAccess usa = this.scopeAccessService
                .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
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

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
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

    @Override
    public Response.ResponseBuilder validateToken(String tokeId,
        String belongsTo, String type, HttpHeaders httpHeaders)
        throws IOException {
        ScopeAccess sa = this.scopeAccessService
            .getScopeAccessByAccessToken(tokeId);

        if (sa == null || !(sa instanceof UserScopeAccess)
            || ((UserScopeAccess) sa).isAccessTokenExpired(new DateTime())) {
            return notFoundExceptionResponse(String.format(
                "token %s not found", tokeId));
        }

        UserScopeAccess usa = (UserScopeAccess) sa;

        if (!StringUtils.isBlank(belongsTo) && !belongsTo.equals(usa.getUsername())) {
            return notFoundExceptionResponse(String.format(
                "token %s not found", tokeId));
        }

        return Response.ok(OBJ_FACTORY.createToken(this.authConverterCloudV11
            .toCloudV11TokenJaxb((UserScopeAccess) sa)));
    }

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

        UserScopeAccess usa = (UserScopeAccess)sa;
        usa.setAccessTokenExpired();
        this.scopeAccessService.updateScopeAccess(usa);
        
        return Response.noContent();
    }

    @Override
    public <T> Response.ResponseBuilder userRedirect(T nastId,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
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

        return Response.ok(OBJ_FACTORY
            .createBaseURLs(this.endpointConverterCloudV11
                .toBaseUrls(filteredBaseUrls)));
    }

    @Override
    public Response.ResponseBuilder getBaseURLId(int baseURLId,
        String serviceName, HttpHeaders httpHeaders) throws IOException {
        CloudBaseUrl baseUrl = this.endpointService.getBaseUrlById(baseURLId);

        if (baseUrl == null) {
            return notFoundExceptionResponse(String.format(
                "BaseUrlId %s not found", baseURLId));
        }

        return Response.ok(OBJ_FACTORY
            .createBaseURL(this.endpointConverterCloudV11.toBaseUrl(baseUrl)));
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(String serviceName,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder migrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder createUser(HttpHeaders httpHeaders,
        String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder unmigrate(String user,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
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
    public Response.ResponseBuilder all(HttpHeaders httpHeaders, String body)
        throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder deleteUser(String userId,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder updateUser(String userId,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
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
    public Response.ResponseBuilder setUserEnabled(String userId,
        HttpHeaders httpHeaders, String body) throws IOException {
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
    public Response.ResponseBuilder setUserKey(String userId,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(String userId,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
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
    public Response.ResponseBuilder addBaseURLRef(String userId,
        HttpHeaders httpHeaders, String body) throws IOException {
        throw new IOException("Not Implemented");
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
    public Response.ResponseBuilder getUserGroups(String userID,
        HttpHeaders httpHeaders) throws IOException {
        throw new IOException("Not Implemented");
    }
}
