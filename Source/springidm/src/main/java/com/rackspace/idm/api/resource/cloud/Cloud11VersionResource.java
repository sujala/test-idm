package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.cloud.jaxb.*;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
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
import java.io.IOException;

/**
 * Cloud Auth 1.1 API Versions
 * 
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

    private static final com.rackspace.idm.cloud.jaxb.ObjectFactory OBJ_FACTORY = new com.rackspace.idm.cloud.jaxb.ObjectFactory();

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
    public Response getCloud11VersionInfo(
    	@Context HttpHeaders httpHeaders		
    ) throws IOException {
        return cloudClient.get(getCloudAuthV11Url(), httpHeaders);
    }

    @POST
    @Path("auth{contentType:(\\.(xml|json))?}")
    public Response authenticate(
        @PathParam ("contentType") String contentType, @Context HttpHeaders httpHeaders, String body
    ) throws IOException {
        return cloudClient.post(getCloudAuthV11Url().concat(getPath("auth", contentType)), httpHeaders , body);
    }

    private String getPath(String path, String contentType) {
        if(contentType != null) {
            return path + contentType;
        }
        return path;
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

    private Response notFoundExceptionResponse(String username) {
        String errMsg = String.format("User %s not found", username);

        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_NOT_FOUND);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_NOT_FOUND)
            .entity(OBJ_FACTORY.createItemNotFound((com.rackspace.idm.cloud.jaxb.ItemNotFoundFault) fault))
            .build();
    }

    private Response notAuthenticatedExceptionResponse(String username) {
        String errMsg = String.format("User %s not authenticated", username);

        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
            .entity(OBJ_FACTORY.createUnauthorized((UnauthorizedFault) fault))
            .build();
    }

    private Response userDisabledExceptionResponse(String username) {
        String errMsg = String.format("User %s is disabled", username);

        AuthFault fault = OBJ_FACTORY.createAuthFault();
        fault.setCode(HttpServletResponse.SC_FORBIDDEN);
        fault.setMessage(errMsg);
        fault.setDetails(MDC.get(Audit.GUUID));

        return Response.status(HttpServletResponse.SC_FORBIDDEN)
            .entity(OBJ_FACTORY.createUserDisabled((UserDisabledFault) fault))
            .build();
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
    
    private Credentials unmarshallCredentialsFromJSON(String jsonBody) {
        
        JSONParser parser=new JSONParser();
        Credentials creds = null;
        
        Object obj;
        try {
            obj = parser.parse(jsonBody);
//            JSONArray array=(JSONArray)obj;
            JSONObject obj2=(JSONObject)obj;
            
            if (obj2.containsKey("credentials")) {
                JSONObject obj3 = (JSONObject)parser.parse(obj2.get("credentials").toString());
                UserCredentials userCreds = new UserCredentials();
                userCreds.setKey(obj3.get("username").toString());
                userCreds.setUsername(obj3.get("key").toString());
                creds = userCreds;
            }
            else if (obj2.containsKey("mossoCredentials")) {
                
            }
            else if (obj2.containsKey("nastCredentials")) {
                
            } else if (obj2.containsKey("passwordCredentials")) {
                
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

        
        
        return creds;
    }
}
