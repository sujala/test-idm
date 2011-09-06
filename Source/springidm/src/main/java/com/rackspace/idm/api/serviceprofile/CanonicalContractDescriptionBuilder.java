package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.exception.NotFoundException;

@Component
public class CanonicalContractDescriptionBuilder extends AbstractContractDescriptionBuilder {

	public static final String VERSION_1_0 = "v1.0";
	
    @Autowired
	public CanonicalContractDescriptionBuilder(ApiDocDao apiDocDao, ServiceDescriptionTemplateUtil templateUtil) {
		super(apiDocDao, templateUtil);
	}
   
    public String buildInternalVersionPage(final String versionId, final UriInfo uriInfo) {
    	if (VERSION_1_0.equals(versionId)) {
    		String content = getFileContent("/docs/v1.0/InternalVersionPage.xml");
    		return build(content, uriInfo);
    	}
    	
    	 String errMsg = String.format("Version %s does not exist", versionId);
         throw new NotFoundException(errMsg);
    }
    
	@Override
	public String buildPublicVersionPage(String versionid, UriInfo uriInfo) {
		// the canonical contract does not have external facing contracts
    	throw new NotFoundException("Canonical contract for customer idm does not support public interface");
	}
}
