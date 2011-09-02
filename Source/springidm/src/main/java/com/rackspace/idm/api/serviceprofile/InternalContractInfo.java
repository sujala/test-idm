package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.jaxb.Contract;
import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.Relation;
import com.rackspace.idm.jaxb.Version;
import com.rackspace.idm.jaxb.VersionChoiceList;
import com.rackspace.idm.jaxb.VersionStatus;

public class InternalContractInfo extends AbstractContractInfo {

	public static final String VERSION_1_0 = "v1.0";
	
	public InternalContractInfo(ServiceProfileUtil util, ServiceProfileConfig config) {
		super(util, config);
	}
	
    public Contract createContractInfo() {
    	Version version1_0 = createCanonicalContractV1_0Version();
    	
    	VersionChoiceList versions = new VersionChoiceList();
    	versions.getVersions().add(version1_0);
    	
    	Contract contract = new Contract();
    	contract.setName("canonical");
    	contract.setVersions(versions);
    	
    	return contract;
    }
    
    public Version createContractVersion(final String versionId) {
    	if (VERSION_1_0.equals(versionId)) {
    		return createCanonicalContractV1_0Version();
    	}
    	
    	 String errMsg = String.format("Version %s does not exist", versionId);
         throw new NotFoundException(errMsg);
    }
    
    Version createCanonicalContractV1_0Version() {
    	com.rackspace.idm.jaxb.MediaType mediaType = util.createMediaType(config.getBaseUrl() + "docs/xsd/idmapi.xsd", "application/vnd.rackspace.idm-v1.0+xml" );
    	
    	Link selfLink = createLink(Relation.SELF, null, config.getBaseUrl() + "v1.0", null);
    	Link describedByLink = createLink(Relation.DESCRIBEDBY, "application/vnd.sun.wadl+xml",config.getBaseUrl() + "v1.0", null);
    	Link documentationLink = createLink(Relation.DOCUMENTATION, "application/pdf", config.getBaseUrl() + "docs/v1.0/developerguide.pdf", null);
    	
    	Version version = new Version();
    	version.setId("v1.0");
    	version.setStatus(VersionStatus.BETA);
    	version.setMediaTypes(util.createMediaTypeList(mediaType));
    	version.getLinks().add(selfLink);
    	version.getLinks().add(describedByLink);
    	version.getLinks().add(documentationLink);
    	
    	return version;
    }
}
