package com.rackspace.idm.oauth;

import java.util.Date;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.OAuthGrantType;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.OAuthService;
import com.rackspace.idm.domain.service.RefreshTokenService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultOAuthService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;

public class OAuthServiceTests {

    UserService mockUserService;
    ClientService mockClientService;
    AccessTokenService mockAccessTokenService;
    RefreshTokenService mockRefreshTokenService;
    AuthorizationService mockAuthorizationService;
    OAuthService oauthService;
    AuthHeaderHelper authHeaderHelper;
    InputValidator inputValidator;
    Configuration mockConfiguration;
    ScopeAccessService mockScopeAccessService;

    String customerId = "123-456-789";
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
        mockClientService = EasyMock.createMock(ClientService.class);
        mockAccessTokenService = EasyMock.createMock(AccessTokenService.class);
        mockRefreshTokenService = EasyMock
            .createMock(RefreshTokenService.class);
        mockAuthorizationService = EasyMock
            .createNiceMock(AuthorizationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        authHeaderHelper = new AuthHeaderHelper();
        inputValidator = EasyMock.createMock(InputValidator.class);
        mockConfiguration = EasyMock.createMock(Configuration.class);

        Configuration appConfig = new PropertiesConfiguration();
        appConfig.addProperty("token.expirationSeconds", expireInSeconds);

        oauthService = new DefaultOAuthService(mockUserService,
            mockClientService, mockAuthorizationService, appConfig,
            inputValidator, mockScopeAccessService);
    }

    @Test
    public void shouldAssertTokenIsExpired() throws Exception {
        ScopeAccessObject sa = getFakeUserScopeAccess();
        boolean isExpired = ((hasAccessToken) sa)
            .isAccessTokenExpired(new DateTime().plusDays(2));
        Assert.assertTrue(isExpired);
    }

    @Test
    public void shouldPassAuthenticationWithNotExpiredToken() throws Exception {
        ScopeAccessObject sa = getFakeUserScopeAccess();
        boolean isExpired = ((hasAccessToken) sa)
            .isAccessTokenExpired(new DateTime());
        Assert.assertFalse(isExpired);
    }

    @Test
    public void shouldGetTokenWithGrantTypeBasicCredentials() {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        DateTime currentTime = new DateTime();

        User user = getFakeUser();
        Client testClient = getTestClient();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user,
            true);
        EasyMock.expect(
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(uaResult);

        EasyMock.expect(
            mockScopeAccessService.getUserScopeAccessForClientId(
                user.getUniqueId(), testClient.getClientId())).andReturn(
            getFakeUserScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));

