package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
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
    UserScopeAccess authorizedUserToken;
    UserScopeAccess otherCompanyUserToken;
    UserScopeAccess authorizedAdminToken;
    UserScopeAccess otherCompanyAdminToken;

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

    private void setUpObjects() {
        
        trustedToken = new RackerScopeAccess();
        trustedToken.setRackerId(rackerId);

        authorizedUserToken = new UserScopeAccess();
        authorizedUserToken.setAccessTokenString(tokenString);

        otherCompanyUserToken = new UserScopeAccess();
        otherCompanyUserToken.setAccessTokenString(tokenString);

        authorizedAdminToken = new UserScopeAccess();
        authorizedAdminToken.setAccessTokenString(tokenString);

        otherCompanyAdminToken = new UserScopeAccess();
        otherCompanyAdminToken.setAccessTokenString(tokenString);
    }
}
