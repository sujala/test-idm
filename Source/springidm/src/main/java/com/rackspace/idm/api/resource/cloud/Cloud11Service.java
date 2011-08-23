package com.rackspace.idm.api.resource.cloud;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

public interface Cloud11Service {

    Response.ResponseBuilder validateToken(String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder authenticate(HttpServletResponse response, HttpHeaders httpHeaders, String body) throws IOException;
}
