package com.rackspace.idm.api.resource.cloud;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public interface Cloud11Service {

    Response.ResponseBuilder validateToken(String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;

}
