package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.MediaType;

import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.Relation;
import com.rackspace.idm.jaxb.Version;
import com.rackspace.idm.jaxb.VersionStatus;

public class CloudPublicContractInfo extends AbstractCloudContractInfo {
    
	public CloudPublicContractInfo(ServiceProfileUtil util, ServiceProfileConfig config) {
		super(util, config);
	}

    public Version createContractV1_0Version() {
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "cloud/v1.0", null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, config.getCloudAuth10ServiceDocumentation(), "Auth Guide");
    	
    	Version version = new Version();
    	version.setId("v1.0");
    	version.setStatus(VersionStatus.CURRENT);
    	version.getLinks().add(selfLink);
    	version.getLinks().add(documentationLink);
    	
    	return version;
    }
    
    public Version createContractV1_1Version() {
    	com.rackspace.idm.jaxb.MediaType mediaType = util.createMediaType(config.getCloudAuth11MediaType(), null);
    	
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "cloud/v1.1", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", config.getCloudAuth11ServiceWadl(), null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, config.getCloudAuth11ServiceDocumentation(), "Service Developer Guide");
    	
    	Version version = new Version();
    	version.setId("v1.1");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(util.createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(documentationLink);

    	return version;
    }
    
    public Version createContractV2_0Version() {
    	com.rackspace.idm.jaxb.MediaType mediaType = util.createMediaType(config.getCloudAuth20MediaType(), null);
 
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "cloud/v2.0", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", config.getCloudAuth20ServiceWadl(), null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, config.getCloudAuth20ServiceDocumentation(), "Service Developer Guide");
    	
    	Version version = new Version();
    	version.setId("v2.0");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(util.createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(documentationLink);

    	return version;
    }
}
