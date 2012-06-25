package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

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
public class DefaultEndpointServiceTest {

    DefaultEndpointService defaultEndpointService;
    EndpointDao endpointDao;
    int baseUrlId = 1;
    private String service = "service";
    private String user = "user";

    @Before
    public void setUp() throws Exception {
        endpointDao = mock(EndpointDao.class);
        defaultEndpointService = new DefaultEndpointService(endpointDao);
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setServiceName(service);
        CloudBaseUrl cloudBaseUrl2 = new CloudBaseUrl();
        cloudBaseUrl.setServiceName(service);
        CloudBaseUrl cloudBaseUrl3 = new CloudBaseUrl();
        cloudBaseUrl.setServiceName("service");
        when(endpointDao.getBaseUrlById(baseUrlId)).thenReturn(cloudBaseUrl);
        List<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        cloudBaseUrls.add(cloudBaseUrl);
        cloudBaseUrls.add(cloudBaseUrl2);
        cloudBaseUrls.add(cloudBaseUrl3);
        when(endpointDao.getBaseUrlsByService(service)).thenReturn(cloudBaseUrls);
    }

    @Test
    public void removeBaseUrlFromUser_getsAllBaseUrlForService() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(false);
        cloudBaseUrl.setBaseUrlId(baseUrlId);
        cloudBaseUrl.setServiceName(service);
        List<CloudBaseUrl> list = new ArrayList<CloudBaseUrl>();
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());

        List<CloudEndpoint> cloudEndpoints = new ArrayList<CloudEndpoint>();
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        cloudEndpoints.add(cloudEndpoint);

        when(endpointDao.getBaseUrlsByService(anyString())).thenReturn(list);
        when(endpointDao.getBaseUrlById(baseUrlId)).thenReturn(cloudBaseUrl);
        when(endpointDao.getEndpointsForUser(user)).thenReturn(cloudEndpoints);
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
        verify(endpointDao).getBaseUrlsByService(service);
    }

    @Test
    public void removeBaseUrlFromUser_getsBaseUrlById() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(baseUrlId);
        cloudBaseUrl.setServiceName(service);
        List<CloudBaseUrl> list = new ArrayList<CloudBaseUrl>();
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());

        List<CloudEndpoint> cloudEndpoints = new ArrayList<CloudEndpoint>();
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        cloudEndpoints.add(cloudEndpoint);

        when(endpointDao.getBaseUrlsByService(anyString())).thenReturn(list);
        when(endpointDao.getBaseUrlById(baseUrlId)).thenReturn(cloudBaseUrl);
        when(endpointDao.getEndpointsForUser(user)).thenReturn(cloudEndpoints);
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
        verify(endpointDao).getBaseUrlById(baseUrlId);
    }

    @Test
    public void removeBaseUrlFromUser_callsGetEndpointsForUser() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(baseUrlId);
        cloudBaseUrl.setServiceName(service);
        List<CloudBaseUrl> list = new ArrayList<CloudBaseUrl>();
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());

        List<CloudEndpoint> cloudEndpoints = new ArrayList<CloudEndpoint>();
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        cloudEndpoints.add(cloudEndpoint);

        when(endpointDao.getBaseUrlsByService(anyString())).thenReturn(list);
        when(endpointDao.getBaseUrlById(baseUrlId)).thenReturn(cloudBaseUrl);
        when(endpointDao.getEndpointsForUser(user)).thenReturn(cloudEndpoints);
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
        verify(endpointDao).getEndpointsForUser(user);
    }

    @Test
    public void removeBaseUrlFromUser_baseURLNotForUser_throwsNotFoundException() throws Exception {
        try{
            CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
            cloudBaseUrl.setDef(false);
            List<CloudBaseUrl> list = new ArrayList<CloudBaseUrl>();
            list.add(new CloudBaseUrl());
            list.add(new CloudBaseUrl());
            list.add(new CloudBaseUrl());
            when(endpointDao.getBaseUrlsByService(anyString())).thenReturn(list);
            when(endpointDao.getBaseUrlById(3)).thenReturn(cloudBaseUrl);
            when(endpointDao.getEndpointsForUser(user)).thenReturn(new ArrayList<CloudEndpoint>());
            defaultEndpointService.removeBaseUrlFromUser(3, user);
            assertTrue("expected exception",false);
        } catch (NotFoundException nf){
            assertThat("exception message",nf.getMessage(),equalTo("Attempting to delete nonexisting baseUrl: 3"));
        }


    }

    @Test
    public void removeBaseUrlFromUser_baseURLForUser_callsEndpointDaoMethod() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setDef(false);
        cloudBaseUrl.setBaseUrlId(3);
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        List<CloudEndpoint> endpoints = new ArrayList<CloudEndpoint>();
        endpoints.add(cloudEndpoint);
        List<CloudBaseUrl> list = new ArrayList<CloudBaseUrl>();
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());
        list.add(new CloudBaseUrl());
        when(endpointDao.getBaseUrlsByService(anyString())).thenReturn(list);
        when(endpointDao.getBaseUrlById(3)).thenReturn(cloudBaseUrl);
        when(endpointDao.getEndpointsForUser(user)).thenReturn(endpoints);
        defaultEndpointService.removeBaseUrlFromUser(3, user);
        verify(endpointDao).removeBaseUrlFromUser(3,user);
    }

    @Test(expected = BadRequestException.class)
    public void removeBaseUrlFromUser_() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        when(endpointDao.getBaseUrlsByService(service)).thenReturn(new ArrayList<CloudBaseUrl>());
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
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
    public void setBaseUrlEnabled_callsEndpointDao_setBaseUrlEnabled() throws Exception {
        defaultEndpointService.setBaseUrlEnabled(1, true);
        verify(endpointDao).setBaseUrlEnabled(1, true);
    }

    @Test
    public void getBaseUrlsByServiceId_callsEndpointDao_getBaseUrls() throws Exception {
        defaultEndpointService.getBaseUrlsByServiceId("service");
        verify(endpointDao).getBaseUrls();
    }

    @Test
    public void getBaseUrlsByServiceId_baseUrlOpenStackTypeEqualsServiceType_addsBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("service");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getBaseUrlsByServiceId("service");
        assertThat("cloud base url", result.get(0), equalTo(cloudBaseUrl));
    }

    @Test
    public void getBaseUrlsByServiceId_baseUrlOpenStackTypeNotEqualServiceType_doesNotAddBaseUrl() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setOpenstackType("differentServiceType");
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        when(endpointDao.getBaseUrls()).thenReturn(cloudBaseUrlList);
        List<CloudBaseUrl> result = defaultEndpointService.getBaseUrlsByServiceId("service");
        assertThat("list", result.size(), equalTo(0));
    }
}
