package com.rackspace.idm.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.authorizationService.AuthorizationRequest;
import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.Entity;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.authorizationService.SunAuthorizationService;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.PermissionSet;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientConflictException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

public class ClientControllerTests {
    ClientController controller;
    UserService mockUserService;
    OAuthService mockOAuthService;
    AuthorizationService mockAuthorizationService;
    RoleService mockRoleService;
    ClientService mockClientService;
    PermissionConverter permissionConverter = new PermissionConverter();
    ClientConverter clientConverter = new ClientConverter(permissionConverter);

    String password = "secret";
    User testUser;
    String username = "testuser";

    Client testClient;
    String authHeader = "Token token=asdf1234";
    com.rackspace.idm.jaxb.Client testClientJaxb;
    com.rackspace.idm.jaxb.Client testClientJaxb2;

    String clientId = "Id";
    ClientSecret clientSecret = ClientSecret.newInstance("secret");
    String customerId = "CustomerID";
    String badCustomerId = "BadCustomerId";
    String iname = "iname";
    String inum = "inum";
    String clientName = "ClientName";
    String owner = "owner";
    String seeAlso = "seeAlso";
    ClientStatus status = ClientStatus.ACTIVE;

    Permission permission;
    Permission permission1;
    Permission permission2;
    List<Permission> permissions = new ArrayList<Permission>();

    String permissionId = "permisisonId";
    String permissionValue = "permissionValue";

    com.rackspace.idm.jaxb.Permission permissionJaxb;

    PermissionSet permset;

    @Before
    public void setUp() {

        mockClientService = EasyMock.createMock(ClientService.class);
        Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockUserService = EasyMock.createMock(UserService.class);
        mockAuthorizationService = EasyMock
            .createMock(SunAuthorizationService.class);
        mockRoleService = EasyMock.createMock(RoleService.class);

        controller = new ClientController(mockClientService,
            new IDMAuthorizationHelper(mockOAuthService,
                mockAuthorizationService, mockRoleService, mockClientService,
                LoggerFactory.getLogger(IDMAuthorizationHelper.class)),
            new InputValidator(validator), clientConverter,
            permissionConverter, new LoggerFactoryWrapper());

        permission1 = new Permission("Permission1", "IDM", "Permission1",
            "Rackspace");
        permission2 = new Permission("Permission2", "IDM", "Permission2",
            "Rackspace");

        permissions.add(permission1);
        permissions.add(permission2);

        testClient = new Client();

        testClient.setClientId(clientId);
        testClient.setClientSecretObj(clientSecret);
        testClient.setCustomerId(customerId);
        testClient.setIname(iname);
        testClient.setInum(inum);
        testClient.setName(clientName);
        testClient.setOwner(owner);
        testClient.setSeeAlso(seeAlso);
        testClient.setStatus(status);
        testClient.setPermissions(permissions);

        testClientJaxb = clientConverter
            .toClientJaxbWithPermissionsAndCredentials(testClient);

        testClientJaxb2 = clientConverter
            .toClientJaxbWithPermissionsAndCredentials(testClient);
        testClientJaxb2
            .setPermissions(new com.rackspace.idm.jaxb.PermissionList());
        com.rackspace.idm.jaxb.ClientCredentials creds = new com.rackspace.idm.jaxb.ClientCredentials();
        creds.setClientSecret("");
        testClientJaxb2.setCredentials(creds);
        clientConverter.toClientDO(testClientJaxb2);

        testClientJaxb2.setPermissions(null);
        testClientJaxb2.setCredentials(null);
        clientConverter.toClientDO(testClientJaxb2);

        permissionJaxb = new com.rackspace.idm.jaxb.Permission();
        permissionJaxb.setClientId(clientId);
        permissionJaxb.setCustomerId(customerId);
        permissionJaxb.setPermissionId(permissionId);
        permissionJaxb.setValue(permissionValue);

        permission = new com.rackspace.idm.entities.Permission();
        permission.setClientId(clientId);
        permission.setCustomerId(customerId);
        permission.setPermissionId(permissionId);
        permission.setValue(permissionValue);

        permissions.add(permission);

        permset = new PermissionSet();
        permset.setDefineds(permissions);

    }

    @Test
    public void shouldAddPermission() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        mockClientService.addDefinedPermission(EasyMock
            .anyObject(com.rackspace.idm.entities.Permission.class));

        controller.addClientPermission(response, authHeader, customerId,
            clientId, permissionJaxb);
        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotAddPermissionForMissingValue() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        mockClientService.addDefinedPermission(EasyMock
            .anyObject(com.rackspace.idm.entities.Permission.class));

        permissionJaxb.setValue(null);

