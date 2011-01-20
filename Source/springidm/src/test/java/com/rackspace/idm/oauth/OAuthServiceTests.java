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

import java.util.HashSet;
import java.util.Set;

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
        mockRefreshTokenService = EasyMock
                .createMock(RefreshTokenService.class);
        authHeaderHelper = new AuthHeaderHelper();
        oauthService = new DefaultOAuthService(mockUserService,
                mockAccessTokenService, mockRefreshTokenService,
                new StubLogger());
    }

    @Test
    public void shouldAssertTokenIsExpired() throws Exception {
        AccessToken accessToken = new AccessToken(tokenVal,
                MemcachedAccessTokenRepository.DATE_PARSER
                        .parseDateTime("20001231210627.300Z"), getFakeUser(), getTestClient(),
                IDM_SCOPE.FULL);
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

        EasyMock.expect(
                mockUserService.authenticateDeprecated(authCredentials.getUsername(), userpass.getValue())).andReturn(true);
        EasyMock.replay(mockUserService);

        EasyMock.expect(
                mockAccessTokenService.getAccessTokenForUser(authCredentials
                        .getUsername(), authCredentials.getClientId(), currentTime))
                .andReturn(testAccessToken);
        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(
                mockRefreshTokenService.getRefreshTokenByUserAndClient(
                        authCredentials.getUsername(), authCredentials.getClientId(),
                        currentTime)).andReturn(null);
        EasyMock.expect(
                mockRefreshTokenService.createRefreshTokenForUser(authCredentials
                        .getUsername(), authCredentials.getClientId())).andReturn(
                testRefreshToken);
        EasyMock.replay(mockRefreshTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldGetTokenWithGrantTypeNone() {

        OAuthGrantType grantType = OAuthGrantType.NONE;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("none");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        DateTime currentTime = new DateTime();

        EasyMock.expect(
                mockAccessTokenService.getAccessTokenForClient(authCredentials
                        .getClientId(), currentTime)).andReturn(testAccessToken);
        EasyMock.replay(mockAccessTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNull(authData.getRefreshToken());

        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldCreateUserAccessTokenWithValidRefreshToken() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        DateTime currentTime = new DateTime();

        EasyMock.expect(
                mockRefreshTokenService
                        .getRefreshTokenByTokenString(refreshTokenVal)).andReturn(
                testRefreshToken);
        mockRefreshTokenService.resetTokenExpiration(testRefreshToken);
        EasyMock.expect(
                mockRefreshTokenService
                        .getRefreshTokenByTokenString(refreshTokenVal)).andReturn(
                testRefreshToken);
        EasyMock.replay(mockRefreshTokenService);

        EasyMock
                .expect(
                        mockAccessTokenService.createAccessTokenForUser(authCredentials
                                .getUsername(), authCredentials.getClientId(),
                                expireInSeconds)).andReturn(testAccessToken);
        EasyMock.replay(mockAccessTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockRefreshTokenService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldCreateNewAccessTokenWhenExpired() throws Exception {

        OAuthGrantType grantType = OAuthGrantType.PASSWORD;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        testAccessToken
                .setExpirationTime(MemcachedAccessTokenRepository.DATE_PARSER
                        .parseDateTime("20000101000000.300Z"));
        DateTime currentTime = new DateTime();

        EasyMock.expect(
                mockUserService.authenticateDeprecated(authCredentials.getUsername(), userpass.getValue())).andReturn(true);
        EasyMock.replay(mockUserService);

        EasyMock.expect(
                mockAccessTokenService.getAccessTokenForUser(authCredentials
                        .getUsername(), authCredentials.getClientId(), currentTime))
                .andReturn(testAccessToken);
        EasyMock
                .expect(
                        mockAccessTokenService.createAccessTokenForUser(authCredentials
                                .getUsername(), authCredentials.getClientId(),
                                expireInSeconds)).andReturn(testAccessToken);
        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(
                mockRefreshTokenService.getRefreshTokenByUserAndClient(
                        authCredentials.getUsername(), authCredentials.getClientId(),
                        currentTime)).andReturn(null);
        EasyMock.expect(
                mockRefreshTokenService.createRefreshTokenForUser(authCredentials
                        .getUsername(), authCredentials.getClientId())).andReturn(
                testRefreshToken);
        EasyMock.replay(mockRefreshTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAccessTokenService);
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
        EasyMock.expect(
                mockUserService.authenticateDeprecated(authCredentials.getUsername(), userpass.getValue())).andReturn(false);
        EasyMock.replay(mockUserService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfInvalidRefreshToken() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        DateTime currentTime = new DateTime();

        EasyMock.expect(
                mockRefreshTokenService
                        .getRefreshTokenByTokenString(refreshTokenVal)).andReturn(null);
        EasyMock.replay(mockRefreshTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockRefreshTokenService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfRefreshTokenIsExpired() {

        OAuthGrantType grantType = OAuthGrantType.REFRESH_TOKEN;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        RefreshToken testRefreshToken = getFakeRefreshToken();
        testRefreshToken
                .setExpirationTime(MemcachedAccessTokenRepository.DATE_PARSER
                        .parseDateTime("20000101000000.300Z"));
        DateTime currentTime = new DateTime();

        EasyMock.expect(
                mockRefreshTokenService
                        .getRefreshTokenByTokenString(refreshTokenVal)).andReturn(
                testRefreshToken);
        EasyMock.replay(mockRefreshTokenService);

        AuthData authData = oauthService.getTokensDeprecated(grantType, authCredentials, expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockRefreshTokenService);
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
                MemcachedAccessTokenRepository.DATE_PARSER
                        .parseDateTime("20201231210627.300Z"), getFakeUser(), getTestClient(),
                IDM_SCOPE.FULL);
    }

    private RefreshToken getFakeRefreshToken() {
        return new RefreshToken(refreshTokenVal, new DateTime()
                .plusSeconds(expireInSeconds), username, clientId);
    }

    private AuthData getFakeAuthData() {
        AccessToken accessToken = getFakeAccessToken();
        RefreshToken refreshToken = getFakeRefreshToken();

        AuthData authData = new AuthData(accessToken, refreshToken);
        return authData;
    }

    private User getFakeUser() {
        User user = new User(username, customerId, useremail,
                new UserHumanName(firstname, "", lastname), new UserLocale(),
                new UserCredential(userpass, "", ""));
        user.setApiKey("1234567890");
        return user;
    }

    private Client getTestClient() {
        Client client = new Client(clientId, ClientSecret
                .newInstance(clientSecret), "DELETE_My_Name", "inum", "iname",
                "RCN-123-456-789", ClientStatus.ACTIVE,
                "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111",
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