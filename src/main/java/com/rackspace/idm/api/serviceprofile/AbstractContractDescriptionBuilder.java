package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.domain.dao.ApiDocDao;

import javax.ws.rs.core.UriInfo;

public abstract class AbstractContractDescriptionBuilder {

    private final ApiDocDao apiDocDao;
    private final ServiceDescriptionTemplateUtil templateUtil;
    
	public AbstractContractDescriptionBuilder(ApiDocDao apiDocDao, ServiceDescriptionTemplateUtil templateUtil) {
		this.apiDocDao = apiDocDao;
		this.templateUtil = templateUtil;
	}
    
    public abstract String buildInternalVersionPage(final String versionid, final UriInfo uriInfo);
    public abstract String buildPublicVersionPage(final String versionid, final UriInfo uriInfo);
    
    protected String build(final String pattern, final UriInfo uriInfo) {
    	return templateUtil.build(pattern, uriInfo);
    }

    protected String build(final String pattern, final String uriInfo) {
    	return templateUtil.build(pattern, uriInfo);
    }
    
    protected String getFileContent(final String fileLocation) {
    	return apiDocDao.getContent(fileLocation);
    }
}
