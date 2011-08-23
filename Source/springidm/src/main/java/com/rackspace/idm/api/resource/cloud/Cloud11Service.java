package com.rackspace.idm.api.resource.cloud;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

public interface Cloud11Service {

    Response.ResponseBuilder validateToken(String tokenId, String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder authenticate(HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder revokeToken(String tokenId, HttpHeaders httpHeaders) throws IOException;

    <T> Response.ResponseBuilder userRedirect(T nastId, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getBaseURLs(String serviceName, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getBaseURLId(int baseURLId, String serviceName, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder getEnabledBaseURL(String serviceName, HttpHeaders httpHeaders) throws IOException;

    Response.ResponseBuilder migrate(String user, HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder unmigrate(String user, HttpHeaders httpHeaders, String body) throws IOException;

    Response.ResponseBuilder all(HttpHeaders httpHeaders, String body) throws IOException;
}
