package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.cloudv11.jaxb.*;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

/**
 * Cloud Auth 1.1 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud11VersionResource {

    private final Configuration config;
    private final CloudClient cloudClient;
    private final UserService userService;
    private final ScopeAccessService scopeAccessService;
    private final EndpointService endpointService;
    private final AuthConverterCloudV11 authConverter;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final com.rackspace.idm.cloudv11.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloudv11.jaxb.ObjectFactory();

    @Autowired
    public Cloud11VersionResource(Configuration config,
                                  CloudClient cloudClient, UserService userService,
                                  ScopeAccessService scopeAccessService, EndpointService endpointService,
                                  AuthConverterCloudV11 authConverter) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.userService = userService;
        this.scopeAccessService = scopeAccessService;
        this.endpointService = endpointService;
        this.authConverter = authConverter;
    }

    @GET
    public Response getCloud11VersionInfo(@Context HttpHeaders httpHeaders)
            throws IOException {
        return cloudClient.get(getCloudAuthV11Url(), httpHeaders).build();
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("auth")
    @Consumes(MediaType.APPLICATION_XML)
    public Response authenticate(@Context HttpServletResponse response,
                                 @Context HttpHeaders httpHeaders, String body) throws IOException {

        JAXBElement<? extends Credentials> cred = null;

        try {
            JAXBContext context = JAXBContext.newInstance("com.rackspace.idm.cloudv11.jaxb");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller
                    .unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return authenticateResponse(cred, httpHeaders, response, body).build();
    }

    @POST
    @Path("auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateJSON(@Context HttpServletResponse response,
                                     @Context HttpHeaders httpHeaders, String body) throws IOException {

        JAXBElement<? extends Credentials> cred = null;

        try {
            cred = unmarshallCredentialsFromJSON(body);
        } catch (BadRequestException bre) {
            return badRequestExceptionResponse(bre.getMessage());
        }

        return authenticateResponse(cred, httpHeaders, response, body).build();
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

            if (useCloudAuth()) {
                return cloudClient.post(getCloudAuthV11Url().concat("auth"),
                        httpHeaders, body);
            } else {
                return notFoundExceptionResponse(username);
            }
        }

        UserScopeAccess usa = null;

        try {
            usa = this.scopeAccessService
                    .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
                            username, apiKey, getCloudAuthClientId());
            List<CloudEndpoint> endpoints = this.endpointService
                    .getEndpointsForUser(username);

            return Response.ok(
                    OBJ_FACTORY.createAuth(this.authConverter
                            .toCloudv11AuthDataJaxb(usa, endpoints)));

        } catch (NotAuthenticatedException nae) {
            return notAuthenticatedExceptionResponse(username);
        } catch (UserDisabledException ude) {
            return userDisabledExceptionResponse(username);
        } catch (Exception ex) {
            return serviceExceptionResponse();
        }
    }

    @GET
    @Path("token{contentType:(\\.(xml|json))?}")
    public Response validateToken(
            @PathParam("contentType") String contentType,
            @QueryParam("belongsTo") String belongsTo,
            @QueryParam("type") String type,
            @Context HttpHeaders httpHeaders
    ) throws IOException {

        if (type == null) {
            type = "CLOUD";
        }

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("belongsTo", belongsTo);
        queryParams.put("type", type);
        return cloudClient.get(getCloudAuthV11Url().concat(getPath("token", contentType, queryParams)), httpHeaders).build();
    }

    @DELETE
    @Path("token")
    public Response revokeToken(
            @PathParam("contentType") String contentType, @Context HttpHeaders httpHeaders, String body
    ) throws IOException {
        //Todo: Jorge implement this method.
        return null;
    }

    private String getPath(String path, String contentType) {
        return getPath(path, contentType, null);
    }

    private String getPath(String path, String contentType, HashMap<String, String> queryParams) {
        String result = path;
        String queryString = "";

        if (contentType != null) {
            result = path + contentType;
        }

        if (queryParams != null) {
            for (String key : queryParams.keySet()) {
                if (queryParams.get(key) != null) {
                    queryString += key + "=" + queryParams.get(key) + "&";
                }
            }

            if (queryString.length() > 0) {
                result += "?" + queryString.substring(0, queryString.length() - 1);
            }
        }

        return result;
    }


//    @POST
//    @Path("auth")
//    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
//    public Response authenticate(@Context HttpHeaders httpHeaders,
//        @Context HttpServletRequest request,
//        @Context HttpServletResponse response,
//        JAXBElement<? extends Credentials> cred) throws IOException {
//
//        if (!(cred.getValue() instanceof UserCredentials)) {
//            handleRedirect(response, "auth-admin");
//        }
//
//        UserCredentials userCreds = (UserCredentials) cred.getValue();
//
//        String username = userCreds.getUsername();
//        String apiKey = userCreds.getKey();
//
//        User user = this.userService.getUser(username);
//
//        if (user == null) {
//
//            if (useCloudAuth()) {
//                return cloudClient.post(getCloudAuthV11Url().concat("auth"),
//                    httpHeaders,
//                    IOUtils.toString(request.getInputStream(), "UTF-8"));
//
//            } else {
//                return notFoundExceptionResponse(username);
//            }
//        }
//
//        UserScopeAccess usa = null;
//
//        try {
//            usa = this.scopeAccessService
//                .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
//                    username, apiKey, getCloudAuthClientId());
//        } catch (NotAuthenticatedException nae) {
//            return notAuthenticatedExceptionResponse(username);
//        } catch (UserDisabledException ude) {
//            return userDisabledExceptionResponse(username);
//        }
//
//        List<CloudEndpoint> endpoints = this.endpointService
//            .getEndpointsForUser(username);
//
//        return Response.ok(
//            OBJ_FACTORY.createAuth(this.authConverter.toCloudv11AuthDataJaxb(
//                usa, endpoints))).build();
//    }

    // @POST
    // @Path("auth-admin")
    // public Response adminAuthenticate(@Context HttpHeaders httpHeaders,
    // @Context HttpServletRequest request,
    // @Context HttpServletResponse response,
    // JAXBElement<? extends Credentials> cred) throws IOException {
    //
    // if (cred.getValue() instanceof UserCredentials) {
    // handleRedirect(response, "auth");
    // }
    //
    // User user = null;
    //
    // if (cred.getValue() instanceof PasswordCredentials) {
    //
    // } else {
    // String apiKey = null;
    // if (cred.getValue() instanceof MossoCredentials) {
    // MossoCredentials mossoCreds = (MossoCredentials) cred
    // .getValue();
    // user = this.userService.getUserByMossoId(mossoCreds
    // .getMossoId());
    // apiKey = mossoCreds.getKey();
    // } else if (cred.getValue() instanceof NastCredentials) {
    // NastCredentials nastCreds = (NastCredentials) cred.getValue();
    // user = this.userService.getUserByNastId(nastCreds.getNastId());
    // apiKey = nastCreds.getKey();
    // }
    // }
    //
    // if (user == null) {
    //
    // if (useCloudAuth()) {
    // return cloudClient.post(getCloudAuthV11Url().concat("auth"),
    // httpHeaders,
    // IOUtils.toString(request.getInputStream(), "UTF-8"));
    //
    // } else {
    // return notFoundExceptionResponse(username);
    // }
    // }
    //
    // UserScopeAccess usa = null;
    //
    // try {
    // usa = this.scopeAccessService
    // .getUserScopeAccessForClientIdByUsernameAndApiCredentials(
    // username, apiKey, getCloudAuthClientId());
    // } catch (NotAuthenticatedException nae) {
    // return notAuthenticatedExceptionResponse(username);
    // } catch (UserDisabledException ude) {
    // return userDisabledExceptionResponse(username);
    // }
    //
    // List<CloudEndpoint> endpoints = this.endpointService
    // .getEndpointsForUser(username);
    //
    // return Response.ok(
    // this.authConverter.toCloudv11AuthDataJaxb(usa, endpoints)).build();
    // }

    private Response badRequestExceptionResponse(String message) {

        BadRequestFault fault = OBJ_FACTORY.createBadRequestFault();
        fault.setCode(HttpServletResponse.SC_BAD_REQUEST);
        fault.setMessage(message);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_BAD_REQUEST)
                .entity(OBJ_FACTORY.createBadRequest(fault))
                .build();
    }

    private Response.ResponseBuilder notFoundExceptionResponse(String username) {
        String errMsg = String.format("User %s not found", username);

        ItemNotFoundFault fault = OBJ_FACTORY.createItemNotFoundFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_NOT_FOUND)
                .entity(OBJ_FACTORY.createItemNotFound(fault))
                ;
    }

    private Response.ResponseBuilder notAuthenticatedExceptionResponse(String username) {
        String errMsg = String.format("User %s not authenticated", username);

        UnauthorizedFault fault = OBJ_FACTORY.createUnauthorizedFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .entity(OBJ_FACTORY.createUnauthorized(fault))
                ;
    }

    private Response.ResponseBuilder serviceExceptionResponse() {

        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .entity(OBJ_FACTORY.createAuthFault(fault));
    }

    private Response.ResponseBuilder userDisabledExceptionResponse(String username) {
        String errMsg = String.format("User %s is disabled", username);

        UserDisabledFault fault = OBJ_FACTORY.createUserDisabledFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_FORBIDDEN)
                .entity(OBJ_FACTORY.createUserDisabled(fault))
                ;
    }

    private void handleRedirect(HttpServletResponse response, String path) {
        try {
            response.sendRedirect(path);
        } catch (IOException e) {
            logger.error("Error in redirecting the " + path + " calls");
        }
    }

    private String getCloudAuthV11Url() {
        return config.getString("cloudAuth11url");
    }

    private boolean useCloudAuth() {
        return config.getBoolean("useCloudAuth", false);
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
}
