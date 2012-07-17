package com.rackspace.idm.domain.service.impl;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.HasAccessToken;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;

public class TokenServiceTests {

    UserService mockUserService;
    ApplicationService mockClientService;
    AuthorizationService mockAuthorizationService;
    TokenService tokenService;
    AuthHeaderHelper authHeaderHelper;
    InputValidator inputValidator;
    Configuration mockConfiguration;
    ScopeAccessService mockScopeAccessService;
    UserDao mockUserDao;
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
        mockUserService = EasyMock.createMock(UserService.class);
        mockClientService = EasyMock.createMock(ApplicationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        mockAuthorizationService = EasyMock
        .createNiceMock(AuthorizationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        authHeaderHelper = new AuthHeaderHelper();
        inputValidator = EasyMock.createMock(InputValidator.class);
        mockConfiguration = EasyMock.createMock(Configuration.class);
        mockUserDao = EasyMock.createMock(UserDao.class);
        mockTenantService = EasyMock.createMock(TenantService.class);

        final Configuration appConfig = new PropertiesConfiguration();
        appConfig.addProperty("token.expirationSeconds", expireInSeconds);
        appConfig.addProperty("rackspace.customerId", "RACKSPACE");

        tokenService = new DefaultTokenService(
                mockClientService, mockAuthorizationService, appConfig,
                inputValidator, mockScopeAccessService, mockUserDao, mockTenantService);
    }

    @Test
    public void shouldRevokeAllTokensForClient() {
        mockScopeAccessService.expireAllTokensForClient(clientId);
        EasyMock.replay(mockScopeAccessService);
        tokenService.revokeAllTokensForClient(clientId);
        EasyMock.verify(mockScopeAccessService);
    }

    @Test
    public void shouldRevokeAllTokensForUser() {
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
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(csa))
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
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(csa))
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
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(csa))
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
        EasyMock.expect(mockAuthorizationService.authorizeCustomerIdm(csa))
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
        final boolean isExpired = ((HasAccessToken) sa)
        .isAccessTokenExpired(new DateTime().plusDays(2));
        Assert.assertTrue(isExpired);
    }

    @Test
    public void shouldPassAuthenticationWithNotExpiredToken() throws Exception {
        final ScopeAccess sa = getFakeUserScopeAccess();
        final boolean isExpired = ((HasAccessToken) sa)
        .isAccessTokenExpired(new DateTime());
        Assert.assertFalse(isExpired);
    }

