package com.rackspace.idm.api.resource.cloud.v20;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:15 PM
 */
public interface Cloud20Service {
    Response.ResponseBuilder authenticate(HttpHeaders httpHeaders, String body) throws IOException;
}
