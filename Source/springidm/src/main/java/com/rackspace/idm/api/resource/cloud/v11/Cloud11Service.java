package com.rackspace.idm.api.resource.cloud.v11;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;

public interface Cloud11Service {

    // Token Methods
    Response.ResponseBuilder validateToken(HttpServletRequest request, String tokenId, String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder revokeToken(HttpServletRequest request, String tokenId, HttpHeaders httpHeaders) throws IOException;

    // Authenticate Methods
    Response.ResponseBuilder authenticate(HttpServletRequest request, HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder adminAuthenticate(HttpServletRequest request, HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException;
    
    // User Methods  
    Response.ResponseBuilder createUser(HttpServletRequest request, HttpHeaders httpHeaders, UriInfo uriInfo, User user) throws IOException, JAXBException;
    Response.ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserFromNastId(HttpServletRequest request, String nastId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder deleteUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder updateUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders, User user) throws IOException, JAXBException;
    Response.ResponseBuilder getUserEnabled(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder setUserEnabled(HttpServletRequest request, String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders) throws IOException, JAXBException;
    Response.ResponseBuilder getUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder setUserKey(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UserWithOnlyKey user) throws IOException, JAXBException;
    Response.ResponseBuilder getServiceCatalog(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getBaseURLRefs(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder addBaseURLRef(HttpServletRequest request, String userId, HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef) throws IOException, JAXBException;
    Response.ResponseBuilder getBaseURLRef(HttpServletRequest request, String userId, String baseURLId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder deleteBaseURLRef(HttpServletRequest request, String userId, String baseURLId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserGroups(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;

    // BaseURL Methods
    Response.ResponseBuilder getBaseURLs(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getBaseURLId(HttpServletRequest request, int baseURLId, String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getEnabledBaseURL(HttpServletRequest request, String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder addBaseURL(HttpServletRequest request, HttpHeaders httpHeaders, BaseURL baseUrl);
    
    // Migration Methods
    Response.ResponseBuilder migrate(HttpServletRequest request, String user, HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder unmigrate(HttpServletRequest request, String user, HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder all(HttpServletRequest request, HttpHeaders httpHeaders, String body) throws IOException;  
}
