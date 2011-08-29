package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.rackspace.idm.cloudv11.jaxb.BaseURL;
import com.rackspace.idm.cloudv11.jaxb.BaseURLRef;
import com.rackspace.idm.cloudv11.jaxb.User;
import com.rackspace.idm.cloudv11.jaxb.UserWithOnlyEnabled;
import com.rackspace.idm.cloudv11.jaxb.UserWithOnlyKey;

public interface Cloud11Service {

    Response.ResponseBuilder validateToken(String tokenId, String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder authenticate(HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder revokeToken(String tokenId, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserFromNastId(HttpServletRequest request, String nastId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getBaseURLs(String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getBaseURLId(int baseURLId, String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getEnabledBaseURL(String serviceName, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder migrate(String user, HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder unmigrate(String user, HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder all(HttpHeaders httpHeaders, String body) throws IOException;
    Response.ResponseBuilder createUser(HttpHeaders httpHeaders, User user) throws IOException;
    Response.ResponseBuilder getUser(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder deleteUser(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder updateUser(String userId, HttpHeaders httpHeaders, User user) throws IOException;
    Response.ResponseBuilder getUserEnabled(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder setUserEnabled(String userId, UserWithOnlyEnabled user, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserKey(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder setUserKey(String userId, HttpHeaders httpHeaders, UserWithOnlyKey user) throws IOException;
    Response.ResponseBuilder getServiceCatalog(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getBaseURLRefs(String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder addBaseURLRef(String userId, HttpHeaders httpHeaders, UriInfo uriInfo, BaseURLRef baseUrlRef) throws IOException;
    Response.ResponseBuilder getBaseURLRef(String userId, String baseURLId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder deleteBaseURLRef(String userId, String baseURLId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserGroups(String userId, HttpHeaders httpHeaders) throws IOException;

    ResponseBuilder adminAuthenticate(HttpServletResponse response,
        HttpHeaders httpHeaders, String body) throws IOException;

    ResponseBuilder addBaseURL(HttpServletRequest request,
        HttpHeaders httpHeaders, BaseURL baseUrl);
}