        controller.addClientPermission(response, authHeader, customerId,
            clientId, permissionJaxb);
        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test
    public void shouldGetPermission() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockClientService.getDefinedPermissionByClientIdAndPermissionId(
                clientId, permissionId)).andReturn(permission);
        EasyMock.replay(mockClientService);

        controller.getClientPermission(response, authHeader, customerId,
            clientId, permissionId);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldGetPermissions() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockClientService.getDefinedPermissionsByClientId(clientId))
            .andReturn(permissions);
        EasyMock.replay(mockClientService);

        com.rackspace.idm.jaxb.Permissions res = controller
            .getClientPermissions(response, authHeader, badCustomerId, clientId);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertTrue(res.getDefined().getPermissions().size() >= 1);
    }

    @Test
    public void shouldUpdatePermission() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        mockClientService.updateDefinedPermission(permission);
        EasyMock.expect(
            mockClientService.getDefinedPermissionByClientIdAndPermissionId(
                clientId, permissionId)).andReturn(permission);
        EasyMock.replay(mockClientService);

        com.rackspace.idm.jaxb.Permission res = controller
            .updateClientPermission(response, authHeader, customerId, clientId,
                permissionId, permissionJaxb);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertTrue(res.getValue().equals(permission.getValue()));
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotUpdatePermissionForMissingValue() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        permission.setValue(null);
        permissionJaxb.setValue(null);
        mockClientService.updateDefinedPermission(permission);
        EasyMock.expect(
            mockClientService.getDefinedPermissionByClientIdAndPermissionId(
                clientId, permissionId)).andReturn(permission);
        EasyMock.replay(mockClientService);

        com.rackspace.idm.jaxb.Permission res = controller
            .updateClientPermission(response, authHeader, customerId, clientId,
                permissionId, permissionJaxb);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertTrue(res.getValue().equals(permission.getValue()));
    }

    @Test
    public void shouldDeletePermission() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        permission.setValue(null);

        mockClientService.deleteDefinedPermission(permission);
        EasyMock.expect(
            mockClientService.getDefinedPermissionByClientIdAndPermissionId(
                clientId, permissionId)).andReturn(permission);
        EasyMock.replay(mockClientService);

        controller.deleteClientPermission(response, authHeader, customerId,
            clientId, permissionId);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldAddClient() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setupAdminAuthorizationChecks();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        controller.addClient(response, authHeader, customerId, testClientJaxb);
        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAddClientWhenNotAuthorized() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        try {
            setupAdminAuthorizationChecksForNotAuthorized();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        controller.addClient(response, authHeader, customerId, testClientJaxb);

    }

    @Test(expected = BadRequestException.class)
    public void shouldNotAddClientIfClientNameBlank() {
        testClientJaxb.setName("");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.addClient(response, authHeader, customerId, testClientJaxb);

    }

    @Test(expected = BadRequestException.class)
    public void shouldNotAddClientIfCustomerIdBlank() {
        testClientJaxb.setCustomerId("");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.addClient(response, authHeader, "", testClientJaxb);

    }

    @Test(expected = CustomerConflictException.class)
    public void shouldNotAddClientIfCustomerIdDoesntMatch() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.addClient(response, authHeader, badCustomerId,
            testClientJaxb);

    }

    @Test(expected = ClientConflictException.class)
    public void shouldNotAddClientIfNameExists() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setupAdminAuthorizationChecks();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mockClientService.add(EasyMock.anyObject(testClient.getClass()));
        EasyMock.expectLastCall().andThrow(
            new DuplicateException(String.format(
                "Clientname %s already exists", testClient.getName())));
        EasyMock.replay(mockClientService);

        controller.addClient(response, authHeader, customerId, testClientJaxb);
    }

    @Test
    public void shouldGetClient() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        controller.getClient(response, authHeader, customerId, clientId);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockClientService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetClientWithWrongCustomerId() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        controller.getClient(response, authHeader, badCustomerId, clientId);
    }

    @Test(expected = NotFoundException.class)
    public void shouldReturnNotFoundForNonExistentClient() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(mockClientService.getById(clientId)).andReturn(null);
        EasyMock.replay(mockClientService);

        controller.getClient(response, authHeader, customerId, clientId);
    }

    private void setupAdminAuthorizationChecks() throws Exception {
        String role = "admin";

        String oldpassword = password;
        String newpassword = "newpass";

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("testUser");
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);

        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser("testUser")).andReturn(
            roleList);
    }

    private void setupAdminAuthorizationChecksForNotAuthorized()
        throws Exception {
        String role = "admin";

        String oldpassword = password;
        String newpassword = "newpass";

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("testUser").times(2);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);

        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser("testUser")).andReturn(
            roleList);
    }

    private AccessToken createTestToken(boolean isTrusted) {
        String tokenString = "asdf1234";
        DateTime expiration = new DateTime().plusHours(1);
        AccessToken testToken = new AccessToken(tokenString, expiration, owner,
            username, IDM_SCOPE.FULL, isTrusted);
        return testToken;
    }
}
