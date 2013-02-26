package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 12/6/11
 * Time: 4:26 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultEndpointServiceTestOld {

    @InjectMocks
    DefaultEndpointService defaultEndpointService = new DefaultEndpointService();
    DefaultEndpointService spy;
    @Mock
    EndpointDao endpointDao;
    int baseUrlId = 1;
    private String service = "defaultApplicationService";

    @Before
    public void setUp() throws Exception {
        spy = spy(defaultEndpointService);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName(service);
        CloudBaseUrl cloudBaseUrl2 = new CloudBaseUrl();
        cloudBaseUrl.setServiceName(service);
        CloudBaseUrl cloudBaseUrl3 = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("defaultApplicationService");
        when(endpointDao.getBaseUrlById(baseUrlId)).thenReturn(cloudBaseUrl);
        List<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        cloudBaseUrls.add(cloudBaseUrl);
        cloudBaseUrls.add(cloudBaseUrl2);
        cloudBaseUrls.add(cloudBaseUrl3);
        when(endpointDao.getBaseUrlsByService(service)).thenReturn(cloudBaseUrls);
    }

    @Test
    public void getBaseUrlsByServiceName_callsEndpointDao() throws Exception {
        defaultEndpointService.getBaseUrlsByServiceName("cloudFiles");
        verify(endpointDao).getBaseUrlsByService("cloudFiles");
    }

    @Test
    public void getBaseUrlsByBaseUrlType_noBaseUrls_returnsEmptyList() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        List<CloudBaseUrl> baseUrlsByBaseUrlType = defaultEndpointService.getBaseUrlsByBaseUrlType("");
        assertThat("list size", baseUrlsByBaseUrlType.size(), equalTo(0));
    }

    @Test
    public void getBaseUrlsByBaseUrlType_nullBaseUrls_returnsEmptyList() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(null);
        List<CloudBaseUrl> baseUrlsByBaseUrlType = defaultEndpointService.getBaseUrlsByBaseUrlType("");
        assertThat("list size", baseUrlsByBaseUrlType.size(), equalTo(0));
    }

    @Test
    public void getBaseUrlsByBaseUrlType_baseUrlsTypeIsNull_skipAdd() throws Exception {
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("MOSSO");
        CloudBaseUrl cloudBaseUrl2 = new CloudBaseUrl();
        cloudBaseUrls.add(cloudBaseUrl);
        cloudBaseUrls.add(cloudBaseUrl2);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrls);
        List<CloudBaseUrl> baseUrlsByBaseUrlType = defaultEndpointService.getBaseUrlsByBaseUrlType("MOSSO");
        assertThat("list size", baseUrlsByBaseUrlType.size(), equalTo(1));
    }


    @Test
    public void getBaseUrlsByBaseUrlType_filtersByName() throws Exception {
        ArrayList<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlType("MOSSO");
        CloudBaseUrl cloudBaseUrl2 = new CloudBaseUrl();
        cloudBaseUrl2.setBaseUrlType("NAST");
        cloudBaseUrls.add(cloudBaseUrl);
        cloudBaseUrls.add(cloudBaseUrl2);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrls);
        List<CloudBaseUrl> baseUrlsByBaseUrlType = defaultEndpointService.getBaseUrlsByBaseUrlType("MOSSO");
        assertThat("list size", baseUrlsByBaseUrlType.size(), equalTo(1));
    }

    @Test
    public void updateBaseUrl_callsEndpointDao_updateCloudBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        defaultEndpointService.updateBaseUrl(cloudBaseUrl);
        verify(endpointDao).updateCloudBaseUrl(cloudBaseUrl);
    }

    @Test
    public void getGlobalBaseUrls_callsEndpointDao_getBaseUrls() throws Exception {
        defaultEndpointService.getGlobalBaseUrls();
        verify(endpointDao).getBaseUrls();
    }

    @Test
    public void getGlobalBaseUrls_baseUrlHasGlobal_addsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setGlobal(true);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getGlobalBaseUrls();
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getGlobalBaseUrls_baseUrlDoesNotHaveGlobal_doesNotAddBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setGlobal(false);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getGlobalBaseUrls();
        assertThat("cloud base url", result.size(), equalTo(0));
    }

    @Test
    public void getDefaultBaseUrls_callsEndpointDao_getBaseUrls() throws Exception {
        defaultEndpointService.getDefaultBaseUrls();
        verify(endpointDao).getBaseUrls();
    }

    @Test
    public void getDefaultBaseUrls_baseUrlGetDefIsNull_returnsEmptyList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(null);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getDefaultBaseUrls();
        assertThat("list", result.size(), equalTo(0));
    }

    @Test
    public void getDefaultBaseUrls_baseUrlGetDefIsFalse_returnsEmptyList() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(false);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getDefaultBaseUrls();
        assertThat("list", result.size(), equalTo(0));
    }

    @Test
    public void getDefaultBaseUrls_baseUrlGetDefIsTrue_addsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(true);
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getDefaultBaseUrls();
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void checkAndGetEndpointTemplate_intParameterAndBaseURLExists_returnsCloudBaseURl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        doReturn(cloudBaseUrl).when(spy).getBaseUrlById(123);
        assertThat("cloud base url", spy.checkAndGetEndpointTemplate(123), equalTo(cloudBaseUrl));
    }

    @Test
    public void checkAndGetEndpointTemplate_intParameterAndBaseURLDoesNotExist_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getBaseUrlById(123);
            spy.checkAndGetEndpointTemplate(123);
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message",ex.getMessage(),equalTo("EndpointTemplate 123 not found"));
        }
    }

    @Test
    public void checkAndGetEndpointTemplate_stringParameterAndIntegerParsingSucceeds_returnsCloudBaseURl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate(123);
        assertThat("cloud base url",spy.checkAndGetEndpointTemplate("123"),equalTo(cloudBaseUrl));
    }

    @Test
    public void checkAndGetEndpointTemplate_stringParameterAndIntegerParsingFails_throwsNotFoundException() throws Exception {
        try{
            spy.checkAndGetEndpointTemplate("hi");
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message",ex.getMessage(),equalTo("EndpointTemplate hi not found"));
        }
    }

    @Test
    public void setBaseUrlEnabled_callsEndpointDao_setBaseUrlEnabled() throws Exception {
        defaultEndpointService.setBaseUrlEnabled(1, true);
        verify(endpointDao).setBaseUrlEnabled(1, true);
    }

    @Test
    public void getBaseUrlsByServiceId_callsEndpointDao_getBaseUrls() throws Exception {
        defaultEndpointService.getBaseUrlsByServiceType("defaultApplicationService");
        verify(endpointDao).getBaseUrls();
    }

    @Test
    public void getBaseUrlsByServiceId_baseUrlOpenStackTypeEqualsServiceType_addsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("defaultApplicationService");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getBaseUrlsByServiceType("defaultApplicationService");
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrlsByServiceId_baseUrlOpenStackTypeNotEqualServiceType_doesNotAddBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("differentServiceType");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getBaseUrlsByServiceType("defaultApplicationService");
        assertThat("list", result.size(), equalTo(0));
    }
}
