package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;

/**
 * Cloud Auth 2.0 API Versions
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class Cloud20VersionResource {
    
    private final Configuration config;
    private final CloudClient cloudClient;
    private final CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    
    @Context
    private UriInfo uriInfo;
    
    @Autowired
    public Cloud20VersionResource(Configuration config, CloudClient cloudClient, 
    		 CloudContractDescriptionBuilder cloudContractDescriptionBuilder) {
        this.config = config;
        this.cloudClient = cloudClient;
        this.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder;
    }

    @GET()
    @Path("public")
    public Response getPublicCloud20VersionInfo(
    	@Context HttpHeaders httpHeaders
    ) throws IOException {
    	//For the pubic profile, we're just forwarding to what cloud has. Once we become the
    	//source of truth, we should use the CloudContractDescriptorBuilder to render this.
        return cloudClient.get(getCloudAuthV20Url(), httpHeaders).build();
    }
    
    @GET
    public Response getInternalCloud20VersionInfo() {
       	final String responseXml = cloudContractDescriptionBuilder.buildInternalVersionPage(CloudContractDescriptionBuilder.VERSION_2_0, uriInfo);
    	return Response.ok(responseXml).build();
    }
    
    private String getCloudAuthV20Url() {
        return config.getString("cloudAuth20url");
    }
}
