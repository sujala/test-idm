package com.rackspace.idm.services;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.domain.config.MemcachedConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DefaultAuthorizationService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;
import junit.framework.Assert;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
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
    String idmClientId = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    String clientId = "clientId";

    String username = "username";

    String verb = "GET";
    String uri = "/resource";

    String permissionId = "Permission";
    String permissionValue = verb + " " + uri;

    BaseClient authorizedClient;
    BaseClient notAuthorizedClient;
    BaseClient nonRackspaceClient;
    BaseClient idmClient;

    BaseUser authorizedUser;
    BaseUser otherCompanyUser;

    BaseUser authorizedAdmin;
    BaseUser otherCompanyAdmin;

    Permission perm;
    List<Permission> permissions;
    ClientGroup admin;
    List<ClientGroup> groups;

    String adminRoleName = "Idm Admin";

    AccessToken trustedToken;
    AccessToken authorizedClientToken;
    AccessToken notAuthorizedClientToken;
    AccessToken nonRackspaceClientToken;
    AccessToken authorizedUserToken;
    AccessToken otherCompanyUserToken;
    AccessToken authorizedAdminToken;
    AccessToken otherCompanyAdminToken;
    AccessToken customerIdmToken;

    @Before
    public void setUp() throws Exception {
        mockClientDao = EasyMock.createMock(ClientDao.class);

        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        service = new DefaultAuthorizationService(mockClientDao,
            appConfig);
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

        boolean authorized = service
            .authorizeRackspaceClient(authorizedClientToken);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForRackspaceClient() {

        boolean authorized = service
            .authorizeRackspaceClient(nonRackspaceClientToken);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForClient() {

        EasyMock.expect(
            mockClientDao.getDefinedPermissionByClientIdAndPermissionId(
                idmClientId, permissionId)).andReturn(perm);
        EasyMock.replay(mockClientDao);

        boolean authorized = service.authorizeClient(authorizedClientToken,
            verb, uri);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForClient() {

        boolean authorized = service.authorizeClient(notAuthorizedClientToken,
            verb, uri);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForAdmin() {

        boolean authorized = service.authorizeAdmin(authorizedAdminToken,
            customerId);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForAdmin() {

        boolean authorized = service.authorizeAdmin(otherCompanyAdminToken,
            customerId);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForUser() {

        boolean authorized = service.authorizeUser(authorizedUserToken,
            customerId, username);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForUser() {

        boolean authorized = service.authorizeUser(otherCompanyUserToken,
            customerId, username);

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

        boolean authorized = service.authorizeCustomerUser(
            otherCompanyUserToken, customerId);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForCustomerIdm() {
        boolean authorized = service.authorizeCustomerIdm(customerIdmToken);
        
        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForCustomerIdm() {
        boolean authorized = service.authorizeCustomerIdm(notAuthorizedClientToken);
        
        Assert.assertTrue(!authorized);
    }

    private void setUpObjects() {
        perm = new Permission();
        perm.setClientId(idmClientId);
        perm.setCustomerId(customerId);
        perm.setPermissionId(permissionId);
        perm.setValue(permissionValue);

        permissions = new ArrayList<Permission>();
        permissions.add(perm);

        admin = new ClientGroup();
        admin.setName(adminRoleName);
        admin.setClientId(idmClientId);
        admin.setCustomerId(customerId);

        groups = new ArrayList<ClientGroup>();
        groups.add(admin);

        idmClient = new BaseClient(idmClientId, customerId, permissions);

        authorizedClient = new BaseClient(clientId, customerId, permissions);
        notAuthorizedClient = new BaseClient(clientId, customerId);
        nonRackspaceClient = new BaseClient(clientId, otherCustomerId);

        authorizedUser = new BaseUser(username, customerId);
        otherCompanyUser = new BaseUser(username, otherCustomerId);
        authorizedAdmin = new BaseUser(username, customerId, groups);
        otherCompanyAdmin = new BaseUser(username, otherCustomerId, groups);

        tokenExpiration = new DateTime();

        trustedToken = new AccessToken(tokenString, tokenExpiration,
            authorizedUser, authorizedClient, AccessToken.IDM_SCOPE.FULL, true);

        authorizedClientToken = new AccessToken(tokenString, tokenExpiration,
            null, authorizedClient, AccessToken.IDM_SCOPE.FULL, false);

        notAuthorizedClientToken = new AccessToken(tokenString,
            tokenExpiration, null, notAuthorizedClient,
            AccessToken.IDM_SCOPE.FULL, false);

        nonRackspaceClientToken = new AccessToken(tokenString, tokenExpiration,
            null, nonRackspaceClient, AccessToken.IDM_SCOPE.FULL, false);

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

        customerIdmToken = new AccessToken(tokenString, tokenExpiration, null,
            idmClient, AccessToken.IDM_SCOPE.FULL, false);
    }
}
