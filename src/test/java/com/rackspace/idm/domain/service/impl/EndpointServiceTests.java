package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BaseUrlConflictException;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EndpointServiceTests {

    EndpointService endpointService;
    EndpointDao mockEndpointDao;
    
    String adminUrl = "http://admin.com";
    int baseUrlId = 1;
    String baseUrlType = "CLOUD";
    boolean def = false;
    String internalUrl = "http://internal.com";
    String publicUrl = "http://public.com";
    String region = "DFW";
    String service = "CloudFiles";
    
    String username = "username";
    
    int mossoId = 1;
    String nastId = "nastId";
    boolean v1preferred = false;
    
    CloudBaseUrl baseUrl;
    List<CloudBaseUrl> baseUrls;
    CloudEndpoint endpoint;
    List<CloudEndpoint> endpoints;

    @Before
    public void setUp() throws Exception {

        mockEndpointDao = EasyMock.createMock(EndpointDao.class);

        endpointService = new DefaultEndpointService();
        endpointService.setEndpointDao(mockEndpointDao);
        
        baseUrl = new CloudBaseUrl();
        baseUrl.setAdminUrl(adminUrl);
        baseUrl.setBaseUrlId(baseUrlId);
        baseUrl.setBaseUrlType(baseUrlType);
        baseUrl.setDef(def);
        baseUrl.setInternalUrl(internalUrl);
        baseUrl.setPublicUrl(publicUrl);
        baseUrl.setRegion(region);
        baseUrl.setServiceName(service);
        
        baseUrls = new ArrayList<CloudBaseUrl>();
        baseUrls.add(baseUrl);
        
        endpoint = new CloudEndpoint();
        endpoint.setBaseUrl(baseUrl);
        endpoint.setMossoId(mossoId);
        endpoint.setNastId(nastId);
        endpoint.setUsername(username);
        endpoint.setV1preferred(v1preferred);
        
        endpoints = new ArrayList<CloudEndpoint>();
        endpoints.add(endpoint);
        
    }
    
    @Test
    public void shouldAddBaseUrl() {
        EasyMock.expect(mockEndpointDao.getBaseUrlById(baseUrlId)).andReturn(null);
        mockEndpointDao.addBaseUrl(baseUrl);
        EasyMock.replay(mockEndpointDao);
        
        endpointService.addBaseUrl(baseUrl);
        
        EasyMock.verify(mockEndpointDao);
    }
    
    @Test(expected = BaseUrlConflictException.class)
    public void shouldNotAddBaseUrlForIdThatAlreadyExists() {
        EasyMock.expect(mockEndpointDao.getBaseUrlById(baseUrlId)).andReturn(baseUrl);
        EasyMock.replay(mockEndpointDao);
        
        endpointService.addBaseUrl(baseUrl);
    }
    
    @Test
    public void shouldDeleteBaseUrl() {
        mockEndpointDao.deleteBaseUrl(baseUrlId);
        EasyMock.replay(mockEndpointDao);
        
        endpointService.deleteBaseUrl(baseUrlId);
        
        EasyMock.verify(mockEndpointDao);
    }
    
    @Test
    public void shouldGetBaseUrls() {
        EasyMock.expect(mockEndpointDao.getBaseUrls()).andReturn(baseUrls);
        EasyMock.replay(mockEndpointDao);
        
        List<CloudBaseUrl> urls = endpointService.getBaseUrls();
        
        Assert.assertTrue(urls.size() == 1);
        EasyMock.verify(mockEndpointDao);
    }
    
    @Test
    public void shouldGetBaseUrl() {
        EasyMock.expect(mockEndpointDao.getBaseUrlById(baseUrlId)).andReturn(baseUrl);
        EasyMock.replay(mockEndpointDao);
        
        CloudBaseUrl url = endpointService.getBaseUrlById(baseUrlId);
        
        Assert.assertTrue(url.getAdminUrl().equals(adminUrl));
        EasyMock.verify(mockEndpointDao);
    }
    
}
