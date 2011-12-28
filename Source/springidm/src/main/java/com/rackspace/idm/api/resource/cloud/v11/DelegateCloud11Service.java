package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspace.idm.domain.service.UserService;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

@Component
public class DelegateCloud11Service implements Cloud11Service {

    @Autowired
    private CloudClient cloudClient;

    @Autowired
    private CredentialUnmarshaller credentialUnmarshaller;

    @Autowired
    private Configuration config;

    @Autowired
    private LdapUserRepository ldapUserRepository;

    @Autowired
    private DefaultCloud11Service defaultCloud11Service;

    @Autowired
    private DummyCloud11Service dummyCloud11Service;
    private static com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY = new com.rackspacecloud.docs.auth.api.v1.ObjectFactory();

    public static final String CLOUD_AUTH_11_URL = "cloudAuth11url";
    public static final String CLOUD_AUTH_ROUTING = "useCloudAuth";
    public static final String GA_SOURCE_OF_TRUTH = "gaIsSourceOfTruth";
    public DelegateCloud11Service() throws JAXBException {
    }

    @Override
    public Response.ResponseBuilder validateToken(HttpServletRequest request, String tokenId, String belongsTo,
                                                  String type, HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud11Service().validateToken(request, tokenId, belongsTo, type, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("belongsTo", belongsTo);
            queryParams.put("type", type);
            String path = getCloudAuthV11Url().concat(getPath("token/" + tokenId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder authenticate(HttpServletRequest request, HttpServletResponse response,
                                                 HttpHeaders httpHeaders, String body) throws IOException, JAXBException {

        Response.ResponseBuilder serviceResponse = getCloud11Service().authenticate(request, response, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            if (!httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                body = marshallObjectToString(credentialUnmarshaller.unmarshallCredentialsFromJSON(body));
            }
            return cloudClient.post(getCloudAuthV11Url().concat("auth"), httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder adminAuthenticate(HttpServletRequest request, HttpServletResponse response,
                                                      HttpHeaders httpHeaders, String body) throws IOException, JAXBException {

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
    }

    @Override
    public Response.ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud11Service().revokeToken(request, tokenId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            return cloudClient.delete(getCloudAuthV11Url().concat("token/" + tokenId), httpHeaders);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders)
            throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud11Service().getUserFromMossoId(request, mossoId, httpHeaders);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();
        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            return cloudClient.get(getCloudAuthV11Url().concat("mosso/" + mossoId), httpHeaders);
        }
        return serviceResponse;
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
        return getCloud11Service().getBaseURLs(request, serviceName, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLId(HttpServletRequest request, int baseURLId, String serviceName, HttpHeaders httpHeaders)
            throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("serviceName", serviceName);
            String path = getCloudAuthV11Url().concat(getPath("baseURLs/" + baseURLId, queryParams));
            return cloudClient.get(path, httpHeaders);
        }
        return defaultCloud11Service.getBaseURLId(request, baseURLId, serviceName, httpHeaders);
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
    public Response.ResponseBuilder migrate(HttpServletRequest request,
                                            String user, HttpHeaders httpHeaders, String body) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud11Service().migrate(
                request, user, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
                .clone();

        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND
                || status == HttpServletResponse.SC_UNAUTHORIZED) {
            String path = "migration/" + user + "/migrate";
            return cloudClient.post(getCloudAuthV11Url().concat(path),
                    httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder unmigrate(HttpServletRequest request,
                                              String user, HttpHeaders httpHeaders, String body) throws IOException {

        Response.ResponseBuilder serviceResponse = getCloud11Service()
                .unmigrate(request, user, httpHeaders, body);

        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse
                .clone();

        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND
                || status == HttpServletResponse.SC_UNAUTHORIZED) {
            String path = "migration/" + user + "/unmigrate";
            return cloudClient.post(getCloudAuthV11Url().concat(path),
                    httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder all(HttpServletRequest request, HttpHeaders httpHeaders, String body) throws IOException {
        Response.ResponseBuilder serviceResponse = getCloud11Service().all(request, httpHeaders, body);
        // We have to clone the ResponseBuilder from above because once we build
        // it below its gone.
        Response.ResponseBuilder clonedServiceResponse = serviceResponse.clone();

        int status = clonedServiceResponse.build().getStatus();
        if (status == HttpServletResponse.SC_NOT_FOUND || status == HttpServletResponse.SC_UNAUTHORIZED) {
            String path = "migration/all";
            return cloudClient.post(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return serviceResponse;
    }

    @Override
    public Response.ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                               User user) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !isGASourceOfTruth()) {
            String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.post(getCloudAuthV11Url().concat("users"), httpHeaders, body);
        }
        return defaultCloud11Service.createUser(request, httpHeaders, uriInfo, user);
    }

    @Override
    public Response.ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && ! userExistsInGA(userId)){
            return cloudClient.get(getCloudAuthV11Url().concat("users/" + userId), httpHeaders);
        }
        return defaultCloud11Service.getUser(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder deleteUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            return cloudClient.delete(getCloudAuthV11Url().concat("users/" + userId), httpHeaders);
        }
        return defaultCloud11Service.deleteUser(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                               User user) throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.put(getCloudAuthV11Url().concat("users/" + userId), httpHeaders, body);
        }
        return defaultCloud11Service.updateUser(request, userId, httpHeaders, user);
    }

    @Override
    public Response.ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = "users/" + userId + "/enabled";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getUserEnabled(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder setUserEnabled(HttpServletRequest request, String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders)
            throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = "users/" + userId + "/enabled";
            String body = this.marshallObjectToString(OBJ_FACTORY.createUser(user));
            return cloudClient.put(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return defaultCloud11Service.setUserEnabled(request, userId, user, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String path = "users/" + userId + "/key";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getUserKey(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UserWithOnlyKey user)
            throws IOException, JAXBException {
        if(isCloudAuthRoutingEnabled() && !userExistsInGA(userId)){
            String body = marshallObjectToString(OBJ_FACTORY.createUser(user));
            String path = "users/" + userId + "/key";
            return cloudClient.put(getCloudAuthV11Url().concat(path), httpHeaders, body);
        }
        return defaultCloud11Service.setUserKey(request, userId, httpHeaders, user);
    }

    @Override
    public Response.ResponseBuilder getServiceCatalog(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if(isCloudAuthRoutingEnabled() && !isGASourceOfTruth()){
            String path = "users/" + userId + "/serviceCatalog";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return defaultCloud11Service.getServiceCatalog(request, userId, httpHeaders);
    }

    @Override
    public Response.ResponseBuilder getBaseURLRefs(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException {
        if (isCloudAuthRoutingEnabled() && !userExistsInGA(userId)) {
            String path = "users/" + userId + "/baseURLRefs";
            return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
        }
        return getCloud11Service().getBaseURLRefs(request, userId, httpHeaders);
    }

    @Override
    public ResponseBuilder addBaseURL(HttpServletRequest request, HttpHeaders httpHeaders, BaseURL baseUrl) throws JAXBException, IOException {
        if (!isCloudAuthRoutingEnabled() || isGASourceOfTruth()) {
            return getCloud11Service().addBaseURL(request, httpHeaders, baseUrl);
        }
        String body = marshallObjectToString(OBJ_FACTORY.createBaseURL(baseUrl));
        return cloudClient.post(getCloudAuthV11Url().concat("baseURLs"), httpHeaders, body);
    }

    @Override
    public Response.ResponseBuilder addBaseURLRef(HttpServletRequest request, String userId, HttpHeaders httpHeaders,
                                                  UriInfo uriInfo, BaseURLRef baseUrlRef) throws IOException, JAXBException {
        if (isCloudAuthRoutingEnabled() && !userExistsInGA(userId)) {
            String body = this.marshallObjectToString(OBJ_FACTORY.createBaseURLRef(baseUrlRef));
            String path = "users/" + userId + "/baseURLRefs";
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
        String path = "users/" + userId + "/baseURLRefs/" + baseURLId;
        return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    @Override
    public Response.ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId,
                                                     HttpHeaders httpHeaders) throws IOException {
        if (!isCloudAuthRoutingEnabled() || userExistsInGA(userId)) {
            return defaultCloud11Service.deleteBaseURLRef(request, userId, baseURLId, httpHeaders);
        }
        String path = "users/" + userId + "/baseURLRefs/" + baseURLId;
        return cloudClient.delete(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    private Cloud11Service getCloud11Service() {
        if (config.getBoolean("GAKeystoneDisabled")) {
            return dummyCloud11Service;
        } else {
            return defaultCloud11Service;
        }
    }

    boolean userExistsInGAByNastId(String nastId){
        com.rackspace.idm.domain.entity.User userById = ldapUserRepository.getUserByNastId(nastId);
        if (userById == null) {
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
        String path = "users/" + userId + "/groups";
        return cloudClient.get(getCloudAuthV11Url().concat(path), httpHeaders);
    }

    private String getPath(String path, HashMap<String, String> queryParams) {
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

    private String marshallObjectToString(Object jaxbObject) throws JAXBException {

        Marshaller marshaller = JAXBContextResolver.get().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();

        try {
            marshaller.marshal(jaxbObject, sw);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sw.toString();
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setCredentialUnmarshaller(CredentialUnmarshaller credentialUnmarshaller) {
        this.credentialUnmarshaller = credentialUnmarshaller;
    }

    public static void setOBJ_FACTORY(ObjectFactory OBJ_FACTORY) {
        DelegateCloud11Service.OBJ_FACTORY = OBJ_FACTORY;
    }

    public void setCloudClient(CloudClient cloudClient) {
        this.cloudClient = cloudClient;
    }

    public void setDefaultCloud11Service(DefaultCloud11Service defaultCloud11Service) {
        this.defaultCloud11Service = defaultCloud11Service;
    }

    public void setMarshaller(Marshaller marshaller) {
    }

    public void setDummyCloud11Service(DummyCloud11Service dummyCloud11Service) {
        this.dummyCloud11Service = dummyCloud11Service;
    }

    public void setLdapUserRepository(LdapUserRepository ldapUserRepository) {
        this.ldapUserRepository = ldapUserRepository;
    }
}
