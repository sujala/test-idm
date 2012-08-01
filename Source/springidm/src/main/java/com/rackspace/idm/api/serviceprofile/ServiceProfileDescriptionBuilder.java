package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.domain.dao.ApiDocDao;
import freemarker.template.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

@Component
public class ServiceProfileDescriptionBuilder {

    private final ApiDocDao apiDocDao;
    private final ServiceDescriptionTemplateUtil templateUtil;
    
    @Autowired
	public ServiceProfileDescriptionBuilder(Configuration freemarkerConfig, ApiDocDao apiDocDao, ServiceDescriptionTemplateUtil templateUtil) {
		this.apiDocDao = apiDocDao;
		this.templateUtil = templateUtil;
	}
	
    public String buildPublicServiceProfile(final UriInfo uriInfo) {
    	String content = apiDocDao.getContent("/docs/PublicServiceProfile.xml");
    	return build(content, uriInfo);
    }
    
    public String buildInternalServiceProfile(final UriInfo uriInfo) {
    	String content = apiDocDao.getContent("/docs/InternalServiceProfile.xml");
    	return build(content, uriInfo);
    }
    
    String build(final String pattern, final UriInfo uriInfo) {
    	return templateUtil.build(pattern, uriInfo);
    }
}
