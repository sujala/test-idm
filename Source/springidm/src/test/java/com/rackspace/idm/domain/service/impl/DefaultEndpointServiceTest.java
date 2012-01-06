package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
        verify(endpointDao).getBaseUrlsByService(service);
    }

    @Test
    public void removeBaseUrlFromUser_getsBaseUrlById() throws Exception {
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
        verify(endpointDao).getBaseUrlById(baseUrlId);
    }

    @Test(expected = BadRequestException.class)
    public void removeBaseUrlFromUser_() throws Exception {
        when(endpointDao.getBaseUrls()).thenReturn(new ArrayList<CloudBaseUrl>());
        when(endpointDao.getBaseUrlsByService(service)).thenReturn(new ArrayList<CloudBaseUrl>());
        defaultEndpointService.removeBaseUrlFromUser(baseUrlId, user);
    }
}
