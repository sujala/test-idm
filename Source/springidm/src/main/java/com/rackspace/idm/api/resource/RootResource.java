package com.rackspace.idm.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.serviceprofile.ServiceProfileConfig;
import com.rackspace.idm.api.serviceprofile.ServiceProfileInfo;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.ServiceProfile;

/**
 * API Versions
 * 
 */
@Path("/")
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class RootResource {

    private final CloudVersionsResource cloudVersionsResource;
    private final VersionResource versionResource;
    private Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    public RootResource(CloudVersionsResource cloudVersionsResource, VersionResource versionResource, Configuration config) {
        this.cloudVersionsResource = cloudVersionsResource;
        this.versionResource = versionResource;
        this.config = config;
    }

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
        ServiceProfileInfo serviceProfileInfo = createServiceProfileInfo();
        
    	ServiceProfile serviceProfile = serviceProfileInfo.createInternalServiceProfile();
    	
    	return Response.ok(serviceProfile).build();
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
    	ServiceProfileInfo serviceProfileInfo = createServiceProfileInfo();
        
    	ServiceProfile serviceProfile = serviceProfileInfo.createExternalServiceProfile();
    	
    	return Response.ok(serviceProfile).build();
    }
    
    @Path("cloud/")
    public CloudVersionsResource getCloudVersionsResource() {
        return cloudVersionsResource;
    }

    @Path("{versionId: v[1-9].[0-9]}")
    public VersionResource getVersionResource(@PathParam("versionId") String versionId) {
        if (versionId.equalsIgnoreCase("v1.0")) {
            return versionResource;
        }
        
        String errMsg = String.format("Version %s does not exist", versionId);
        logger.warn(errMsg);
        throw new NotFoundException(errMsg);
    }
    
    private ServiceProfileInfo createServiceProfileInfo() {
        ServiceProfileConfig serviceProfileConfig = new ServiceProfileConfig(config, uriInfo);
        ServiceProfileInfo serviceProfileInfo = new ServiceProfileInfo(serviceProfileConfig);
        
        return serviceProfileInfo;
    }
}
