package com.rackspace.idm.interceptors;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.test.stub.StubLogger;

public class AuthorizationInterceptorTests {
    private AuthorizationInterceptor authorizationInterceptor;

    private ClientService clientService;

    private OAuthService oauthService;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    @Before
    public void setUp() {
        oauthService = EasyMock.createMock(OAuthService.class);
        clientService = EasyMock.createMock(ClientService.class);
        logger = new StubLogger();
        authorizationHelper = new IDMAuthorizationHelper(oauthService,
        null, null,
        clientService,logger);

        

        authorizationInterceptor = new AuthorizationInterceptor(clientService,
            oauthService, authorizationHelper, logger);
    }

    @Test
    public void shouldAcceptRequestsForLockingCustomers() {
        InterceptedCall call = InterceptedCall.SET_LOCK_STATUS;
        boolean result = authorizationInterceptor.accept(call
            .getControllerClass(), call.getInterceptedMethod());
        Assert.assertTrue(result);
    }

    @Test
    public void shouldAcceptRequestsForInitialUserCreation() {
        InterceptedCall call = InterceptedCall.ADD_FIRST_USER;
        boolean result = authorizationInterceptor.accept(call
            .getControllerClass(), call.getInterceptedMethod());
        Assert.assertTrue(result);
    }

    @Test
    public void shouldIgnoreAnyOtherRequest() {
        InterceptedCall call = InterceptedCall.SET_USER_PASSWORD;
        boolean result = authorizationInterceptor.accept(call
            .getControllerClass(), call.getInterceptedMethod());
        Assert.assertFalse(result);
    }

    @Test
    public void shouldAddFirstUserWhenAuthorized() throws URISyntaxException {
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("Sign-up");

        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("addFirstUser",
            "IDM", "POST /users", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("Sign-up")).andReturn(client);

        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.post("/users").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");

        ServerResponse response = authorizationInterceptor.preProcess(request,
            null);
        Assert.assertNull(response);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAddFirstUserWhenNotAuthorized()
        throws URISyntaxException {
        
        AccessToken token = new AccessToken("XXX", new DateTime(), "owner","requestor", IDM_SCOPE.FULL, false);
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("Sign-up");
        EasyMock.expect(oauthService.getTokenFromAuthHeader(EasyMock.anyObject(String.class))).andReturn(token);
        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("addFirstUser",
            "IDM", "POST /customers/RCN-000-000-000/users/", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("Sign-up")).andReturn(client);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.get(
            "/customers/RCN-000-000-000/users").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        authorizationInterceptor.preProcess(request, null);
    }

    @Test
    public void shouldSetCustomerLockWhenAuthorized() throws URISyntaxException {
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("Billing");
        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("setLockStatus",
            "IDM", "PUT /customers/.+/actions/lock", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("Billing")).andReturn(client);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.put(
            "/customers/RCN-000-000-000/actions/lock").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        ServerResponse response = authorizationInterceptor.preProcess(request,
            null);
        Assert.assertNull(response);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenNotAuthorized()
        throws URISyntaxException {
        
        AccessToken token = new AccessToken("XXX", new DateTime(), "owner","requestor", IDM_SCOPE.FULL, false);
        
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("Billing");
        EasyMock.expect(oauthService.getTokenFromAuthHeader(EasyMock.anyObject(String.class))).andReturn(token);
        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("setLockStatus",
            "IDM", "POST /customers/", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("Billing")).andReturn(client);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.get("/customers").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        authorizationInterceptor.preProcess(request, null);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenClientDoesNotExist()
        throws URISyntaxException {
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("Billing");
        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("setLockStatus",
            "IDM", "POST /customers/", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("Billing")).andReturn(null);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.get("/customers").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        authorizationInterceptor.preProcess(request, null);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenClientIdIsNull()
        throws URISyntaxException {
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("null");
        EasyMock.replay(oauthService);

        Client client = new Client();
        List<Permission> permissions = new ArrayList<Permission>();

        Permission setLockStatusPermission = new Permission("setLockStatus",
            "IDM", "POST /customers/", "Rackspace");
        permissions.add(setLockStatusPermission);
        client.setPermissions(permissions);

        EasyMock.expect(clientService.getById("null")).andReturn(null);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.get("/customers").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        authorizationInterceptor.preProcess(request, null);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetCustomerLockWhenPermissionListIsIsNull()
        throws URISyntaxException {
        EasyMock.expect(
            oauthService.getClientIdFromAuthHeaderToken("OAuth goodtoken"))
            .andReturn("null");
        EasyMock.replay(oauthService);

        Client client = new Client();
        client.setPermissions(null);

        EasyMock.expect(clientService.getById("null")).andReturn(null);
        EasyMock.replay(clientService);

        MockHttpRequest request = MockHttpRequest.put("/customers").header(
            HttpHeaders.AUTHORIZATION, "OAuth goodtoken");
        authorizationInterceptor.preProcess(request, null);
    }
}
