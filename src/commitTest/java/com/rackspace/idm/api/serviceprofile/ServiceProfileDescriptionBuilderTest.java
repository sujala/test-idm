package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.domain.dao.ApiDocDao;
import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceProfileDescriptionBuilderTest {
    private ServiceProfileDescriptionBuilder serviceProfileDescriptionBuilder;
    private ApiDocDao apiDocDao;
    private ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;

    @Before
    public void setUp() throws Exception {
        apiDocDao = mock(ApiDocDao.class);
        serviceDescriptionTemplateUtil = mock(ServiceDescriptionTemplateUtil.class);
        serviceProfileDescriptionBuilder = new ServiceProfileDescriptionBuilder(apiDocDao, serviceDescriptionTemplateUtil);
    }

    @Test
    public void buildPublicServiceProfile_callsApiDocDao_getContent() throws Exception {
        serviceProfileDescriptionBuilder.buildPublicServiceProfile(null);
        verify(apiDocDao).getContent("/docs/PublicServiceProfile.xml");
    }

    @Test
    public void buildPublicServiceProfile_callsServiceTemplateUtil_build() throws Exception {
        serviceProfileDescriptionBuilder.buildPublicServiceProfile(null);
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildInternalServiceProfile_callsApiDocDao_getContect() throws Exception {
        serviceProfileDescriptionBuilder.buildInternalServiceProfile(null);
        verify(apiDocDao).getContent("/docs/InternalServiceProfile.xml");
    }

    @Test
    public void buildInternalServiceProfile_callsServiceTemplateUtil_build() throws Exception {
        serviceProfileDescriptionBuilder.buildInternalServiceProfile(null);
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void build_callsServiceTemplateUtil_build() throws Exception {
        serviceProfileDescriptionBuilder.build(null, null);
        verify(serviceDescriptionTemplateUtil).build(null, (UriInfo)null);
    }
}
