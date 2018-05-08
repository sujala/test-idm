package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import static org.powermock.api.mockito.PowerMockito.mock;

public class AuthorizationServiceTests {
    TenantDao mockTenantDao;
    ApplicationDao mockClientDao;
    ScopeAccessDao mockScopeAccessDao;
    AuthorizationService service;
    UriInfo mockUriInfo;
    
    String uniqueId = "uniqueId";

    String rackerId = "rackerId";
    String tokenString = "XXXX";

    String customerId = "RACKSPACE";
    String otherCustomerId = "RCN-000-000-000";
    String idmClientId = "18e7a7032733486cd32f472d7bd58f709ac0d221";
    String clientId = "clientId";

    String username = "username";

    String permissionId = "Permission";

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
        mockTenantDao = mock(TenantDao.class);
        mockClientDao = EasyMock.createMock(ApplicationDao.class);
        mockScopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        mockUriInfo = EasyMock.createMock(UriInfo.class);
        Configuration appConfig = new PropertyFileConfiguration().getConfig();
        service = new DefaultAuthorizationService();
        service.setConfig(appConfig);
        setUpObjects();
    }

    @Test
    public void shouldReturnFalseForRacker() {

        boolean authorized = service.authorizeRacker(authorizedAdminToken);

        Assert.assertTrue(!authorized);
    }

    @Test
    public void ShouldReturnTrueForRackspaceClient() {

        boolean authorized = service.authorizeRackspaceClient(authorizedClientToken);

        Assert.assertTrue(authorized);
    }

    @Test
    public void ShouldReturnFalseForRackspaceClient() {

        boolean authorized = service
            .authorizeRackspaceClient(nonRackspaceClientToken);

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

        otherCompanyUserToken = new UserScopeAccess();
        otherCompanyUserToken.setAccessTokenString(tokenString);

        authorizedAdminToken = new UserScopeAccess();
        authorizedAdminToken.setAccessTokenString(tokenString);

        otherCompanyAdminToken = new UserScopeAccess();
        otherCompanyAdminToken.setAccessTokenString(tokenString);

        customerIdmToken = new ClientScopeAccess();
        customerIdmToken.setAccessTokenString(tokenString);
        customerIdmToken.setClientId(idmClientId);
        customerIdmToken.setClientRCN(customerId);
    }
}
