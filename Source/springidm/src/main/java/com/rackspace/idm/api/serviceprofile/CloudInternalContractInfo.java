package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.MediaType;

import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.Relation;
import com.rackspace.idm.jaxb.Version;
import com.rackspace.idm.jaxb.VersionStatus;

public class CloudInternalContractInfo extends AbstractCloudContractInfo {

	public CloudInternalContractInfo(ServiceProfileUtil util, ServiceProfileConfig config) {
		super(util, config);
	}
    
    public Version createContractV1_0Version() {
    	// The only internal cloud auth 1.0 endpoint we will be supporting, is the same one as the external endpoint.
    	// We will not be supporting the internal xmlrpc, rpc calls  
    	CloudPublicContractInfo cloudPublicContractInfo = new CloudPublicContractInfo(util,config);
    	return cloudPublicContractInfo.createContractV1_0Version();
    }
    
    public Version createContractV1_1Version() {
    	com.rackspace.idm.jaxb.MediaType mediaType = util.createMediaType(config.getCloudAuth11MediaType(), null);
    	
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "cloud/v1.1", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", config.getCloudAuth11AdminWadl(), null);
    	Link adminDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, config.getCloudAuth11AdminDocumentation(), "Admin Developer Guide");
    	
    	Version version = new Version();
    	version.setId("v1.1");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(util.createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(adminDocumentationLink);

    	return version;
    }
    
    public Version createContractV2_0Version() {
    	com.rackspace.idm.jaxb.MediaType mediaType = util.createMediaType(config.getCloudAuth20MediaType(), null);
 
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "cloud/v2.0", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml", config.getCloudAuth20AdminWadl(), null);
    	Link adminDocumentationLink = createLink(Relation.DOCUMENTATION, MediaType.TEXT_HTML, config.getCloudAuth20AdminDocumentation(), "Admin Developer Guide");
    	
    	Version version = new Version();
    	version.setId("v2.0");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(util.createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(adminDocumentationLink);

    	return version;
    }
}
