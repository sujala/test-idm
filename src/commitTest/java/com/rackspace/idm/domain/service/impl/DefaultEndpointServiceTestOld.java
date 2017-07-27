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

    @Test
    public void getBaseUrlsByServiceName_callsEndpointDao() throws Exception {
        defaultEndpointService.getBaseUrlsByServiceName("cloudFiles");
        verify(endpointDao).getBaseUrlsByService("cloudFiles");
    }

    @Test
    public void updateBaseUrl_callsEndpointDao_updateCloudBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        defaultEndpointService.updateBaseUrl(cloudBaseUrl);
        verify(endpointDao).updateCloudBaseUrl(cloudBaseUrl);
    }

    @Test
    public void getGlobalBaseUrls_callsEndpointDao_getBaseUrls() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
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
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
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
    public void getBaseUrlsByServiceId_callsEndpointDao_getBaseUrls() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
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
