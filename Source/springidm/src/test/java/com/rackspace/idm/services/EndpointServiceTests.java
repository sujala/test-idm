package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.EndpointDao;
import com.rackspace.idm.entities.CloudBaseUrl;
import com.rackspace.idm.entities.CloudEndpoint;
import com.rackspace.idm.exceptions.BaseUrlConflictException;
import com.rackspace.idm.test.stub.StubLogger;

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

        endpointService = new DefaultEndpointService(mockEndpointDao, new StubLogger());
        
        baseUrl = new CloudBaseUrl();
        baseUrl.setAdminUrl(adminUrl);
        baseUrl.setBaseUrlId(baseUrlId);
        baseUrl.setBaseUrlType(baseUrlType);
        baseUrl.setDef(def);
        baseUrl.setInternalUrl(internalUrl);
        baseUrl.setPublicUrl(publicUrl);
        baseUrl.setRegion(region);
        baseUrl.setService(service);
        
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
    public void shouldAddBaseUrlToUser() {
        mockEndpointDao.addBaseUrlToUser(baseUrlId, def, username);
        EasyMock.replay(mockEndpointDao);
        
        endpointService.addBaseUrlToUser(baseUrlId, def, username);
        
        EasyMock.verify(mockEndpointDao);
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
    
    @Test
    public void shouldGetUserEndpoint() {
        EasyMock.expect(mockEndpointDao.getEndpointsForUser(username)).andReturn(endpoints);
        EasyMock.replay(mockEndpointDao);
        
        CloudEndpoint point = endpointService.getEndpointForUser(username, baseUrlId);
        
        Assert.assertTrue(point.getBaseUrl().equals(baseUrl));
        EasyMock.verify(mockEndpointDao);
    }
    
    @Test
    public void shouldGetUserEndpoints() {
        EasyMock.expect(mockEndpointDao.getEndpointsForUser(username)).andReturn(endpoints);
        EasyMock.replay(mockEndpointDao);
        
        List<CloudEndpoint> points = endpointService.getEndpointsForUser(username);
        
        Assert.assertTrue(points.size() == 1);
        EasyMock.verify(mockEndpointDao);
    }
    
    @Test
    public void shouldRemoveBaseUrlFromUser() {
        mockEndpointDao.removeBaseUrlFromUser(baseUrlId, username);
        EasyMock.replay(mockEndpointDao);
        
        endpointService.removeBaseUrlFromUser(baseUrlId, username);
        
        EasyMock.verify(mockEndpointDao);
    }
}
