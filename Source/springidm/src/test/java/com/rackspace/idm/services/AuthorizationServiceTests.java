package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.util.WadlTrie;

public class AuthorizationServiceTests {
    ClientDao mockClientDao;
    ScopeAccessDao mockScopeAccessDao;
    AuthorizationService service;
    WadlTrie mockWadlTrie;
    UriInfo mockUriInfo;
    
    UriInfo uriInfo = null;

    String authHeader = "OAuth XXXX";
    
    String uniqueId = "uniqueId";
    
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

    String adminGroupName = "Idm Admin";

    RackerScopeAccess trustedToken;
    ClientScopeAccess authorizedClientToken;
    ClientScopeAccess notAuthorizedClientToken;
    ClientScopeAccess nonRackspaceClientToken;
    UserScopeAccess authorizedUserToken;
    UserScopeAccess otherCompanyUserToken;
    UserScopeAccess authorizedAdminToken;
    UserScopeAccess otherCompanyAdminToken;
    ClientScopeAccess customerIdmToken;

    @Before
    public void setUp() throws Exception {
        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        mockWadlTrie = EasyMock.createMock(WadlTrie.class);
        mockUriInfo = EasyMock.createMock(UriInfo.class);
        Configuration appConfig = new PropertyFileConfiguration().getConfig();
        service = new DefaultAuthorizationService(mockScopeAccessDao,
            mockClientDao, mockWadlTrie, appConfig);
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

        EasyMock.expect(mockScopeAccessDao.doesAccessTokenHavePermission(authorizedClientToken, perm)).andReturn(true);
        EasyMock.replay(mockScopeAccessDao);

        
        EasyMock.expect(mockWadlTrie.getPermissionFor(verb, mockUriInfo)).andReturn(permissionId);
        EasyMock.replay(mockWadlTrie);


        boolean authorized = service.authorizeClient(authorizedClientToken, verb, mockUriInfo);

        Assert.assertTrue(authorized);
    }
    
    @Test
    public void ShouldReturnFalseForClientForMissingPermissionInWadl() {

        EasyMock.expect(mockScopeAccessDao.doesAccessTokenHavePermission(authorizedClientToken, perm)).andReturn(true);
        EasyMock.replay(mockScopeAccessDao);

        
        EasyMock.expect(mockWadlTrie.getPermissionFor(verb, mockUriInfo)).andReturn(null);
        EasyMock.replay(mockWadlTrie);


        boolean authorized = service.authorizeClient(authorizedClientToken, verb, mockUriInfo);

        Assert.assertFalse(authorized);
    }

    @Test
    public void ShouldReturnFalseForClient() {

        EasyMock.expect(mockScopeAccessDao.doesAccessTokenHavePermission(notAuthorizedClientToken, perm)).andReturn(false);
        EasyMock.replay(mockScopeAccessDao);
        

        EasyMock.expect(mockWadlTrie.getPermissionFor(verb, mockUriInfo)).andReturn(permissionId);
        EasyMock.replay(mockWadlTrie);
        
        boolean authorized = service.authorizeClient(notAuthorizedClientToken, verb, mockUriInfo);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForAdmin() {

        EasyMock.expect(mockClientDao.getClientGroup(customerId, idmClientId, adminGroupName)).andReturn(admin);
        EasyMock.expect(mockClientDao.isUserInClientGroup(username, admin.getUniqueId())).andReturn(true);
        EasyMock.replay(mockClientDao);
        boolean authorized = service.authorizeAdmin(authorizedAdminToken,
            customerId);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForAdmin() {

        EasyMock.expect(mockClientDao.getClientGroup(customerId, idmClientId, adminGroupName)).andReturn(admin);
        EasyMock.expect(mockClientDao.isUserInClientGroup(username, admin.getUniqueId())).andReturn(false);
        EasyMock.replay(mockClientDao);
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
        
        perm = new Permission();
        perm.setClientId(idmClientId);
        perm.setCustomerId(customerId);
        perm.setPermissionId(permissionId);

        permissions = new ArrayList<Permission>();
        permissions.add(perm);

        admin = new ClientGroup();
        admin.setUniqueId(uniqueId);
        admin.setName(adminGroupName);
        admin.setClientId(idmClientId);
        admin.setCustomerId(customerId);

        trustedToken = new RackerScopeAccess();
        trustedToken.setRackerId(rackerId);

        authorizedClientToken = new ClientScopeAccess();
        authorizedClientToken.setAccessTokenString(tokenString);
        authorizedClientToken.setClientId(clientId);
        authorizedClientToken.setClientRCN(customerId);

        notAuthorizedClientToken = new ClientScopeAccess();
        notAuthorizedClientToken.setAccessTokenString(tokenString);
        notAuthorizedClientToken.setClientId(clientId);
        notAuthorizedClientToken.setClientRCN(customerId);

        nonRackspaceClientToken = new ClientScopeAccess();
        nonRackspaceClientToken.setAccessTokenString(tokenString);
        nonRackspaceClientToken.setClientId(clientId);
        nonRackspaceClientToken.setClientRCN(otherCustomerId);

        authorizedUserToken = new UserScopeAccess();
        authorizedUserToken.setAccessTokenString(tokenString);
        authorizedUserToken.setUsername(username);
        authorizedUserToken.setUserRCN(customerId);

        otherCompanyUserToken = new UserScopeAccess();
        otherCompanyUserToken.setAccessTokenString(tokenString);
        otherCompanyUserToken.setUsername(username);
        otherCompanyUserToken.setUserRCN(otherCustomerId);

        authorizedAdminToken = new UserScopeAccess();
        authorizedAdminToken.setAccessTokenString(tokenString);
        authorizedAdminToken.setUsername(username);
        authorizedAdminToken.setUserRCN(customerId);

        otherCompanyAdminToken = new UserScopeAccess();
        otherCompanyAdminToken.setAccessTokenString(tokenString);
        otherCompanyAdminToken.setUsername(username);
        otherCompanyAdminToken.setUserRCN(otherCustomerId);

        customerIdmToken = new ClientScopeAccess();
        customerIdmToken.setAccessTokenString(tokenString);
        customerIdmToken.setClientId(idmClientId);
        customerIdmToken.setClientRCN(customerId);
    }
}
