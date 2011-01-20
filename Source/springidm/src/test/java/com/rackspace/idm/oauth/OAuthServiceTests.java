package com.rackspace.idm.oauth;

import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.entities.*;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;
import junit.framework.Assert;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class OAuthServiceTests {

    UserService mockUserService;
    ClientService mockClientService;
    AccessTokenService mockAccessTokenService;
    RefreshTokenService mockRefreshTokenService;
    OAuthService oauthService;
    AuthHeaderHelper authHeaderHelper;

    String customerId = "123-456-789";
    String clientId = "DELETE_My_ClientId";
    String clientSecret = "DELETE_My_Client_Secret";
    String username = "someuser";
    Password userpass = Password.newInstance("secret");
    String firstname = "John";
    String lastname = "Smith";
    String useremail = "someuser@example.com";
    String tokenVal = "asdf1234";
    String refreshTokenVal = "somerefreshtoken1234";
    int expireInSeconds = 3600;

    @Before
    public void setUp() {
        mockUserService = EasyMock.createMock(UserService.class);
        mockClientService = EasyMock.createMock(ClientService.class);
        mockAccessTokenService = EasyMock.createMock(AccessTokenService.class);
        mockRefreshTokenService = EasyMock.createMock(RefreshTokenService.class);
        authHeaderHelper = new AuthHeaderHelper();
        oauthService = new DefaultOAuthService(mockUserService, mockClientService, mockAccessTokenService,
                mockRefreshTokenService, new StubLogger());
    }

    @Test
    public void shouldAssertTokenIsExpired() throws Exception {
        AccessToken accessToken = new AccessToken(tokenVal,
                MemcachedAccessTokenRepository.DATE_PARSER.parseDateTime("20001231210627.300Z"), getFakeUser(),
                getTestClient(), IDM_SCOPE.FULL);
        Assert.assertTrue(accessToken.isExpired(new DateTime()));
    }

    @Test
    public void shouldPassAuthenticationWithNotExpiredToken() throws Exception {
        AccessToken accessToken = getFakeAccessToken();
        Assert.assertTrue(!accessToken.isExpired(new DateTime()));
    }

    @Test
    public void shouldGetTokenWithGrantTypeBasicCredentials() {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        DateTime currentTime = new DateTime();

        User user = getFakeUser();
        Client testClient = getTestClient();
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        EasyMock.expect(mockUserService.authenticate(authCredentials.getUsername(), userpass.getValue()))
                .andReturn(uaResult);
        EasyMock.expect(mockAccessTokenService.getDefaultTokenExpirationSeconds()).andReturn(3600);
        EasyMock.expect(
                mockAccessTokenService.getTokenByBasicCredentials(testClient, user, expireInSeconds, currentTime))
                .andReturn(testAccessToken);

        EasyMock.expect(mockRefreshTokenService
                .getRefreshTokenByUserAndClient(authCredentials.getUsername(), authCredentials.getClientId(),
                        currentTime)).andReturn(null);
        EasyMock.expect(mockRefreshTokenService
                .createRefreshTokenForUser(authCredentials.getUsername(), authCredentials.getClientId()))
                .andReturn(testRefreshToken);

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret)).andReturn(caResult);

        EasyMock.replay(mockUserService, mockAccessTokenService, mockRefreshTokenService, mockClientService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService, mockAccessTokenService, mockRefreshTokenService, mockClientService);
    }

    @Test
    public void shouldGetTokenWithGrantTypeNone() {

        OAuthGrantType grantType = OAuthGrantType.NONE;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("none");

        AccessToken testAccessToken = getFakeAccessToken();
        DateTime currentTime = new DateTime();

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);
        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockAccessTokenService.getDefaultTokenExpirationSeconds()).andReturn(3600);
        EasyMock.expect(mockAccessTokenService.getAccessTokenForClient(caResult.getClient(), currentTime))
                .andReturn(testAccessToken);
        EasyMock.replay(mockClientService, mockAccessTokenService);
        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNull(authData.getRefreshToken());

        EasyMock.verify(mockClientService, mockAccessTokenService);
    }

    @Test
    public void shouldCreateUserAccessTokenWithValidRefreshToken() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        DateTime currentTime = new DateTime();

        EasyMock.expect(mockRefreshTokenService.getRefreshTokenByTokenString(refreshTokenVal))
                .andReturn(testRefreshToken);
        mockRefreshTokenService.resetTokenExpiration(testRefreshToken);

        EasyMock.expect(mockAccessTokenService
                .createAccessTokenForUser(authCredentials.getUsername(), authCredentials.getClientId(),
                        expireInSeconds)).andReturn(testAccessToken);
        EasyMock.expect(mockAccessTokenService.getDefaultTokenExpirationSeconds()).andReturn(3600);

        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret))
                .andReturn(new ClientAuthenticationResult(getTestClient(), true));

        EasyMock.replay(mockRefreshTokenService, mockAccessTokenService, mockClientService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockRefreshTokenService, mockAccessTokenService, mockClientService);
    }

    @Test
    public void shouldCreateNewAccessTokenWhenExpired() throws Exception {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        testAccessToken
                .setExpirationTime(MemcachedAccessTokenRepository.DATE_PARSER.parseDateTime("20000101000000.300Z"));
        DateTime currentTime = new DateTime();

        UserAuthenticationResult uaResult = new UserAuthenticationResult(getFakeUser(), true);
        ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);

        EasyMock.expect(mockUserService.authenticate(authCredentials.getUsername(), userpass.getValue()))
                .andReturn(uaResult);

        EasyMock.expect(mockAccessTokenService
                .getTokenByBasicCredentials(caResult.getClient(), uaResult.getUser(), expireInSeconds, currentTime))
                .andReturn(testAccessToken);
        EasyMock.expect(mockAccessTokenService.getDefaultTokenExpirationSeconds()).andReturn(expireInSeconds);

        EasyMock.expect(mockRefreshTokenService
                .getRefreshTokenByUserAndClient(authCredentials.getUsername(), authCredentials.getClientId(),
                        currentTime)).andReturn(null);
        EasyMock.expect(mockRefreshTokenService
                .createRefreshTokenForUser(authCredentials.getUsername(), authCredentials.getClientId()))
                .andReturn(testRefreshToken);

        EasyMock.expect(mockClientService.authenticate(clientId, clientSecret)).andReturn(caResult);

        EasyMock.replay(mockUserService, mockAccessTokenService, mockRefreshTokenService, mockClientService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService, mockAccessTokenService, mockRefreshTokenService, mockClientService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfFailUserAuthentication() throws Exception {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        DateTime currentTime = new DateTime();
        User testuser = getFakeUser();

        EasyMock.expect(mockUserService.getUser(authCredentials.getUsername())).andReturn(testuser);
        UserAuthenticationResult uaResult = new UserAuthenticationResult(testuser, false);
        EasyMock.expect(mockUserService.authenticate(authCredentials.getUsername(), userpass.getValue()))
                .andReturn(uaResult);

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);
        EasyMock.expect(
                mockClientService.authenticate(authCredentials.getClientId(), authCredentials.getClientSecret()))
                .andReturn(caResult);

        EasyMock.expect(mockAccessTokenService.getDefaultTokenExpirationSeconds()).andReturn(expireInSeconds);
        EasyMock.expect(mockAccessTokenService
                .getTokenByBasicCredentials(caResult.getClient(), uaResult.getUser(), expireInSeconds, currentTime))
                .andReturn(getFakeAccessToken());

        RefreshToken fakeRefreshToken = getFakeRefreshToken();
        EasyMock.expect(mockRefreshTokenService.getRefreshTokenByUserAndClient(username, clientId, currentTime))
                .andReturn(fakeRefreshToken);
        mockRefreshTokenService.resetTokenExpiration(fakeRefreshToken);
        EasyMock.expectLastCall();

        EasyMock.replay(mockUserService, mockClientService, mockAccessTokenService, mockRefreshTokenService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockUserService, mockClientService, mockAccessTokenService, mockRefreshTokenService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfInvalidRefreshToken() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        DateTime currentTime = new DateTime();

        EasyMock.expect(mockRefreshTokenService.getRefreshTokenByTokenString(refreshTokenVal)).andReturn(null);

        ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);
        EasyMock.expect(
                mockClientService.authenticate(authCredentials.getClientId(), authCredentials.getClientSecret()))
                .andReturn(caResult);

        EasyMock.replay(mockRefreshTokenService, mockClientService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockRefreshTokenService, mockClientService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfRefreshTokenIsExpired() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        testRefreshToken
                .setExpirationTime(MemcachedAccessTokenRepository.DATE_PARSER.parseDateTime("20000101000000.300Z"));
        DateTime currentTime = new DateTime();

        EasyMock.expect(mockRefreshTokenService.getRefreshTokenByTokenString(refreshTokenVal))
                .andReturn(testRefreshToken);
        ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);
        EasyMock.expect(
                mockClientService.authenticate(authCredentials.getClientId(), authCredentials.getClientSecret()))
                .andReturn(caResult);

        EasyMock.replay(mockRefreshTokenService, mockClientService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockRefreshTokenService, mockClientService);
    }

    @Ignore
    @Test
    public void shouldRevokeToken() {
        //TODO Moved from AccessTokenService

        /*User testUser = getFakeUser();
        Client testClient = getFakeClient();

        AccessToken userToken = getFakeUserToken();
        userToken.setTokenClient(testClient);
        AccessToken clientToken = getFakeClientToken();
        clientToken.setTokenUser(testUser);

        EasyMock.expect(
            mockTokenDao.findByTokenString(userToken.getTokenString()))
            .andReturn(userToken);
        EasyMock.expect(
            mockTokenDao.findByTokenString(userToken.getTokenString()))
            .andReturn(clientToken);
        mockTokenDao.delete(userToken.getTokenString());
        EasyMock.replay(mockTokenDao);

        EasyMock.expect(mockUserService.getUser(testUser.getUsername()))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientDao);

        Set<String> tokenRequestors = new HashSet<String>();
        tokenRequestors.add(testClient.getClientId());
        mockRefreshTokenDao.deleteAllTokensForUser(testUser.getUsername(),
            tokenRequestors);
        EasyMock.replay(mockRefreshTokenDao);

        tokenService.revokeToken(clientToken.getTokenString(),
            userToken.getTokenString());

        EasyMock.verify(mockTokenDao);*/
    }

    @Ignore
    @Test(expected = IllegalStateException.class)
    public void shouldNotRevokeTokenForTokenNotFound() {
        //TODO Moved from AccessTokenService

        /*AccessToken userToken = getFakeUserToken();
        AccessToken clientToken = getFakeClientToken();

        EasyMock.expect(
            mockTokenDao.findByTokenString(userToken.getTokenString()))
            .andReturn(null);
        EasyMock.expect(
            mockTokenDao.findByTokenString(clientToken.getTokenString()))
            .andReturn(clientToken);
        mockTokenDao.delete(userToken.getTokenString());
        EasyMock.replay(mockTokenDao);

        tokenService.revokeToken(clientToken.getTokenString(),
            userToken.getTokenString());*/
    }

    @Ignore
    @Test(expected = IllegalStateException.class)
    public void shouldNotRevokeTokenForTokenThatDoesNotExist() {
        //TODO Moved from AccessTokenService
        /*AccessToken userToken = getFakeUserToken();
        AccessToken clientToken = getFakeClientToken();

        EasyMock.expect(
            mockTokenDao.findByTokenString(clientToken.getTokenString()))
            .andReturn(null);
        EasyMock.replay(mockTokenDao);

        tokenService.revokeToken(clientToken.getTokenString(),
            userToken.getTokenString());*/
    }

    // helpers
    private AccessToken getFakeAccessToken() {
        return new AccessToken(tokenVal,
                MemcachedAccessTokenRepository.DATE_PARSER.parseDateTime("20201231210627.300Z"), getFakeUser(),
                getTestClient(), IDM_SCOPE.FULL);
    }

    private RefreshToken getFakeRefreshToken() {
        return new RefreshToken(refreshTokenVal, new DateTime().plusSeconds(expireInSeconds), username, clientId);
    }

    private AuthData getFakeAuthData() {
        AccessToken accessToken = getFakeAccessToken();
        RefreshToken refreshToken = getFakeRefreshToken();

        AuthData authData = new AuthData(accessToken, refreshToken);
        return authData;
    }

    private User getFakeUser() {
        User user =
                new User(username, customerId, useremail, new UserHumanName(firstname, "", lastname), new UserLocale(),
                        new UserCredential(userpass, "", ""));
        user.setApiKey("1234567890");
        return user;
    }

    private Client getTestClient() {
        Client client = new Client(clientId, ClientSecret.newInstance(clientSecret), "DELETE_My_Name", "inum", "iname",
                "RCN-123-456-789", ClientStatus.ACTIVE, "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111",
                "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111");
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