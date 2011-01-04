package com.rackspace.idm.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;

public class ClientResourceTests {

    ClientResource resource;
    ClientService mockClientService;
    ClientConverter clientConverter;
    PermissionConverter permissionConverter;
    AuthorizationService mockAuthorizationService;
    UriInfo mockUriInfo;
    Request mockRequest;
    
    String authHeader = "OAuth XXXXX";

    String clientId = "ABCDEFG";
    String customerId = "RCN-000-000-000";
    String permissionId = "GetClient";
    String permissionValue = "GET /customers/.+/clients/.+";

    List<Permission> testPermissions;
    Permission testPermission;
    Client testClient;

    String verb = "GET";
    String uri = "/customers/" + customerId + "/clients/" + clientId;

    @Before
    public void setUp() {
        mockClientService = EasyMock.createMock(ClientService.class);
        mockAuthorizationService = EasyMock
            .createMock(AuthorizationService.class);

        resource = new ClientResource(mockClientService, new ClientConverter(
            new PermissionConverter()), null, mockAuthorizationService,
            new LoggerFactoryWrapper());
        mockUriInfo = EasyMock.createMock(UriInfo.class);
        mockRequest = EasyMock.createMock(Request.class);

        setUpTestClient();
    }
    
    @Test
    public void shouldGetClientWithRackerToken() {

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(testClient);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClient(mockRequest, mockUriInfo, authHeader, customerId, clientId);

        com.rackspace.idm.jaxb.Client returnClient = (com.rackspace.idm.jaxb.Client)response.getEntity();
        Assert.assertTrue(returnClient.getClientId().equals(clientId));
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
    }
    
    @Test
    public void shouldGetClientWithClientToken() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(testClient);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClient(mockRequest, mockUriInfo, authHeader, customerId, clientId);

        com.rackspace.idm.jaxb.Client returnClient = (com.rackspace.idm.jaxb.Client)response.getEntity();
        Assert.assertTrue(returnClient.getClientId().equals(clientId));
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void shouldGetClientWithAdminToken() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(testClient);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClient(mockRequest, mockUriInfo, authHeader, customerId, clientId);

        com.rackspace.idm.jaxb.Client returnClient = (com.rackspace.idm.jaxb.Client)response.getEntity();
        Assert.assertTrue(returnClient.getClientId().equals(clientId));
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }

    @Test(expected = NotFoundException.class)
    public void shouldReturnNotFoundForNonExistentClient() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(null);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClient(mockRequest, mockUriInfo, authHeader, customerId, clientId);

    }
    
    @Test(expected = ForbiddenException.class)
    public void shouldNotGetClientIfUnauthorized() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(false);
        EasyMock.replay(mockAuthorizationService);
        
        Response response = resource.getClient(mockRequest, mockUriInfo, authHeader, customerId, clientId);

    }

    private void setUpTestClient() {
        testPermission = new Permission();
        testPermission.setClientId(clientId);
        testPermission.setCustomerId(customerId);
        testPermission.setPermissionId(permissionId);
        testPermission.setValue(permissionValue);

        testPermissions = new ArrayList<Permission>();
        testPermissions.add(testPermission);

        testClient = new Client();
        testClient.setClientId(clientId);
        testClient.setCustomerId(customerId);
        testClient.setPermissions(testPermissions);
    }
}
