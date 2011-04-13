package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.service.ApiDocService;

public class DefaultApiDocService implements ApiDocService {
    private static final String WADL_PATH = "/application.wadl";
    private static final String XSLT_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"></xsl:stylesheet>";
    private static final String XSD_PATH = "/xsd/";
    private ApiDocDao apiDocDao;

    public DefaultApiDocService(ApiDocDao apiDocDao) {
        this.apiDocDao = apiDocDao;
    }

    @Override
    public String getXsd(String fileName) {
        return apiDocDao.getContent(XSD_PATH + fileName);
    }

    @Override
    public String getXslt() {
        return XSLT_CONTENT;
    }

    @Override
    public String getWadl() {
        return apiDocDao.getContent(WADL_PATH);
    }

}
