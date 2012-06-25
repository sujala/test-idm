package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApiDocDao;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/22/12
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultApiDocServiceTest {

    DefaultApiDocService defaultApiDocService;
    ApiDocDao apiDocDao = mock(ApiDocDao.class);

    @Before
    public void setUp() throws Exception {
        defaultApiDocService = new DefaultApiDocService(apiDocDao);
    }

    @Test
    public void getXsd_returnsString() throws Exception {
        when(apiDocDao.getContent("/xsd/fileName")).thenReturn("test");
        assertThat("xsd", defaultApiDocService.getXsd("fileName"), equalTo("test"));
    }

    @Test
    public void getXslt_returnsString() throws Exception {
        assertThat("xslt",defaultApiDocService.getXslt(),equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xsl:stylesheet version=\"1.0\" " +
                "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"></xsl:stylesheet>"));
    }

    @Test
    public void getWadl_returnsString() throws Exception {
        when(apiDocDao.getContent("/idm.wadl")).thenReturn("test");
        assertThat("wadl",defaultApiDocService.getWadl(),equalTo("test"));
    }
}
