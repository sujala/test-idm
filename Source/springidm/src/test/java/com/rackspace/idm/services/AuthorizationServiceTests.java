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
        mockClientDao = EasyMock.createMock(ClientDao.class);
        service = new DefaultAuthorizationService(mockClientDao, new StubLogger());
        setUpObjects();
    }

    @Test
    public void shouldReturnTrueForRacker() {
        boolean authorized = service.authorizeRacker(trustedToken);
        Assert.assertTrue(authorized);
    }

    @Test
    public void shouldReturnFalseForRacker() {

        boolean authorized = service.authorizeRacker(authorizedUserToken);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForRackspaceClient() {

        boolean authorized = service.authorizeRackspaceClient(authorizedClientToken);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForRackspaceClient() {

        boolean authorized = service.authorizeRackspaceClient(nonRackspaceClientToken);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForClient() {

        EasyMock.expect(
                mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                        clientId, permissionId)).andReturn(perm);
        EasyMock.replay(mockClientDao);

        boolean authorized = service.authorizeClient(authorizedClientToken, verb, uri);

        Assert.assertTrue(authorized);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void ShouldReturnFalseForClient() {

        boolean authorized = service.authorizeClient(notAuthorizedClientToken, verb, uri);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForAdmin() {

        boolean authorized = service.authorizeAdmin(authorizedAdminToken, customerId);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForAdmin() {

        boolean authorized = service.authorizeAdmin(otherCompanyAdminToken, customerId);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForUser() {

        boolean authorized = service.authorizeUser(authorizedUserToken, customerId,
                username);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForUser() {

        boolean authorized = service.authorizeUser(otherCompanyUserToken, customerId,
                username);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForCompanyUser() {

        boolean authorized = service.authorizeCustomerUser(authorizedUserToken,
                customerId);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForCompanyUser() {

        boolean authorized = service.authorizeCustomerUser(otherCompanyUserToken,
                customerId);

        Assert.assertTrue(!authorized);
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
