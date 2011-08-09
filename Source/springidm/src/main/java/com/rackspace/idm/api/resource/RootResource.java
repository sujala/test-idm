package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    private final Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public RootResource(CloudVersionsResource cloudVersionsResource, VersionResource versionResource, Configuration config) {
        this.cloudVersionsResource = cloudVersionsResource;
        this.versionResource = versionResource;
        this.config = config;
    }

    /**
     * Gets the API Versions info.
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
    public Response getVersionsInfo() {
        com.rackspace.idm.jaxb.Versions versions = new com.rackspace.idm.jaxb.Versions();
        com.rackspace.idm.jaxb.Version version = new com.rackspace.idm.jaxb.Version();
        version.setDocURL(config.getString("app.version.doc.url"));
        version.setId(config.getString("app.version"));
        version.setStatus(Enum.valueOf(
            com.rackspace.idm.jaxb.VersionStatus.class,
            config.getString("app.version.status").toUpperCase()));
        version.setWadl(config.getString("app.version.wadl.url"));
        versions.getVersions().add(version);
        return Response.ok(versions).build();
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
}
