package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.WadlTrie;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudContractDescriptionBuilderTest {
    private CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    private ApiDocDao apiDocDao;
    private ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;

    @Before
    public void setUp() throws Exception {
        apiDocDao = mock(ApiDocDao.class);
        serviceDescriptionTemplateUtil = mock(ServiceDescriptionTemplateUtil.class);
        cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(apiDocDao, serviceDescriptionTemplateUtil);
    }

    @Test
    public void buildInternalRootPage_callsFileContent_rootUrl() throws Exception {
        cloudContractDescriptionBuilder.buildInternalRootPage();
        verify(apiDocDao).getContent("/docs/cloud/versions.xml");
    }

    @Test
    public void buildVersion11Page_callsFileContent_version11Url() throws Exception {
        cloudContractDescriptionBuilder.buildVersion11Page();
        verify(apiDocDao).getContent("/docs/cloud/v1.1/version11.xml");
    }

    @Test
    public void buildVersion20Page_callsFileContent_version20Url() throws Exception {
        cloudContractDescriptionBuilder.buildVersion20Page();
        verify(apiDocDao).getContent("/docs/cloud/v2.0/version20.xml");
    }

    @Test
    public void buildPublicRootPage_callsFileContent_publicRootUrl() throws Exception {
        cloudContractDescriptionBuilder.buildPublicRootPage(null);
        verify(apiDocDao).getContent("/docs/cloud/PublicContractPage.xml");
    }

    @Test
    public void buildPublicRootPage_callsServiceTemplate_build() throws Exception {
        cloudContractDescriptionBuilder.buildPublicRootPage(null);
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildInternalVersionPage_versionIdIsV10_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildInternalVersionPage("v1.0", null);
        verify(apiDocDao).getContent("/docs/cloud/v1.0/PublicVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildInternalVersionPage_versionIdIsV11_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildInternalVersionPage("v1.1", null);
        verify(apiDocDao).getContent("/docs/cloud/v1.1/InternalVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildInternalVersionPage_versionIdIsV20_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildInternalVersionPage("v2.0", null);
        verify(apiDocDao).getContent("/docs/cloud/v2.0/InternalVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test (expected = NotFoundException.class)
    public void buildInternalVersionPage_versionDoesNotExist_throwsNotFound() throws Exception {
        cloudContractDescriptionBuilder.buildInternalVersionPage("3.0", null);
    }

    @Test
    public void buildPublicVersionPage_versionIdIsV10_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildPublicVersionPage("v1.0", null);
        verify(apiDocDao).getContent("/docs/cloud/v1.0/PublicVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildPublicVersionPage_versionIdIsV11_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildPublicVersionPage("v1.1", null);
        verify(apiDocDao).getContent("/docs/cloud/v1.1/PublicVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test
    public void buildPublicVersionPage_versionIdIsV20_callsBuild() throws Exception {
        cloudContractDescriptionBuilder.buildPublicVersionPage("v2.0", null);
        verify(apiDocDao).getContent("/docs/cloud/v2.0/PublicVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test (expected = NotFoundException.class)
    public void buildPublicVersionPage_versionDoesNotExist_throwsNotFound() throws Exception {
        cloudContractDescriptionBuilder.buildPublicVersionPage("3.0", null);
    }
}
