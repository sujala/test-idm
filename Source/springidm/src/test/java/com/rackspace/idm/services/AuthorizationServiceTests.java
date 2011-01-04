package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.BaseClient;
import com.rackspace.idm.entities.BaseUser;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.test.stub.StubLogger;

public class AuthorizationServiceTests {
    OAuthService mockOauthService;
    ClientService mockClientService;
    AuthorizationService service;
    
    String authHeader = "OAuth XXXX";
    
    String tokenString = "XXXX";
    DateTime tokenExpiration;
    
    String customerId = "RACKSPACE";
    String otherCustomerId = "RCN-000-000-000";
    String clientId = "ABCDEF";
    
    String username = "username";
    
    String verb = "GET";
    String uri = "/resource";
    
    String permissionId = "Permission";
    String permissionValue = verb + " " + uri;
    
    BaseClient authorizedClient;
    BaseClient notAuthorizedClient;
    
    BaseUser authorizedUser;
    BaseUser otherCompanyUser;
    
    BaseUser authorizedAdmin;
    BaseUser otherCompanyAdmin;
    
    Permission perm;
    List<Permission> permissions;
    Role admin;
    List<Role> roles;
    
    String adminRoleName = GlobalConstants.IDM_ADMIN_ROLE_NAME;
    
    AccessToken trustedToken;
    AccessToken authorizedClientToken;
    AccessToken notAuthorizedClientToken;
    AccessToken authorizedUserToken;
    AccessToken otherCompanyUserToken;
    AccessToken authorizedAdminToken;
    AccessToken otherCompanyAdminToken;
    
    
    @Before
    public void setUp() throws Exception {

        mockOauthService = EasyMock.createMock(OAuthService.class);
        mockClientService = EasyMock.createMock(ClientService.class);

        service = new DefaultAuthorizationService(mockOauthService, mockClientService, new StubLogger());
        
        setUpObjects();
    }
    
    @Test
    public void ShouldReturnTrueForRacker() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(trustedToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeRacker(authHeader);
        
        Assert.assertTrue(authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnFalseForRacker() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(authorizedUserToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeRacker(authHeader);
        
        Assert.assertTrue(!authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnTrueForClient() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(authorizedClientToken);
        EasyMock.replay(mockOauthService);
        
        EasyMock.expect(mockClientService.getDefinedPermissionByClientIdAndPermissionId(clientId, permissionId)).andReturn(perm);
        EasyMock.replay(mockClientService);
        
        boolean authorized = service.authorizeClient(authHeader, verb, uri);
        
        Assert.assertTrue(authorized);
        EasyMock.verify(mockOauthService);
        EasyMock.verify(mockClientService);
    }
    
    @Test
    public void ShouldReturnFalseForClient() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(notAuthorizedClientToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeClient(authHeader, verb, uri);
        
        Assert.assertTrue(!authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnTrueForAdmin() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(authorizedAdminToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeAdmin(authHeader, customerId);
        
        Assert.assertTrue(authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnFalseForAdmin() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(otherCompanyAdminToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeAdmin(authHeader, customerId);
        
        Assert.assertTrue(!authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnTrueForUser() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(authorizedUserToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeUser(authHeader, customerId, username);
        
        Assert.assertTrue(authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnFalseForUser() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(otherCompanyUserToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeUser(authHeader, customerId, username);
        
        Assert.assertTrue(!authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnTrueForCompanyUser() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(authorizedUserToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeCustomerUser(authHeader, customerId);
        
        Assert.assertTrue(authorized);
        EasyMock.verify(mockOauthService);
    }
    
    @Test
    public void ShouldReturnFalseForCompanyUser() {
        EasyMock.expect(mockOauthService.getTokenFromAuthHeader(authHeader)).andReturn(otherCompanyUserToken);
        EasyMock.replay(mockOauthService);
        
        boolean authorized = service.authorizeCustomerUser(authHeader, customerId);
        
        Assert.assertTrue(!authorized);
        EasyMock.verify(mockOauthService);
    }
    
    private void setUpObjects() {
        perm = new Permission();
        perm.setClientId(clientId);
        perm.setCustomerId(customerId);
        perm.setPermissionId(permissionId);
        perm.setValue(permissionValue);
        
        permissions = new ArrayList<Permission>();
        permissions.add(perm);
        
        admin = new Role();
        admin.setName(adminRoleName);
        
        roles = new ArrayList<Role>();
        roles.add(admin);
        
        authorizedClient = new Client();
        authorizedClient.setClientId(clientId);
        authorizedClient.setCustomerId(customerId);
        authorizedClient.setPermissions(permissions);
        
        notAuthorizedClient = new Client();
        notAuthorizedClient.setClientId(clientId);
        notAuthorizedClient.setCustomerId(customerId);
        
        authorizedUser = new User();
        authorizedUser.setCustomerId(customerId);
        authorizedUser.setUsername(username);
        
        otherCompanyUser = new User();
        otherCompanyUser.setCustomerId(otherCustomerId);
        otherCompanyUser.setUsername(username);
        
        authorizedAdmin = new User();
        authorizedAdmin.setCustomerId(customerId);
        authorizedAdmin.setUsername(username);
        authorizedAdmin.setRoles(roles);
        
        otherCompanyAdmin = new User();
        otherCompanyAdmin.setCustomerId(otherCustomerId);
        otherCompanyAdmin.setUsername(username);
        otherCompanyAdmin.setRoles(roles);
        
        tokenExpiration = new DateTime();
        
        trustedToken = new AccessToken(tokenString, tokenExpiration,
            authorizedUser, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            true);
        
        authorizedClientToken = new AccessToken(tokenString, tokenExpiration,
            null, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
        
        notAuthorizedClientToken = new AccessToken(tokenString, tokenExpiration,
            null, notAuthorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
        
        authorizedUserToken = new AccessToken(tokenString, tokenExpiration,
            authorizedUser, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
        
        otherCompanyUserToken = new AccessToken(tokenString, tokenExpiration,
            otherCompanyUser, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
        
        authorizedAdminToken = new AccessToken(tokenString, tokenExpiration,
            authorizedAdmin, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
        
        otherCompanyAdminToken = new AccessToken(tokenString, tokenExpiration,
            otherCompanyAdmin, authorizedClient, AccessToken.IDM_SCOPE.FULL,
            false);
    }
}
