package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class TokenServiceTests {

    ApplicationService mockClientService;
    AuthorizationService mockAuthorizationService;
    TokenService tokenService;
    AuthHeaderHelper authHeaderHelper;
    Configuration mockConfiguration;
    ScopeAccessService mockScopeAccessService;
    UserService userService;
    TenantService mockTenantService;

    String customerId = "RACKSPACE";
    String clientId = "DELETE_My_ClientId";
    String clientSecret = "DELETE_My_Client_Secret";
    String username = "someuser";
    Password userpass = Password.newInstance("secret");
    String firstname = "John";
    String lastname = "Smith";
    String useremail = "someuser@example.com";
    String tokenVal = "asdf1234";
    String requestorToken = "requestortoken5678";
    String refreshTokenVal = "somerefreshtoken1234";
    int expireInSeconds = 3600;
    String rackerId = "rackerId";
    String uniqueId = "uniqueId";

    @Before
    public void setUp() {
        mockClientService = EasyMock.createMock(ApplicationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        mockAuthorizationService = EasyMock.createNiceMock(AuthorizationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        authHeaderHelper = new AuthHeaderHelper();
        mockConfiguration = EasyMock.createMock(Configuration.class);
        userService = EasyMock.createMock(UserService.class);
        mockTenantService = EasyMock.createMock(TenantService.class);

        final Configuration appConfig = new PropertiesConfiguration();
        appConfig.addProperty("rackspace.customerId", "RACKSPACE");

        tokenService = new DefaultTokenService();
        tokenService.setClientService(mockClientService);
        tokenService.setAuthorizationService(mockAuthorizationService);
        tokenService.setConfig(appConfig);
        tokenService.setScopeAccessService(mockScopeAccessService);
        tokenService.setUserService(userService);
        tokenService.setTenantService(mockTenantService);
    }

    @Test
    public void shouldRevokeAllTokensForClient() {
        mockScopeAccessService.expireAllTokensForClient(clientId);
        EasyMock.replay(mockScopeAccessService);
        tokenService.revokeAllTokensForClient(clientId);
        EasyMock.verify(mockScopeAccessService);
    }

    @Test
    public void shouldRevokeAllTokensForUser() throws IOException, JAXBException {
        mockScopeAccessService.expireAllTokensForUser(username);
        EasyMock.replay(mockScopeAccessService);
        tokenService.revokeAllTokensForUser(username);
        EasyMock.verify(mockScopeAccessService);
    }

    @Test
    public void shouldRevokeAccessToken() {

        final UserScopeAccess usa = getFakeUserScopeAccess();
        final ClientScopeAccess csa = getFakeClientScopeAccess();
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(usa);
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(csa);
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(EasyMock.anyObject(ScopeAccess.class)))
        .andReturn(true);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockScopeAccessService, mockAuthorizationService);
        tokenService.revokeAccessToken(tokenVal, tokenVal);

        EasyMock.verify(mockScopeAccessService, mockAuthorizationService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotRevokeAccessTokenForDeletingTokenNotFound() {

        getFakeUserScopeAccess();
        final ClientScopeAccess csa = getFakeClientScopeAccess();
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(null);
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(csa);
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(EasyMock.anyObject(ScopeAccess.class)))
        .andReturn(true);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockScopeAccessService, mockAuthorizationService);
        tokenService.revokeAccessToken(tokenVal, tokenVal);

        EasyMock.verify(mockScopeAccessService, mockAuthorizationService);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotRevokeAccessTokenForRequestingTokenNotFound() {

        final UserScopeAccess usa = getFakeUserScopeAccess();
        final ClientScopeAccess csa = getFakeClientScopeAccess();
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(usa);
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(null);
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(EasyMock.anyObject(ScopeAccess.class)))
        .andReturn(true);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockScopeAccessService, mockAuthorizationService);
        tokenService.revokeAccessToken(tokenVal, tokenVal);

        EasyMock.verify(mockScopeAccessService, mockAuthorizationService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotRevokeAccessTokenForForbidden() {

        final UserScopeAccess usa = getFakeUserScopeAccess();
        final ClientScopeAccess csa = getFakeClientScopeAccess();
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(usa);
        EasyMock.expect(
                mockScopeAccessService.getScopeAccessByAccessToken(tokenVal))
                .andReturn(csa);
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(EasyMock.anyObject(ScopeAccess.class)))
        .andReturn(false);
        EasyMock.expect(mockAuthorizationService.authorizeAsRequestorOrOwner(usa, csa))
        .andReturn(false);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockScopeAccessService, mockAuthorizationService);
        tokenService.revokeAccessToken(tokenVal, tokenVal);

        EasyMock.verify(mockScopeAccessService, mockAuthorizationService);
    }

    @Test
    public void shouldAssertTokenIsExpired() throws Exception {
        final ScopeAccess sa = getFakeUserScopeAccess();
        final boolean isExpired = sa.isAccessTokenExpired(new DateTime().plusDays(2));
        Assert.assertTrue(isExpired);
    }

    @Test
    public void shouldPassAuthenticationWithNotExpiredToken() throws Exception {
        final ScopeAccess sa = getFakeUserScopeAccess();
        final boolean isExpired = sa.isAccessTokenExpired(new DateTime());
        Assert.assertFalse(isExpired);
    }

    private UserScopeAccess getFakeUserScopeAccess() {
        final UserScopeAccess usa = new UserScopeAccess();
        usa.setAccessTokenString(tokenVal);
        usa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        usa.setRefreshTokenString(refreshTokenVal);
        usa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        usa.setClientId(clientId);
        usa.setClientRCN(customerId);
        return usa;
    }

    private ClientScopeAccess getFakeClientScopeAccess() {
        final ClientScopeAccess csa = new ClientScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        return csa;
    }
}
