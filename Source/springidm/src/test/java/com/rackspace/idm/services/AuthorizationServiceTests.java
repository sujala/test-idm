package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;

public class AuthorizationServiceTests {
    ClientService mockClientService;
    ScopeAccessObjectDao mockScopeAccessDao;
    AuthorizationService service;
    

    String authHeader = "OAuth XXXX";
    
    String methodName = "methodName";

    String rackerId = "rackerId";
    String tokenString = "XXXX";
    DateTime tokenExpiration;

    String customerId = "RACKSPACE";
    String otherCustomerId = "RCN-000-000-000";
    String idmClientId = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    String clientId = "clientId";

    String username = "username";

    String verb = "GET";
    String uri = "/resource";
    
    List<PathSegment> segments = null;

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

    RackerScopeAccessObject trustedToken;
    ClientScopeAccessObject authorizedClientToken;
    ClientScopeAccessObject notAuthorizedClientToken;
    ClientScopeAccessObject nonRackspaceClientToken;
    UserScopeAccessObject authorizedUserToken;
    UserScopeAccessObject otherCompanyUserToken;
    UserScopeAccessObject authorizedAdminToken;
    UserScopeAccessObject otherCompanyAdminToken;
    ClientScopeAccessObject customerIdmToken;

    @Before
    public void setUp() throws Exception {
        mockClientService = EasyMock.createMock(ClientService.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessObjectDao.class);
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        service = new DefaultAuthorizationService(mockScopeAccessDao,
            mockClientService, appConfig);
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

        EasyMock.expect(mockScopeAccessDao.doesAccessTokenHavePermission(tokenString, perm)).andReturn(true);
        EasyMock.replay(mockScopeAccessDao);

        boolean authorized = service.authorizeClient(authorizedClientToken, verb, segments);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForClient() {

        EasyMock.expect(mockScopeAccessDao.doesAccessTokenHavePermission(tokenString, perm)).andReturn(false);
        EasyMock.replay(mockScopeAccessDao);
        
        boolean authorized = service.authorizeClient(notAuthorizedClientToken, verb, segments);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForAdmin() {

        EasyMock.expect(mockClientService.isUserMemberOfClientGroup(username, admin)).andReturn(true);
        EasyMock.replay(mockClientService);
        boolean authorized = service.authorizeAdmin(authorizedAdminToken,
            customerId);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForAdmin() {

        EasyMock.expect(mockClientService.isUserMemberOfClientGroup(username, admin)).andReturn(false);
        EasyMock.replay(mockClientService);
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
        boolean authorized = service
            .authorizeCustomerIdm(notAuthorizedClientToken);

        Assert.assertTrue(!authorized);
    }

    private void setUpObjects() {
        
        segments = new ArrayList<PathSegment>();
        segments.add(new PathSegment() {

            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return null;
            }

            @Override
            public String getPath() {
                return "/";
            }});
        segments.add(new PathSegment() {

            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return null;
            }

            @Override
            public String getPath() {
                return "resource";
            }});
        
        perm = new Permission();
        perm.setClientId(idmClientId);
        perm.setCustomerId(customerId);
        perm.setPermissionId(permissionId);

        permissions = new ArrayList<Permission>();
        permissions.add(perm);

        admin = new ClientGroup();
        admin.setName(adminRoleName);
        admin.setClientId(idmClientId);
        admin.setCustomerId(customerId);

        trustedToken = new RackerScopeAccessObject();
        trustedToken.setRackerId(rackerId);

        authorizedClientToken = new ClientScopeAccessObject();
        authorizedClientToken.setAccessTokenString(tokenString);
        authorizedClientToken.setClientId(clientId);
        authorizedClientToken.setClientRCN(customerId);

        notAuthorizedClientToken = new ClientScopeAccessObject();
        notAuthorizedClientToken.setAccessTokenString(tokenString);
        notAuthorizedClientToken.setClientId(clientId);
        notAuthorizedClientToken.setClientRCN(customerId);

        nonRackspaceClientToken = new ClientScopeAccessObject();
        nonRackspaceClientToken.setAccessTokenString(tokenString);
        nonRackspaceClientToken.setClientId(clientId);
        nonRackspaceClientToken.setClientRCN(otherCustomerId);

        authorizedUserToken = new UserScopeAccessObject();
        authorizedUserToken.setAccessTokenString(tokenString);
        authorizedUserToken.setUsername(username);
        authorizedUserToken.setUserRCN(customerId);

        otherCompanyUserToken = new UserScopeAccessObject();
        otherCompanyUserToken.setAccessTokenString(tokenString);
        otherCompanyUserToken.setUsername(username);
        otherCompanyUserToken.setUserRCN(otherCustomerId);

        authorizedAdminToken = new UserScopeAccessObject();
        authorizedAdminToken.setAccessTokenString(tokenString);
        authorizedAdminToken.setUsername(username);
        authorizedAdminToken.setUserRCN(customerId);

        otherCompanyAdminToken = new UserScopeAccessObject();
        otherCompanyAdminToken.setAccessTokenString(tokenString);
        otherCompanyAdminToken.setUsername(username);
        otherCompanyAdminToken.setUserRCN(otherCustomerId);

        customerIdmToken = new ClientScopeAccessObject();
        customerIdmToken.setAccessTokenString(tokenString);
        customerIdmToken.setClientId(idmClientId);
        customerIdmToken.setClientRCN(customerId);
    }
}
