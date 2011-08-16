package com.rackspace.idm.api.resource.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {
    @Autowired
    private CloudClient cloudClient;

    @Value("#{properties.cloudAuth20url}")
    private String url;

    @GET
    public Response getCloud20VersionInfo() throws IOException {
        return cloudClient.get(url,null,null);
    }
}
