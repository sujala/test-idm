package com.rackspace.idm.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
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
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.validation.InputValidator;

public class ClientsResourceTests {
    
    ClientsResource resource;
    CustomerService mockCustomerService;
    ClientService mockClientService;
    ClientConverter clientConverter;
    PermissionConverter permissionConverter;
    AuthorizationService mockAuthorizationService;
    UriInfo mockUriInfo;
    Request mockRequest;
    
    String authHeader = "OAuth XXXXX";

    String clientId = "ABCDEFG";
    String clientName = "Test Client";
    String customerId = "RCN-000-000-000";
    String permissionId = "GetClient";
    String permissionValue = "GET /customers/.+/clients/.+";

    List<Permission> testPermissions;
    Permission testPermission;
    List<Client> testClients;
    Client testClient;
    Customer testCustomer;

    String verb = "verb";
    String uri = "uri";
    
    @Before
    public void setUp() {
        mockClientService = EasyMock.createMock(ClientService.class);
        mockCustomerService = EasyMock.createMock(CustomerService.class);
        mockAuthorizationService = EasyMock
            .createMock(AuthorizationService.class);
        Validator validator = Validation.buildDefaultValidatorFactory()
        .getValidator();

        resource = new ClientsResource(mockCustomerService, new InputValidator(validator), new ClientConverter(
            new PermissionConverter()),mockClientService, null, mockAuthorizationService,
            new LoggerFactoryWrapper());
        mockUriInfo = EasyMock.createMock(UriInfo.class);
        mockRequest = EasyMock.createMock(Request.class);

        setUpTestClient();
    }
    
    @Test
    public void shouldGetClientsWithRackerToken() {
        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(testCustomer);
        EasyMock.replay(mockCustomerService);
        
        EasyMock.expect(mockClientService.getByCustomerId(customerId)).andReturn(testClients);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClients(mockRequest, mockUriInfo, authHeader, customerId);

        com.rackspace.idm.jaxb.Clients returnClients = (com.rackspace.idm.jaxb.Clients)response.getEntity();
        Assert.assertTrue(returnClients.getClients().size() == 1);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
    }
    
    @Test
    public void shouldGetClientsWithClientToken() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        
        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(testCustomer);
        EasyMock.replay(mockCustomerService);
        
        EasyMock.expect(mockClientService.getByCustomerId(customerId)).andReturn(testClients);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClients(mockRequest, mockUriInfo, authHeader, customerId);

        com.rackspace.idm.jaxb.Clients returnClients = (com.rackspace.idm.jaxb.Clients)response.getEntity();
        Assert.assertTrue(returnClients.getClients().size() == 1);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void shouldGetClientsWithAdminToken() {
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
        
        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(testCustomer);
        EasyMock.replay(mockCustomerService);
        
        EasyMock.expect(mockClientService.getByCustomerId(customerId)).andReturn(testClients);
        EasyMock.replay(mockClientService);
        
        Response response = resource.getClients(mockRequest, mockUriInfo, authHeader, customerId);

        com.rackspace.idm.jaxb.Clients returnClients = (com.rackspace.idm.jaxb.Clients)response.getEntity();
        Assert.assertTrue(returnClients.getClients().size() == 1);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotGetClientsForNonExistentCustomer() {
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
        
        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(null);
        EasyMock.replay(mockCustomerService);
        
        Response response = resource.getClients(mockRequest, mockUriInfo, authHeader, customerId);
    }
    
    @Test(expected = ForbiddenException.class)
    public void shouldNotGetClientsIfNotAuthorized() {
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
        
        Response response = resource.getClients(mockRequest, mockUriInfo, authHeader, customerId);
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
        testClient.setName(clientName);
        testClient.setClientId(clientId);
        testClient.setCustomerId(customerId);
        testClient.setPermissions(testPermissions);
        
        testClients = new ArrayList<Client>();
        testClients.add(testClient);
        
        testCustomer = new Customer();
        testCustomer.setCustomerId(customerId);
        testCustomer.setDefaults();
    }
    
}
