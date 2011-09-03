package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.exception.NotFoundException;

@Component
public class CloudContractDescriptionBuilder extends AbstractContractDescriptionBuilder {

	public static final String VERSION_1_0 = "v1.0";
	public static final String VERSION_1_1 = "v1.1";
	public static final String VERSION_2_0 = "v2.0";
	
    @Autowired
	public CloudContractDescriptionBuilder(ApiDocDao apiDocDao, ServiceDescriptionTemplateUtil templateUtil) {
		super(apiDocDao, templateUtil);
	}
   
    public String buildInternalRootPage(final UriInfo uriInfo) {
    	String content = getFileContent("/docs/cloud/InternalContractPage.xml");
    	return build(content, uriInfo);
    }

    public String buildPublicRootPage(final UriInfo uriInfo) {
    	String content = getFileContent("/docs/cloud/PublicContractPage.xml");
    	return build(content, uriInfo);
    }
    
    public String buildInternalVersionPage(final String versionId, final UriInfo uriInfo) {
    	String content = null;
    	if (VERSION_1_0.equals(versionId)) {
    		// version 1.0 internal version page will be the public one, since we are not
    		// implementing the management api functions. The public version page is always
    		// a subset of the management api page. At a minimum we need to support the
    		// public version capabilities
    		content = getFileContent("/docs/cloud/v1.0/PublicVersionPage.xml");
    	}
    	else if (VERSION_1_1.equals(versionId)) {
    		content = getFileContent("/docs/cloud/v1.1/InternalVersionPage.xml");
    	}
    	else if (VERSION_2_0.equals(versionId)) {
    		content = getFileContent("/docs/cloud/v2.0/InternalVersionPage.xml");
    	}
    	else {
    		String errMsg = String.format("Version %s does not exist", versionId);
    		throw new NotFoundException(errMsg);
    	}
    	
     	return build(content, uriInfo);
    }
    
	@Override
	public String buildPublicVersionPage(String versionId, UriInfo uriInfo) {
	  	String content = null;
    	if (VERSION_1_0.equals(versionId)) {
    		content = getFileContent("/docs/cloud/v1.0/PublicVersionPage.xml");
    	}
    	else if (VERSION_1_1.equals(versionId)) {
    		content = getFileContent("/docs/cloud/v1.1/PublicVersionPage.xml");
    	}
    	else if (VERSION_2_0.equals(versionId)) {
    		content = getFileContent("/docs/cloud/v2.0/PublicVersionPage.xml");
    	}
    	else {
    		String errMsg = String.format("Version %s does not exist", versionId);
    		throw new NotFoundException(errMsg);
    	}
    	
     	return build(content, uriInfo);
	}
}
