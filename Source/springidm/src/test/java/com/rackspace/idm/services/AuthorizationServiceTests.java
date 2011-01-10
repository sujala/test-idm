package com.rackspace.idm.services;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationServiceTests {
    AccessTokenDao mockAccessTokenDao;
    ClientDao mockClientDao;
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
    BaseClient nonRackspaceClient;

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
    AccessToken nonRackspaceClientToken;
    AccessToken authorizedUserToken;
    AccessToken otherCompanyUserToken;
    AccessToken authorizedAdminToken;
    AccessToken otherCompanyAdminToken;

    @Before
    public void setUp() throws Exception {
        mockAccessTokenDao = EasyMock.createMock(AccessTokenDao.class);
        mockClientDao = EasyMock.createMock(ClientDao.class);
        service = new DefaultAuthorizationService(mockAccessTokenDao, mockClientDao, new AuthHeaderHelper(), new StubLogger());
        setUpObjects();
    }

    @Test
    public void shouldReturnTrueForRacker() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString)).andReturn(trustedToken);
        EasyMock.replay(mockAccessTokenDao);
        boolean authorized = service.authorizeRacker(authHeader);
        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void shouldReturnFalseForRacker() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedUserToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeRacker(authHeader);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnTrueForRackspaceClient() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedClientToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeRackspaceClient(authHeader);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnFalseForRackspaceClient() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(nonRackspaceClientToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeRackspaceClient(authHeader);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnTrueForClient() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedClientToken);
        EasyMock.replay(mockAccessTokenDao);

        EasyMock.expect(
                mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                        clientId, permissionId)).andReturn(perm);
        EasyMock.replay(mockClientDao);

        boolean authorized = service.authorizeClient(authHeader, verb, uri);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldReturnFalseForClient() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(notAuthorizedClientToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeClient(authHeader, verb, uri);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnTrueForAdmin() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedAdminToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeAdmin(authHeader, customerId);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnFalseForAdmin() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(otherCompanyAdminToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeAdmin(authHeader, customerId);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnTrueForUser() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedUserToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeUser(authHeader, customerId,
                username);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnFalseForUser() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(otherCompanyUserToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeUser(authHeader, customerId,
                username);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnTrueForCompanyUser() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(authorizedUserToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeCustomerUser(authHeader,
                customerId);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockAccessTokenDao);
    }

    @Test
    public void ShouldReturnFalseForCompanyUser() {
        EasyMock.expect(mockAccessTokenDao.findByTokenString(tokenString))
                .andReturn(otherCompanyUserToken);
        EasyMock.replay(mockAccessTokenDao);

        boolean authorized = service.authorizeCustomerUser(authHeader,
                customerId);

        Assert.assertTrue(!authorized);
        EasyMock.verify(mockAccessTokenDao);
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

        nonRackspaceClient = new Client();
        nonRackspaceClient.setClientId(clientId);
        nonRackspaceClient.setCustomerId(otherCustomerId);

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
                authorizedUser, authorizedClient, AccessToken.IDM_SCOPE.FULL, true);

        authorizedClientToken = new AccessToken(tokenString, tokenExpiration,
                null, authorizedClient, AccessToken.IDM_SCOPE.FULL, false);

        notAuthorizedClientToken = new AccessToken(tokenString,
                tokenExpiration, null, notAuthorizedClient,
                AccessToken.IDM_SCOPE.FULL, false);

        nonRackspaceClientToken = new AccessToken(tokenString,
                tokenExpiration, null, nonRackspaceClient,
                AccessToken.IDM_SCOPE.FULL, false);

        authorizedUserToken = new AccessToken(tokenString, tokenExpiration,
                authorizedUser, authorizedClient, AccessToken.IDM_SCOPE.FULL, false);

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
