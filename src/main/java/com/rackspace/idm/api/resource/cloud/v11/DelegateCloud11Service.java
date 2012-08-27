package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.CloudUserExtractor;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
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
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class DelegateCloud11Service implements Cloud11Service {

    public static final String USERS = "users/";
    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private CloudUserExtractor cloudUserExtractor;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private CredentialUnmarshaller credentialUnmarshaller;

    @Autowired
    private Configuration config;

    @Autowired
    private LdapUserRepository ldapUserRepository;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Autowired
    private DefaultUserService defaultUserService;

    private static com.rackspacecloud.docs.auth.api.v1.ObjectFactory objFactory = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public static final String CLOUD_AUTH_11_URL = "cloudAuth11url";
    public static final String CLOUD_AUTH_ROUTING = "useCloudAuth";
    public static final String GA_SOURCE_OF_TRUTH = "gaIsSourceOfTruth";
    private Logger logger = LoggerFactory.getLogger(DelegateCloud11Service.class);

    public DelegateCloud11Service() throws JAXBException {
    }

    @Override
    public Response.ResponseBuilder validateToken(HttpServletRequest request, String tokenId, String belongsTo,
                                                  String type, HttpHeaders httpHeaders) throws IOException {

        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenId);
        if(scopeAccess != null) {
            return defaultCloud11Service.validateToken(request, tokenId, belongsTo, type, httpHeaders);
        } else {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("belongsTo", belongsTo);
            queryParams.put("type", type);
            String path = getCloudAuthV11Url().concat(getPath("token/" + tokenId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletRequest request, HttpServletResponse response,
                                                 HttpHeaders httpHeaders, String body) throws IOException, JAXBException, URISyntaxException {

         //Get "user" from LDAP
        JAXBElement<? extends Credentials> cred = extractCredentials(httpHeaders, body);
        com.rackspace.idm.domain.entity.User user = cloudUserExtractor.getUserByCredentialType(cred);
        if(defaultUserService.isMigratedUser(user)) {
            return defaultCloud11Service.authenticate(request, response, httpHeaders, body);
        }

         //Get Cloud Auth response
        String xmlBody = body;
        if (!httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            xmlBody = marshallObjectToString(cred);
        }
        Response.ResponseBuilder serviceResponse = cloudClient.post(getCloudAuthV11Url().concat("auth"), httpHeaders, xmlBody);
        Response dummyResponse = serviceResponse.clone().build();
         //If SUCCESS and "user" is not null, store token to "user" and return cloud response
        int status = dummyResponse.getStatus();
        if (status == HttpServletResponse.SC_MOVED_TEMPORARILY){
            serviceResponse.location(new URI(config.getString("ga.endpoint")+"cloud/v1.1/auth-admin"));
        }
        if (status == HttpServletResponse.SC_OK && user != null) {
            AuthData authResult = getAuthFromResponse(dummyResponse.getEntity().toString());
            if(authResult != null) {
                String token = authResult.getToken().getId();
                Date expires = authResult.getToken().getExpires().toGregorianCalendar().getTime();
                scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(user, getCloudAuthClientId(), token, expires);
            }
            return serviceResponse;
        }else if(user == null){ //If "user" is null return cloud response
            return serviceResponse;
        }
        else { //If we get this far, return Default Service Response
            return defaultCloud11Service.authenticate(request, response, httpHeaders, body);
        }
    }

    @Override
    public Response.ResponseBuilder adminAuthenticate(HttpServletRequest request, HttpServletResponse response,
                                                      HttpHeaders httpHeaders, String body) throws IOException, JAXBException {

        //Get "user" from LDAP
        JAXBElement<? extends Credentials> cred = extractCredentials(httpHeaders, body);

        com.rackspace.idm.domain.entity.User user = cloudUserExtractor.getUserByCredentialType(cred);

         //Get Cloud Auth response
        String xmlBody = body;
        if (!httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
            xmlBody = marshallObjectToString(cred);
        }
        Response.ResponseBuilder serviceResponse = cloudClient.post(getCloudAuthV11Url().concat("auth-admin"), httpHeaders, xmlBody);
        Response dummyResponse = serviceResponse.clone().build();
         //If SUCCESS and "user" is not null, store token to "user" and return cloud response
        int status = dummyResponse.getStatus();
        if (status == HttpServletResponse.SC_OK && user != null) {
            AuthData authResult = getAuthFromResponse(dummyResponse.getEntity().toString());
            if(authResult != null) {
                String token = authResult.getToken().getId();
                Date expires = authResult.getToken().getExpires().toGregorianCalendar().getTime();
                scopeAccessService.updateUserScopeAccessTokenForClientIdByUser(user, getCloudAuthClientId(), token, expires);
            }
            return serviceResponse;
        }else if(user == null){ //If "user" is null return cloud response
            return serviceResponse;
        }
        else { //If we get this far, return Default Service Response
            return defaultCloud11Service.adminAuthenticate(request, response, httpHeaders, body);
        }

        /*
        Response.ResponseBuilder serviceResponse = getCloud11Service().adminAuthenticate(request, response, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            if (!httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                body = marshallObjectToString(credentialUnmarshaller.unmarshallCredentialsFromJSON(body));
            }
            return cloudClient.post(getCloudAuthV11Url().concat("auth-admin"), httpHeaders, body);
        }
        return serviceResponse;
        */
    }

    @Override
    public Response.ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders) throws IOException {
        Response.ResponseBuilder cloudResponse = cloudClient.delete(getCloudAuthV11Url().concat("token/" + tokenId), httpHeaders);
        Response.ResponseBuilder defaultResponse = defaultCloud11Service.revokeToken(request, tokenId, httpHeaders);
        Response.ResponseBuilder clonedCloudResponse = cloudResponse.clone();
        int status = clonedCloudResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NO_CONTENT) {
            return cloudResponse;
        }
        return defaultResponse;

//        Response.ResponseBuilder serviceResponse = defaultCloud11Service.revokeToken(request, tokenId, httpHeaders);
//        // We have to clone the ResponseBuilder from above because once we build
//        // it below its gone.
//        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
//        int status = clonedServiceResponse.build().getStatus();
//        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
//            return cloudClient.delete(getCloudAuthV11Url().concat("token/" + tokenId), httpHeaders);
//        }
//        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGAByMossoId(mossoId)){
            return cloudClient.get(getCloudAuthV11Url().concat("mosso/" + mossoId), httpHeaders);
        }
        return defaultCloud11Service.getUserFromMossoId(request, mossoId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getUserFromNastId(HttpServletRequest request, String nastId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGAByNastId(nastId)){
            return cloudClient.get(getCloudAuthV11Url().concat("nast/" + nastId), httpHeaders);
        }
        return defaultCloud11Service.getUserFromNastId(request, nastId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLs(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders) throws IOException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(getPath("baseURLs", queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return defaultCloud11Service.getBaseURLs(request, serviceName, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLById(HttpServletRequest request, int baseURLId, String serviceName, HttpHeaders httpHeaders)
            throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(getPath("baseURLs/" + baseURLId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return defaultCloud11Service.getBaseURLById(request, baseURLId, serviceName, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getEnabledBaseURL(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders)
            throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(getPath("baseURLs/enabled", queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return defaultCloud11Service.getEnabledBaseURL(request, serviceName, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                               User user) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String body = this.marshallObjectToString(objFactory.createUser(user));
            return cloudClient.post(getCloudAuthV11Url().concat("users"), httpHeaders, body);
        }
        return defaultCloud11Service.createUser(request, httpHeaders, uriInfo, user);
    }

    @Override
    public Response.ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && ! userExistsInGA(userId)){
            return cloudClient.get(getCloudAuthV11Url().concat(USERS + userId), httpHeaders);
        }
        return defaultCloud11Service.getUser(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder deleteUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled()) {
            com.rackspace.idm.domain.entity.User user = defaultUserService.getSoftDeletedUserByUsername(userId);
            if(user == null){
                user = defaultUserService.getUser(userId);
            }
            if (user != null) {
                if (defaultUserService.isMigratedUser(user)) {
                    User updateUser = new User();
                    updateUser.setId(user.getUsername());
                    updateUser.setEnabled(false);
                    httpHeaders.getRequestHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
                    String body = marshallObjectToString(objFactory.createUser(updateUser));
                    ResponseBuilder response = cloudClient.put(getCloudAuthV11Url().concat(USERS + userId).concat("/enabled"), httpHeaders, body);
                    if (response.build().getStatus() == 200) {
                        return defaultCloud11Service.deleteUser(request, userId, httpHeaders);
                    } else {
                        throw new BadRequestException("Could not delete user");
                    }
                }
            }else{
                return cloudClient.delete(getCloudAuthV11Url().concat(USERS + userId), httpHeaders);
            }
        }
        return defaultCloud11Service.deleteUser(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                               User user) throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String body = this.marshallObjectToString(objFactory.createUser(user));
            return cloudClient.put(getCloudAuthV11Url().concat(USERS + userId), httpHeaders, body);
        }
        return defaultCloud11Service.updateUser(request, userId, httpHeaders, user);
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = USERS + userId + "/enabled";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getUserEnabled(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(HttpServletRequest request, String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders)
            throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = USERS + userId + "/enabled";
            String body = this.marshallObjectToString(objFactory.createUser(user));
            return cloudClient.put(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return defaultCloud11Service.setUserEnabled(request, userId, user, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = USERS + userId + "/key";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getUserKey(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UserWithOnlyKey user)
            throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String body = marshallObjectToString(objFactory.createUser(user));
            String path = USERS + userId + "/key";
            return cloudClient.put(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return defaultCloud11Service.setUserKey(request, userId, httpHeaders, user);
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = USERS + userId + "/serviceCatalog";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getServiceCatalog(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if (isCloudAuthRoutingEnabled() && !userExistsInGA(userId)) {
            String path = USERS + userId + "/baseURLRefs";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getBaseURLRefs(request, userId, httpHeaders);
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request, HttpHeaders httpHeaders, BaseURL baseUrl) throws JAXBException, IOException {
        if (!isCloudAuthRoutingEnabled() || isGASourceOfTruth()) {
            return defaultCloud11Service.addBaseURL(request, httpHeaders, baseUrl);
        }
        String body = marshallObjectToString(objFactory.createBaseURL(baseUrl));
        return cloudClient.post(getCloudAuthV11Url().concat("baseURLs"), httpHeaders, body);
    }

    @Override
    public Response.ResponseBuilder addBaseURLRef(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                                  UriInfo uriInfo, BaseURLRef baseUrlRef) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !userExistsInGA(userId)) {
            String body = this.marshallObjectToString(objFactory.createBaseURLRef(baseUrlRef));
            String path = USERS + userId + "/baseURLRefs";
            return cloudClient.post(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return defaultCloud11Service.addBaseURLRef(request, userId, httpHeaders, uriInfo, baseUrlRef);
    }

    @Override
    public Response.ResponseBuilder getBaseURLRef(HttpServletRequest request, String userId, String baseURLId, HttpHeaders httpHeaders)
            throws IOException {
        if (!isCloudAuthRoutingEnabled() || userExistsInGA(userId)) {
            return defaultCloud11Service.getBaseURLRef(request, userId, baseURLId, httpHeaders);
        }
        String path = USERS + userId + "/baseURLRefs/" + baseURLId;
        return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
                                                     HttpHeaders httpHeaders) throws IOException {
        if (!isCloudAuthRoutingEnabled() || userExistsInGA(userId)) {
            return defaultCloud11Service.deleteBaseURLRef(request, userId, baseURLId, httpHeaders);
        }
        String path = USERS + userId + "/baseURLRefs/" + baseURLId;
        return cloudClient.delete(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    @Override
    public Response.ResponseBuilder extensions(HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            return cloudClient.get(getCloudAuthV11Url().concat("extensions"),httpHeaders);
        }
        return defaultCloud11Service.extensions(httpHeaders);
    }

    @Override
    public ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            String path = "extensions/" + alias;
            return cloudClient.get(getCloudAuthV11Url().concat(path),httpHeaders);
        }
        return defaultCloud11Service.extensions(httpHeaders);
    }

    boolean userExistsInGAByMossoId(int mossoId){
        com.rackspace.idm.domain.entity.Users usersById = ldapUserRepository.getUsersByMossoId(mossoId);
        if(usersById.getUsers() == null) {
            return false;
        } if(usersById.getUsers().size() == 0) {
            return false;
        }
        return true;
    }

    boolean userExistsInGAByNastId(String nastId){
        com.rackspace.idm.domain.entity.Users usersById = ldapUserRepository.getUsersByNastId(nastId);
        if(usersById.getUsers() == null) {
            return false;
        } if (usersById.getUsers().size() == 0) {
            return false;
        }
        return true;
    }

    boolean userExistsInGA(String username) {
        com.rackspace.idm.domain.entity.User userById = ldapUserRepository.getUserByUsername(username);
        if (userById == null) {
            return false;
        }
        return true;
    }

    private boolean isGASourceOfTruth() {
        return config.getBoolean(GA_SOURCE_OF_TRUTH);
    }

    @Override
    public Response.ResponseBuilder getUserGroups(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if (!isCloudAuthRoutingEnabled() || userExistsInGA(userId)) {
            return defaultCloud11Service.getUserGroups(request, userId, httpHeaders);
        }
        String path = USERS + userId + "/groups";
        return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    String getPath(String path, Map<String, String> queryParams) {
        String result = path;
        String queryString = "";

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

    private String getCloudAuthV11Url() {
        return config.getString(CLOUD_AUTH_11_URL);
    }

    private boolean isCloudAuthRoutingEnabled() {
        return config.getBoolean(CLOUD_AUTH_ROUTING);
    }

    AuthData getAuthFromResponse(String entity) {
        try {
            JAXBContext jc = JAXBContext.newInstance(AuthData.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            StreamSource xml = new StreamSource(new StringReader(entity));
            JAXBElement ob = unmarshaller.unmarshal(xml, AuthData.class);
            return (AuthData)ob.getValue();
        } catch(Exception ex) {
            return null;
        }
    }

    private String marshallObjectToString(Object jaxbObject) throws JAXBException {

        Marshaller marshaller = JAXBContextResolver.get().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();

        try {
            marshaller.marshal(jaxbObject, sw);
        } catch (JAXBException e) {
            logger.info("failed to marshall object to string: " + e.getMessage());
        }
        return sw.toString();
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setCredentialUnmarshaller(CredentialUnmarshaller credentialUnmarshaller) {
        this.credentialUnmarshaller = credentialUnmarshaller;
    }

    public static void setObjFactory(ObjectFactory objFactory) {
        DelegateCloud11Service.objFactory = objFactory;
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public void setDefaultCloud11Service(DefaultCloud11Service defaultCloud11Service) {
        this.defaultCloud11Service = defaultCloud11Service;
    }

    public void setLdapUserRepository(LdapUserRepository ldapUserRepository) {
        this.ldapUserRepository = ldapUserRepository;
    }

    public void setCloudUserExtractor(CloudUserExtractor cloudUserExtractor) {
        this.cloudUserExtractor = cloudUserExtractor;
    }

    public void setDefaultUserService(DefaultUserService defaultUserService) {
        this.defaultUserService = defaultUserService;
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    JAXBElement<? extends Credentials> extractCredentials(HttpHeaders httpHeaders, String body) {
        if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                return extractXMLCredentials(body);
            } else {
                return extractJSONCredentials(body);
            }
    }

    JAXBElement<? extends Credentials> extractJSONCredentials(String body) {
        return credentialUnmarshaller.unmarshallCredentialsFromJSON(body);
    }
    JAXBElement<? extends Credentials> extractXMLCredentials(String body) {
        JAXBElement<? extends Credentials> cred = null;
        try {
            JAXBContext context = JAXBContextResolver.get();
            Unmarshaller unmarshaller = context.createUnmarshaller();
            cred = (JAXBElement<? extends Credentials>) unmarshaller.unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            logger.info("failed to extract XMLCredentials: " + e.getMessage());
        }
        return cred;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

}