        EasyMock
            .expect(mockUserService.getUserPasswordExpirationDate(username))
            .andReturn(new DateTime().plusDays(5));

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(
            testClient, true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(caResult);

        EasyMock.replay(mockUserService, mockClientService,
            mockScopeAccessService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(((UserScopeAccessObject) authData)
            .getAccessTokenString());
        Assert.assertNotNull(((UserScopeAccessObject) authData)
            .getRefreshTokenString());

        EasyMock.verify(mockUserService, mockClientService,
            mockScopeAccessService);
    }

    @Test
    public void shouldGetPasswordResetTokenWhenPasswordRotationDurationHasExpired() {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        DateTime currentTime = new DateTime();

        User user = getFakeUser();
        Client testClient = getTestClient();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user,
            true);
        EasyMock.expect(
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(uaResult);

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(
            testClient, true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(caResult);

        EasyMock
            .expect(mockUserService.getUserPasswordExpirationDate(username))
            .andReturn(new DateTime().minusDays(5));

        EasyMock.expect(
            mockScopeAccessService
                .getOrCreatePasswordResetScopeAccessForUser(uniqueId))
            .andReturn(getFakePasswordResetScopeAccess());

        EasyMock.replay(mockUserService, mockClientService,
            mockScopeAccessService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(authData);
        Assert.assertTrue(authData instanceof PasswordResetScopeAccessObject);

        EasyMock.verify(mockUserService, mockClientService,
            mockScopeAccessService);
    }

    @Test
    public void shouldGetTokenWithGrantTypeNone() {

        OAuthGrantType grantType = OAuthGrantType.CLIENT_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("none");

        DateTime currentTime = new DateTime();

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(
            getTestClient(), true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(caResult);
        EasyMock.expect(
            mockScopeAccessService.getClientScopeAccessForClientId(uniqueId,
                clientId)).andReturn(getFakeClientScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));

        EasyMock.replay(mockClientService, mockAccessTokenService,
            mockScopeAccessService);
        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(authData);
        Assert.assertTrue(authData instanceof ClientScopeAccessObject);

        EasyMock.verify(mockClientService, mockAccessTokenService,
            mockScopeAccessService);
    }

    @Test
    public void shouldCreateUserAccessTokenWithValidRefreshToken() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        DateTime currentTime = new DateTime();

        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(new ClientAuthenticationResult(getTestClient(), true));

        EasyMock.expect(
            mockScopeAccessService
                .getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(
            getFakeUserScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));
        EasyMock.replay(mockScopeAccessService, mockClientService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(authData);
        Assert.assertTrue(authData instanceof UserScopeAccessObject);

        EasyMock.verify(mockScopeAccessService, mockClientService);
    }

    @Test
    public void shouldCreateNewAccessTokenWhenExpired() throws Exception {

        UserScopeAccessObject usa = getFakeUserScopeAccess();
        usa.setAccessTokenExp(new DateTime().minusDays(1).toDate());

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        DateTime currentTime = new DateTime();

        User user = getFakeUser();
        Client testClient = getTestClient();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user,
            true);
        EasyMock.expect(
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(uaResult);

        EasyMock.expect(
            mockScopeAccessService.getUserScopeAccessForClientId(
                user.getUniqueId(), testClient.getClientId())).andReturn(usa);
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));

        EasyMock
            .expect(mockUserService.getUserPasswordExpirationDate(username))
            .andReturn(new DateTime().plusDays(5));

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(
            testClient, true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(caResult);

        EasyMock.replay(mockUserService, mockClientService,
            mockScopeAccessService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(((UserScopeAccessObject) authData)
            .getAccessTokenString());
        Assert.assertNotNull(((UserScopeAccessObject) authData)
            .getRefreshTokenString());
        Assert.assertTrue(((UserScopeAccessObject) authData)
            .getAccessTokenExp().after(new Date()));

        EasyMock.verify(mockUserService, mockClientService,
            mockScopeAccessService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfFailUserAuthentication()
        throws Exception {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        DateTime currentTime = new DateTime();
        User testuser = getFakeUser();

        EasyMock.expect(mockUserService.getUser(authCredentials.getUsername()))
            .andReturn(testuser);
        UserAuthenticationResult uaResult = new UserAuthenticationResult(
            testuser, false);
        EasyMock.expect(
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(uaResult);

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(
            getTestClient(), true);
        EasyMock.expect(
            mockClientService.authenticate(authCredentials.getClientId(),
                authCredentials.getClientSecret())).andReturn(caResult);

        EasyMock.expectLastCall();

        EasyMock.replay(mockUserService, mockClientService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockUserService, mockClientService,
            mockAccessTokenService, mockRefreshTokenService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfInvalidRefreshToken() {

        UserScopeAccessObject usa = getFakeUserScopeAccess();
        usa.setRefreshTokenExp(new DateTime().minusDays(1).toDate());

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        DateTime currentTime = new DateTime();

        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(new ClientAuthenticationResult(getTestClient(), true));

        EasyMock.expect(
            mockScopeAccessService
                .getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(null);
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));
        EasyMock.replay(mockScopeAccessService, mockClientService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(authData);
        Assert.assertTrue(authData instanceof UserScopeAccessObject);

        EasyMock.verify(mockScopeAccessService, mockClientService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfRefreshTokenIsExpired() {

        UserScopeAccessObject usa = getFakeUserScopeAccess();
        usa.setRefreshTokenExp(new DateTime().minusDays(1).toDate());

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        DateTime currentTime = new DateTime();

        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
            .andReturn(new ClientAuthenticationResult(getTestClient(), true));

        EasyMock.expect(
            mockScopeAccessService
                .getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(usa);
        mockScopeAccessService.updateScopeAccess(EasyMock
            .anyObject(ScopeAccessObject.class));
        EasyMock.replay(mockScopeAccessService, mockClientService);

        ScopeAccessObject authData = oauthService.getTokens(grantType,
            authCredentials, currentTime);

        Assert.assertNotNull(authData);
        Assert.assertTrue(authData instanceof UserScopeAccessObject);

        EasyMock.verify(mockScopeAccessService, mockClientService);
    }

    // @Test
    // public void shouldRevokeToken() {
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(tokenVal))
    // .andReturn(getFakeAccessToken());
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(requestorToken))
    // .andReturn(getFakeClientAccessToken());
    // mockAccessTokenService.delete(tokenVal);
    // EasyMock.expectLastCall();
    // EasyMock.expect(
    // mockAuthorizationService
    // .authorizeCustomerIdm(getFakeClientAccessToken())).andReturn(
    // true);
    //
    // EasyMock.replay(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    //
    // oauthService.revokeTokensLocally(requestorToken, tokenVal);
    //
    // EasyMock.verify(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    // }
    //
    // @Test
    // public void shouldRevokeTokenForOwner() {
    //
    // AccessToken accessToken = getFakeAccessToken();
    //
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(tokenVal))
    // .andReturn(accessToken);
    //
    // mockAccessTokenService.deleteAllForOwner(accessToken.getOwner());
    // EasyMock.expectLastCall();
    //
    // EasyMock.expect(
    // mockAuthorizationService.authorizeCustomerIdm(accessToken))
    // .andReturn(true);
    //
    // EasyMock.replay(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    //
    // TokenDeleteByType queryType = TokenDeleteByType.owner;
    // oauthService.revokeTokensLocallyForOwnerOrCustomer(tokenVal, queryType,
    // accessToken.getOwner());
    //
    // EasyMock.verify(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    // }
    //
    // @Test
    // public void shouldRevokeTokenForCustomer() {
    //
    // oauthService = new DefaultOAuthService(mockUserService,
    // mockClientService, mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService, mockConfiguration, inputValidator);
    //
    // AccessToken accessToken = getFakeAccessToken();
    //
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(tokenVal))
    // .andReturn(accessToken);
    //
    // EasyMock.expect(mockConfiguration.getInt("ldap.paging.limit.max"))
    // .andReturn(1).atLeastOnce();
    // EasyMock.replay(mockConfiguration);
    //
    // Users usersObj = new Users();
    // List<User> users = new ArrayList<User>();
    // User testUser = getFakeUser();
    // users.add(testUser);
    // usersObj.setUsers(users);
    // usersObj.setTotalRecords(1);
    // List<User> usersList = new ArrayList<User>();
    // usersList.addAll(usersObj.getUsers());
    //
    // EasyMock.expect(mockUserService.getByCustomerId(customerId, 0, 1))
    // .andReturn(usersObj).atLeastOnce();
    // EasyMock.replay(mockUserService);
    //
    // Clients clientsObj = new Clients();
    // List<Client> clients = new ArrayList<Client>();
    // Client testClient = getTestClient();
    // clients.add(testClient);
    // clientsObj.setTotalRecords(1);
    // clientsObj.setClients(clients);
    // List<Client> clientsList = new ArrayList<Client>();
    // clientsList.addAll(clientsObj.getClients());
    //
    // EasyMock.expect(mockClientService.getByCustomerId(customerId, 0, 1))
    // .andReturn(clientsObj);
    // EasyMock.replay(mockClientService);
    //
    // mockAccessTokenService.deleteAllForOwner(testUser.getUsername());
    //
    // mockAccessTokenService.deleteAllForOwner(testClient.getClientId());
    //
    // EasyMock.expectLastCall();
    //
    // EasyMock.expect(
    // mockAuthorizationService.authorizeCustomerIdm(accessToken))
    // .andReturn(true);
    //
    // EasyMock.replay(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    //
    // TokenDeleteByType queryType = TokenDeleteByType.customer;
    // oauthService.revokeTokensLocallyForOwnerOrCustomer(tokenVal, queryType,
    // customerId);
    //
    // EasyMock.verify(mockAccessTokenService, mockRefreshTokenService,
    // mockAuthorizationService);
    // }
    //
    // @Test(expected = NotFoundException.class)
    // public void shouldNotRevokeTokenForTokenNotFound() {
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(tokenVal))
    // .andReturn(null);
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(requestorToken))
    // .andReturn(getFakeAccessToken());
    // mockAccessTokenService.delete(tokenVal);
    // EasyMock.expectLastCall();
    //
    // EasyMock.expect(mockUserService.getUser(username)).andReturn(null);
    // EasyMock.expect(mockClientService.getById(clientId)).andReturn(
    // getTestClient());
    //
    // mockRefreshTokenService.deleteAllTokensForUser(username);
    //
    // EasyMock.replay(mockAccessTokenService, mockUserService,
    // mockClientService, mockRefreshTokenService);
    //
    // oauthService.revokeTokensLocally(requestorToken, tokenVal);
    //
    // EasyMock.verify(mockAccessTokenService, mockUserService,
    // mockClientService, mockRefreshTokenService);
    // }
    //
    // @Test(expected = IllegalStateException.class)
    // public void shouldNotRevokeTokenForTokenThatDoesNotExist() {
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(tokenVal))
    // .andReturn(getFakeAccessToken());
    // EasyMock.expect(
    // mockAccessTokenService.getAccessTokenByTokenString(requestorToken))
    // .andReturn(null);
    // mockAccessTokenService.delete(tokenVal);
    // EasyMock.expectLastCall();
    //
    // EasyMock.expect(mockUserService.getUser(username)).andReturn(
    // getFakeUser());
    // EasyMock.expect(mockClientService.getById(clientId)).andReturn(null);
    //
    // mockRefreshTokenService.deleteAllTokensForUser(username);
    //
    // EasyMock.replay(mockAccessTokenService, mockUserService,
    // mockClientService, mockRefreshTokenService);
    //
    // oauthService.revokeTokensLocally(requestorToken, tokenVal);
    //
    // EasyMock.verify(mockAccessTokenService, mockUserService,
    // mockClientService, mockRefreshTokenService);
    // }

    private UserScopeAccessObject getFakeUserScopeAccess() {
        UserScopeAccessObject usa = new UserScopeAccessObject();
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

    private ClientScopeAccessObject getFakeClientScopeAccess() {
        ClientScopeAccessObject csa = new ClientScopeAccessObject();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        return csa;
    }

    private RackerScopeAccessObject getFakeRackerScopeAcces() {
        RackerScopeAccessObject csa = new RackerScopeAccessObject();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setRefreshTokenString(refreshTokenVal);
        csa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setRackerId(rackerId);
        return csa;
    }

    private PasswordResetScopeAccessObject getFakePasswordResetScopeAccess() {
        PasswordResetScopeAccessObject csa = new PasswordResetScopeAccessObject();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setUsername(username);
        csa.setUserRCN(customerId);
        return csa;
    }

    private User getFakeUser() {
        User user = new User(username, customerId, useremail,
            new UserHumanName(firstname, "", lastname), new UserLocale(),
            new UserCredential(userpass, "", ""));
        user.setApiKey("1234567890");
        user.setUniqueId(uniqueId);
        return user;
    }

    private Client getTestClient() {
        Client client = new Client(clientId,
            ClientSecret.newInstance(clientSecret), "DELETE_My_Name", "inum",
            "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        client.setUniqueId(uniqueId);
        return client;
    }

    private AuthCredentials getTestAuthCredentials() {

        AuthCredentials authCredentials = new AuthCredentials();
        authCredentials.setClientId(clientId);
        authCredentials.setClientSecret(clientSecret);
        authCredentials.setExpirationInSec(expireInSeconds);
        authCredentials.setGrantType("NONE");
        authCredentials.setUsername(username);
        authCredentials.setPassword(userpass.getValue());
        authCredentials.setRefreshToken(refreshTokenVal);

        return authCredentials;
    }
}