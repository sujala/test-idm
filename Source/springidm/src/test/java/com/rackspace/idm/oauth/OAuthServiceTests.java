package com.rackspace.idm.oauth;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RefreshTokenService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;

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
    String tokenSecret = "somesecret1234";
    String refreshTokenVal = "somerefreshtoken1234";
    int expireInSeconds = 3600;

    @Before
    public void setUp() throws Exception {

        mockUserService = EasyMock.createMock(UserService.class);
        mockClientService = EasyMock.createMock(ClientService.class);
        mockAccessTokenService = EasyMock.createMock(AccessTokenService.class);
        mockRefreshTokenService = EasyMock
            .createMock(RefreshTokenService.class);

        authHeaderHelper = new AuthHeaderHelper();

        oauthService = new DefaultOAuthService(mockUserService,
            mockClientService, mockAccessTokenService, mockRefreshTokenService,
            authHeaderHelper, new StubLogger());
    }

    @Test
    public void shouldAuthenticateHeaderWithFlowTypeToken() throws Exception {

        String authHeader = String.format("OAuth %s", tokenVal);
        AccessToken accessToken = getFakeAccessToken();

        EasyMock.expect(
            mockAccessTokenService.getTokenByTokenString(accessToken
                .getTokenString())).andReturn(accessToken);
        EasyMock.replay(mockAccessTokenService);

        Boolean authenticated = oauthService.authenticateAuthHeader(authHeader);

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldNotAuthenticateWithBadHeader() throws Exception {

        String authHeader = "client_credentials client_id=10eeaa768d39579eca73b2a21fb721b2453d6790,client_secret=dd0725319e2b635a37f895ec6823a9fddda120b9";
        Boolean authenticated = oauthService.authenticateAuthHeader(authHeader);

        Assert.assertTrue(!authenticated);
    }

    @Test
    public void shouldAuthenticateClient() throws Exception {
        Client client = getTestClient();

        EasyMock.expect(
            mockClientService.authenticate(client.getClientId(), client
                .getClientSecretObj().getValue())).andReturn(true);
        EasyMock.replay(mockClientService);

        Boolean authenticated = oauthService.authenticateClient(client
            .getClientId(), client.getClientSecretObj().getValue());

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockClientService);
    }

    @Test
    public void shouldNotAuthenticateClientThatDoesNotExist() throws Exception {
        String badKey = "---";
        EasyMock.expect(mockClientService.authenticate(badKey, "asdf"))
            .andReturn(false);
        EasyMock.replay(mockClientService);

        Boolean authenticated = oauthService.authenticateClient(badKey, "asdf");

        Assert.assertTrue(!authenticated);
        EasyMock.verify(mockClientService);
    }

    @Test
    public void shouldNotAuthenticateClientWithBadSecret() throws Exception {
        Client client = getTestClient();
        client.setClientSecretObj(ClientSecret.newInstance("good_secret"));

        EasyMock.expect(
            mockClientService.authenticate(client.getClientId(), "badsecret"))
            .andReturn(false);
        EasyMock.replay(mockClientService);

        Boolean authenticated = oauthService.authenticateClient(client
            .getClientId(), "badsecret");

        Assert.assertTrue(!authenticated);
        EasyMock.verify(mockClientService);
    }

    @Test
    public void shouldAuthenticateUser() throws Exception {
        User user = getFakeUser();
        EasyMock.expect(
            mockUserService.authenticate(username, user.getPasswordObj()
                .getValue())).andReturn(true);
        EasyMock.replay(mockUserService);

        Boolean authenticated = oauthService.authenticateUser(username,
            userpass.getValue());

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldAuthenticateUserByApidKey() throws Exception {

        User user = getFakeUser();

        EasyMock.expect(
            mockUserService.authenticateWithApiKey(username, user.getApiKey()))
            .andReturn(true);
        EasyMock.replay(mockUserService);

        Boolean authenticated = oauthService.authenticateUserApiKey(username,
            user.getApiKey());

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shoudNotAuthenticateUserWithBadPassword() throws Exception {
        EasyMock.expect(mockUserService.authenticate(username, "badpassword"))
            .andReturn(false);
        EasyMock.replay(mockUserService);

        Boolean authenticated = oauthService.authenticateUser(username,
            "badpassword");

        Assert.assertTrue(!authenticated);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldAuthenticateToken() throws Exception {
        AccessToken accessToken = getFakeAccessToken();
        EasyMock.expect(
            mockAccessTokenService.getTokenByTokenString(accessToken
                .getTokenString())).andReturn(accessToken);
        EasyMock.replay(mockAccessTokenService);

        Boolean authenticated = oauthService.authenticateToken(tokenVal);

        Assert.assertTrue(authenticated);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldNotAuthenticateTokenThatDoesNotExist() throws Exception {
        String badToken = "badtoken";

        EasyMock.expect(mockAccessTokenService.getTokenByTokenString(badToken))
            .andReturn(null);
        EasyMock.replay(mockAccessTokenService);

        Boolean authenticated = oauthService.authenticateToken(badToken);

        Assert.assertTrue(!authenticated);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldAssertTokenIsExpired() throws Exception {
        AccessToken accessToken = new AccessToken(tokenVal,
            MemcachedAccessTokenRepository.DATE_PARSER
                .parseDateTime("20001231210627.300Z"), username, "requestor",
            IDM_SCOPE.FULL);
        Assert.assertTrue(accessToken.isExpired(new DateTime()));
    }

    @Test
    public void shouldPassAuthenticationWithNotExpiredToken() throws Exception {
        AccessToken accessToken = getFakeAccessToken();
        Assert.assertTrue(!accessToken.isExpired(new DateTime()));
    }

    @Test
    public void shouldGetTokenWithGrantTypeApiCredentials() {

        OAuthGrantType grantType = OAuthGrantType.API_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("api-credentials");
        authCredentials.setClientId(GlobalConstants.RESTRICTED_CLIENT_ID);

        AccessToken testAccessToken = getFakeAccessToken();
        RefreshToken testRefreshToken = getFakeRefreshToken();
        DateTime currentTime = new DateTime();

        EasyMock.expect(
            mockUserService.authenticateWithApiKey(authCredentials
                .getUsername(), userpass.getValue())).andReturn(true);
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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAccessTokenService);
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
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(true);
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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

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
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(true);
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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldCreateNewAccessTokenWhenExpiredWithApiKey()
        throws Exception {

        OAuthGrantType grantType = OAuthGrantType.API_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("api-credentials");
        authCredentials.setClientId(GlobalConstants.RESTRICTED_CLIENT_ID);

        RefreshToken testRefreshToken = getFakeRefreshToken();
        AccessToken testAccessToken = getFakeAccessToken();
        testAccessToken
            .setExpirationTime(MemcachedAccessTokenRepository.DATE_PARSER
                .parseDateTime("20000101000000.300Z"));
        DateTime currentTime = new DateTime();

        EasyMock.expect(
            mockUserService.authenticateWithApiKey(authCredentials
                .getUsername(), userpass.getValue())).andReturn(true);
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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfFailClientAuthWithGrantTypeApiCred()
        throws Exception {

        OAuthGrantType grantType = OAuthGrantType.API_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("api-credentials");
        authCredentials.setClientId(GlobalConstants.RESTRICTED_CLIENT_ID);

        DateTime currentTime = new DateTime();

        EasyMock.expect(
            mockUserService.authenticateWithApiKey(authCredentials
                .getUsername(), userpass.getValue())).andReturn(false);
        EasyMock.replay(mockUserService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfNotRestrictedClient() throws Exception {

        OAuthGrantType grantType = OAuthGrantType.API_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("api-credentials");
        authCredentials.setClientId("Not_restricted_client");

        DateTime currentTime = new DateTime();
        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNull(authData);
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
            mockUserService.authenticate(authCredentials.getUsername(),
                userpass.getValue())).andReturn(false);
        EasyMock.replay(mockUserService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfFailUserAuthenticationByApiKey()
        throws Exception {

        OAuthGrantType grantType = OAuthGrantType.API_CREDENTIALS;
        AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("api-credentials");
        authCredentials.setClientId(GlobalConstants.RESTRICTED_CLIENT_ID);

        DateTime currentTime = new DateTime();

        EasyMock.expect(
            mockUserService.authenticateWithApiKey(authCredentials
                .getUsername(), userpass.getValue())).andReturn(false);
        EasyMock.replay(mockUserService);

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

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

        AuthData authData = oauthService.getTokens(grantType, authCredentials,
            expireInSeconds, currentTime);

        Assert.assertNull(authData);
        EasyMock.verify(mockRefreshTokenService);
    }

    // helpers
    private AccessToken getFakeAccessToken() {
        return new AccessToken(tokenVal,
            MemcachedAccessTokenRepository.DATE_PARSER
                .parseDateTime("20201231210627.300Z"), username, "requestor",
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