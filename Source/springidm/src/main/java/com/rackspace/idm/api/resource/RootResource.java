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
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.Contract;
import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.MediaTypeList;
import com.rackspace.idm.jaxb.Relation;
import com.rackspace.idm.jaxb.ServiceModel;
import com.rackspace.idm.jaxb.ServiceProfile;
import com.rackspace.idm.jaxb.VersionChoice;
import com.rackspace.idm.jaxb.VersionChoiceList;
import com.rackspace.idm.jaxb.VersionStatus;

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

    @Context
    private UriInfo uriInfo;
    
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
    public Response getServiceProfile() {
    	ServiceProfile serviceProfile = new ServiceProfile();
    	serviceProfile.setName("Customer Identity Management");
    	serviceProfile.setCanonicalName("idm");
    	serviceProfile.setDnsZone("idm.api.rackspace.com");
    	serviceProfile.setServiceModel(ServiceModel.UTILITY);
    	serviceProfile.setShortDescription("Allows users access Rackspace resources and systems.");
    	serviceProfile.setDetailedDescription("The global auth api allows Rackspace clients to obtain tokens that can be used to access resources in Rackspace. It also allows clients manage identities and delegate access to resources.");
    	
    	Link selfLink = createLink(Relation.SELF, null, uriInfo.getBaseUri().toString(), null); 
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, MediaType.TEXT_HTML, "http://serviceregistry.rackspace.com/services/idm", null); 
    	
    	serviceProfile.getLinks().add(selfLink);
    	serviceProfile.getLinks().add(describedByLink);
    	serviceProfile.getContracts().add(createCanonicalContract());
    	serviceProfile.getContracts().add(createCloudContract());
    
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
    
    private Contract createCanonicalContract() {
    	com.rackspace.idm.jaxb.MediaType mediaType = createMediaType(uriInfo.getBaseUri().toString() + "docs/xsd/idmapi.xsd", "application/vnd.rackspace.idm-v1.0+xml" );
    	
    	Link selfLink = createLink(Relation.SELF, null, uriInfo.getBaseUri().toString() + "v1.0", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", uriInfo.getBaseUri().toString() + "v1.0", null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, "application/pdf", uriInfo.getBaseUri().toString() + "docs/v1.0/developerguide.pdf", null);
    	
    	VersionChoice version = new VersionChoice();
    	version.setId("v1.0");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(documentationLink);
    	
    	VersionChoiceList versions = new VersionChoiceList();
    	versions.getVersions().add(version);
    	
      	Contract contract = new Contract();
    	contract.setName("canonical");
    	contract.setVersions(versions);
    	
    	return contract;
    }
    
    private Contract createCloudContract() {
    	VersionChoice cloudv1_0Version = createCloudV10ContractVersion();
    	VersionChoice cloudv1_1Version = createCloudV11ContractVersion();
    	VersionChoice cloudv2_0Version = createCloudV20ContractVersion();
    	
    	VersionChoiceList versions = new VersionChoiceList();
    	versions.getVersions().add(cloudv1_0Version);
       	versions.getVersions().add(cloudv1_1Version);
    	versions.getVersions().add(cloudv2_0Version);
    	
    	Contract contract = new Contract();
    	contract.setName("cloud auth");
    	contract.setVersions(versions);
    	
    	return contract;
    }
    
    private VersionChoice createCloudV10ContractVersion() {
    	Link selfLink = createLink(Relation.SELF, null, uriInfo.getBaseUri().toString() + "cloud/v1.0", null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, getCloudAuth10Documentation(), "Auth Guide");
    	
    	VersionChoice version = new VersionChoice();
    	version.setId("v1.0");
    	version.setStatus(VersionStatus.CURRENT);
    	version.getLinks().add(selfLink);
    	version.getLinks().add(documentationLink);
    	
    	return version;
    }
    
    private VersionChoice createCloudV11ContractVersion() {
    	com.rackspace.idm.jaxb.MediaType mediaType = createMediaType(getCloudAuth11MediaType(), null);
    	
    	Link selfLink = createLink(Relation.SELF, null, uriInfo.getBaseUri().toString() + "cloud/v1.1", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", getCloudAuth11Wadl(), null);
    	Link serviceDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, getCloudAuth11ServiceDocumentation(), "Service Developer Guide");
    	Link adminDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, getCloudAuth11AdminDocumentation(), "Admin Developer Guide");
    	
    	VersionChoice version = new VersionChoice();
    	version.setId("v1.1");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(serviceDocumentationLink);
    	version.getLinks().add(adminDocumentationLink);

    	return version;
    }
    
    private VersionChoice createCloudV20ContractVersion() {
    	com.rackspace.idm.jaxb.MediaType mediaType = createMediaType(getCloudAuth20MediaType(), null);
 
    	Link selfLink = createLink(Relation.SELF, null, uriInfo.getBaseUri().toString() + "cloud/v2.0", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", getCloudAuth20Wadl(), null);
    	Link serviceDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, getCloudAuth20ServiceDocumentation(), "Service Developer Guide");
    	Link adminDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, getCloudAuth20AdminDocumentation(), "Admin Developer Guide");
    	
    	VersionChoice version = new VersionChoice();
    	version.setId("v2.0");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(serviceDocumentationLink);
    	version.getLinks().add(adminDocumentationLink);

    	return version;
    }
    
    private Link createLink(final Relation rel, final String type, final String href, final String title) {
    	Link link = new Link();
    	link.setRel(rel);
    	link.setHref(href);
    	link.setType(type);
    	link.setTitle(title);
    	return link;
    }
    
    /**
     * helper method that creates media type list from single media type
     * @param mediaType
     * @return
     */
    private MediaTypeList createMediaTypeList(com.rackspace.idm.jaxb.MediaType mediaType) {
    	MediaTypeList mediaTypes = new MediaTypeList();
    	mediaTypes.getMediaTypes().add(mediaType);
    	return mediaTypes;
    }
    
    private com.rackspace.idm.jaxb.MediaType createMediaType(final String describedByLink, final String type) {
    	com.rackspace.idm.jaxb.MediaType mediaType = new com.rackspace.idm.jaxb.MediaType();
		mediaType.setBase(MediaType.APPLICATION_XML);
		mediaType.setType(type);
		mediaType.getLinks().add(createLink(Relation.DESCRIBEDBY, MediaType.APPLICATION_XML, describedByLink, null));
		return mediaType;
    }

    private String getCloudAuth10Documentation() {
    	return config.getString("serviceProfile.cloudAuth10.documentation");
    }

    private String getCloudAuth11MediaType() {
    	return config.getString("serviceProfile.cloudAuth11.mediaType");
    }
    
    private String getCloudAuth11Wadl() {
    	return config.getString("serviceProfile.cloudAuth11.wadl");
    }
    
    private String getCloudAuth11ServiceDocumentation() {
    	return config.getString("serviceProfile.cloudAuth11.documentation.service");
    }
    
    private String getCloudAuth11AdminDocumentation() {
    	return config.getString("serviceProfile.cloudAuth11.documentation.admin");
    }

    private String getCloudAuth20MediaType() {
    	return config.getString("serviceProfile.cloudAuth20.mediaType");
    }
    
    private String getCloudAuth20Wadl() {
    	return config.getString("serviceProfile.cloudAuth20.wadl");
    }
    
    private String getCloudAuth20ServiceDocumentation() {
    	return config.getString("serviceProfile.cloudAuth20.documentation.service");
    }
    
    private String getCloudAuth20AdminDocumentation() {
    	return config.getString("serviceProfile.cloudAuth20.documentation.admin");
    }
}