//	@Test
//	public void shouldRevokeToken() {
//		EasyMock.expect(mockScopeAccessService.getAccessTokenByTokenString(tokenVal)).andReturn(getFakeAccessToken());
//		EasyMock.expect(mockScopeAccessService.getAccessTokenByTokenString(requestorToken)).andReturn(getFakeClientAccessToken());
//		mockScopeAccessService.delete(tokenVal);
//		EasyMock.expectLastCall();
//
//		EasyMock.replay(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//
//		oauthService.revokeTokensLocally(requestorToken, tokenVal);
//
//		EasyMock.verify(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//	}
//    
//	@Test
//	public void shouldRevokeTokenForOwner() {
//
//		AccessToken accessToken = getFakeAccessToken();
//
//		EasyMock.expect(
//				mockScopeAccessService.getAccessTokenByTokenString(tokenVal))
//				.andReturn(accessToken);
//
//		mockScopeAccessService.deleteAllForOwner(accessToken.getOwner());
//		EasyMock.expectLastCall();
//
//		EasyMock.expect(
//				mockAuthorizationService.authorizeCustomerIdm(accessToken))
//				.andReturn(true);
//
//		EasyMock.replay(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//
//		TokenDeleteByType queryType = TokenDeleteByType.owner;
//		oauthService.revokeTokensLocallyForOwnerOrCustomer(tokenVal, queryType,
//				accessToken.getOwner());
//
//		EasyMock.verify(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//	}
//	
//	@Test
//	public void shouldRevokeTokenForCustomer() {
//
//		oauthService = new DefaultOAuthService(mockUserService,
//				mockClientService, mockScopeAccessService,
//				mockRefreshTokenService, mockAuthorizationService,
//				mockConfiguration, inputValidator);
//
//		AccessToken accessToken = getFakeAccessToken();
//
//		EasyMock.expect(
//				mockScopeAccessService.getAccessTokenByTokenString(tokenVal))
//				.andReturn(accessToken);
//
//		EasyMock.expect(mockConfiguration.getInt("ldap.paging.limit.max"))
//				.andReturn(1).atLeastOnce();
//		EasyMock.replay(mockConfiguration);
//
//		Users usersObj = new Users();
//		List<User> users = new ArrayList<User>();
//		User testUser = getFakeUser();
//		users.add(testUser);
//		usersObj.setUsers(users);
//		usersObj.setTotalRecords(1);
//		List<User> usersList = new ArrayList<User>();
//		usersList.addAll(usersObj.getUsers());
//
//		EasyMock.expect(mockUserService.getByCustomerId(customerId, 0, 1))
//				.andReturn(usersObj).atLeastOnce();
//		EasyMock.replay(mockUserService);
//
//		Clients clientsObj = new Clients();
//		List<Client> clients = new ArrayList<Client>();
//		Client testClient = getTestClient();
//		clients.add(testClient);
//		clientsObj.setTotalRecords(1);
//		clientsObj.setClients(clients);
//		List<Client> clientsList = new ArrayList<Client>();
//		clientsList.addAll(clientsObj.getClients());
//
//		EasyMock.expect(mockClientService.getByCustomerId(customerId, 0, 1))
//				.andReturn(clientsObj);
//		EasyMock.replay(mockClientService);
//
//		mockScopeAccessService.deleteAllForOwner(testUser.getUsername());
//
//		mockScopeAccessService.deleteAllForOwner(testClient.getClientId());
//
//		EasyMock.expectLastCall();
//
//		EasyMock.expect(
//				mockAuthorizationService.authorizeCustomerIdm(accessToken))
//				.andReturn(true);
//
//		EasyMock.replay(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//
//		TokenDeleteByType queryType = TokenDeleteByType.customer;
//		oauthService.revokeTokensLocallyForOwnerOrCustomer(tokenVal, queryType,
//				customerId);
//
//		EasyMock.verify(mockScopeAccessService, mockRefreshTokenService,
//				mockAuthorizationService);
//	}
//
//	@Test(expected = NotFoundException.class)
//	public void shouldNotRevokeTokenForTokenNotFound() {
//		EasyMock.expect(
//				mockScopeAccessService.getAccessTokenByTokenString(tokenVal))
//				.andReturn(null);
//		EasyMock.expect(
//				mockScopeAccessService
//						.getAccessTokenByTokenString(requestorToken))
//				.andReturn(getFakeAccessToken());
//		mockScopeAccessService.delete(tokenVal);
//		EasyMock.expectLastCall();
//
//		EasyMock.expect(mockUserService.getUser(username)).andReturn(null);
//		EasyMock.expect(mockClientService.getById(clientId)).andReturn(
//				getTestClient());
//
//		mockRefreshTokenService.deleteAllTokensForUser(username);
//
//		EasyMock.replay(mockScopeAccessService, mockUserService,
//				mockClientService, mockRefreshTokenService);
//
//		oauthService.revokeTokensLocally(requestorToken, tokenVal);
//
//		EasyMock.verify(mockScopeAccessService, mockUserService,
//				mockClientService, mockRefreshTokenService);
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void shouldNotRevokeTokenForTokenThatDoesNotExist() {
//		EasyMock.expect(
//				mockScopeAccessService.getAccessTokenByTokenString(tokenVal))
//				.andReturn(getFakeAccessToken());
//		EasyMock.expect(
//				mockScopeAccessService
//						.getAccessTokenByTokenString(requestorToken))
//				.andReturn(null);
//		mockScopeAccessService.delete(tokenVal);
//		EasyMock.expectLastCall();
//
//		EasyMock.expect(mockUserService.getUser(username)).andReturn(
//				getFakeUser());
//		EasyMock.expect(mockClientService.getById(clientId)).andReturn(null);
//
//		mockRefreshTokenService.deleteAllTokensForUser(username);
//
//		EasyMock.replay(mockScopeAccessService, mockUserService,
//				mockClientService, mockRefreshTokenService);
//
//		oauthService.revokeTokensLocally(requestorToken, tokenVal);
//
//		EasyMock.verify(mockScopeAccessService, mockUserService,
//				mockClientService, mockRefreshTokenService);
//	}

    private UserScopeAccess getFakeUserScopeAccess() {
        final UserScopeAccess usa = new UserScopeAccess();
        usa.setAccessTokenString(tokenVal);
        usa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        usa.setRefreshTokenString(refreshTokenVal);
        usa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        usa.setUsername(username);
        usa.setUserRCN(customerId);
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