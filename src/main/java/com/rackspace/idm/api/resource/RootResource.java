package com.rackspace.idm.api.resource;


import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.serviceprofile.ServiceProfileDescriptionBuilder;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * API Versions
 * 
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RootResource {

    @Autowired
    private CloudVersionsResource cloudVersionsResource;
    @Autowired
    private DevOpsResource devOpsResource;
    @Autowired
    private ServiceProfileDescriptionBuilder serviceProfileDescriptionBuilder;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Configuration config;

    @Context
    private UriInfo uriInfo;

    /**
     * Gets the internal service profile. The root resource defaults to the internal service profile.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}versions
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    public Response getInternalServiceProfile() {
    	final String responseXml = serviceProfileDescriptionBuilder.buildInternalServiceProfile(uriInfo);
    	return Response.ok(responseXml).build();
    }
    
    /**
     * Gets the public service profile.
     *
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}versions
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @GET
    @Path("public")
    public Response getPublicServiceProfile() {
       	final String responseXml = serviceProfileDescriptionBuilder.buildPublicServiceProfile(uriInfo);
    	return Response.ok(responseXml).build();
    }
    
    @Path("cloud")
    public CloudVersionsResource getCloudVersionsResource() {
        return cloudVersionsResource;
    }

    @Path("devops")
    public DevOpsResource getDevOpsResource() {
        return devOpsResource;
    }

    @Path("buildInfo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getBuildInfo() {
        JSONObject version = new JSONObject();
        version.put("version", config.getString("version"));
        version.put("build", config.getString("buildVersion"));
        return version.toJSONString();
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public void setCloudVersionResource(CloudVersionsResource cloudVersionResource) {
        this.cloudVersionsResource = cloudVersionResource;
    }

    public void setServiceProfileDescriptionBuilder(ServiceProfileDescriptionBuilder serviceProfileDescriptionBuilder) {
        this.serviceProfileDescriptionBuilder = serviceProfileDescriptionBuilder;
    }
}
