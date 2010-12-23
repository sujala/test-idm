package com.rackspace.idm.controllers;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.WebApplicationException;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.authorizationService.AuthorizationRequest;
import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.Entity;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.AuthConverter;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.dao.LdapRefreshTokenRepository;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.exceptions.ApiException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.jaxb.Auth;
import com.rackspace.idm.oauth.AuthCredentials;
import com.rackspace.idm.oauth.OAuthGrantType;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;

public class AccessTokenControllerTests {

    TokenController controller;
    AccessTokenService mockAccessTokenService;
    UserService mockUserService;
    OAuthService mockOAuthService;
    RoleService mockRoleService;
    ClientService mockClientService;

    AuthorizationService mockAuthZService;
    AuthHeaderHelper authHeaderHelper;

    String userFlowType = "user";
    String grantTypeStrVal = "PASSWORD";
    String apiFlowType = "api-credentials";
    String customerId = "123-456-789";
    String clientId = "asdf1234";
    String clientSecret = "secret";
    String username = "testUser";
    String userPassword = "shhhh!";
    String userApiKey = "01234567890";
    String restrictedClientId = "1183ca858a25100bd8bbd68f5f82ebe2ec8dfa87";

    String email = "newuser.com";
    String firstname = "new";
    String lastname = "user";
    String password = "secret";
    Password userpass = Password.newInstance(password);

    String tokenVal = "asdf1234";
    String tokenSecret = "somesecret1234";
    String refreshTokenVal = "somerefreshtoken1234";
    String expiresIn = "3600";
    int expireInSeconds = 3600;
    DateTime currentTime = new DateTime();

    Client testClient = new Client();

    AuthCredentials tokenReqParm;
    com.rackspace.idm.jaxb.AuthCredentials tokenReqParmJaxb;

    @Before
    public void setUp() throws Exception {

        mockAccessTokenService = EasyMock.createMock(AccessTokenService.class);
        mockUserService = EasyMock.createMock(UserService.class);
        mockAuthZService = EasyMock.createMock(AuthorizationService.class);
        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockRoleService = EasyMock.createMock(RoleService.class);
        mockClientService = EasyMock.createMock(ClientService.class);

        authHeaderHelper = new AuthHeaderHelper();
        controller = createTestController();

        tokenReqParm = new AuthCredentials();
        tokenReqParm.setClientId(clientId);
        tokenReqParm.setClientSecret(clientSecret);
        tokenReqParm.setExpirationInSec(expireInSeconds);
        tokenReqParm.setGrantType(grantTypeStrVal);
        tokenReqParm.setPassword(password);
        tokenReqParm.setUsername(username);
        tokenReqParm.setRefreshToken(refreshTokenVal);

        tokenReqParmJaxb = new com.rackspace.idm.jaxb.AuthCredentials();
        tokenReqParmJaxb.setClientId(clientId);
        tokenReqParmJaxb.setClientSecret(clientSecret);
        tokenReqParmJaxb
            .setGrantType(com.rackspace.idm.jaxb.AuthGrantType.PASSWORD);
        tokenReqParmJaxb.setPassword(password);
        tokenReqParmJaxb.setUsername(username);
        tokenReqParmJaxb.setRefreshToken(refreshTokenVal);

        testClient.setClientId(clientId);
        testClient.setStatus(ClientStatus.ACTIVE);
        testClient.setSoftDeleted(false);
    }

    @Test
    public void shouldGetAccessToken() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthData testAuthData = getFakeAuthData();
        OAuthGrantType grantType = OAuthGrantType.valueOf(grantTypeStrVal
            .replace("-", "_").toUpperCase());

        setupGetExpireSeconds();

        EasyMock.expect(
            mockOAuthService.authenticateClient(clientId, clientSecret))
            .andReturn(true);
        EasyMock.expect(
            mockOAuthService.getTokens(EasyMock.eq(grantType), EasyMock
                .anyObject(tokenReqParm.getClass()), EasyMock
                .eq(expireInSeconds), EasyMock.eq(currentTime))).andReturn(
            testAuthData);
        EasyMock.replay(mockOAuthService);

        Auth authData = controller.getAccessToken(response, null,
            tokenReqParmJaxb);

        Assert.assertNotNull(authData);
        EasyMock.verify(mockOAuthService);
    }

    @Test(expected = NotAuthorizedException.class)
    public void shouldNotGetTokenIfFailClientAuthentication() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenReqParm.setClientId(clientId);
        tokenReqParm.setGrantType("none");
        tokenReqParm.setPassword(clientSecret);
        tokenReqParm.setUsername("");
        tokenReqParm.setPassword("");

        setupGetExpireSeconds();

        EasyMock.expect(
            mockOAuthService.authenticateClient(clientId, clientSecret))
            .andReturn(false);
        EasyMock.replay(mockOAuthService);

        controller.getAccessToken(response, null, tokenReqParmJaxb);
    }

    @Test
    public void shouldValidateTokenAndReturnUserDetails() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        String authHeader = "Token token=\"asdf1234\"";
        AccessToken accessToken = getFakeAccessToken();

        EasyMock.expect(mockAccessTokenService.validateToken(tokenVal))
            .andReturn(accessToken);
        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenVal))
            .andReturn(username);
        EasyMock.expect(
            mockAccessTokenService.getClientIdByTokenString(tokenVal))
            .andReturn(clientId);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        User user = getFakeUser();

        user.setPersonId("personId");
        user.setDisplayName("displayName");
        user.setCountry("country");
        user.setStatus(UserStatus.ACTIVE);
        user.setSoftDeleted(false);
        user.setTimeZone("America/Chicago");
        user.setPrefferedLang("en_US");

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Auth retval = controller.validateAccessToken(
            response, authHeader, tokenVal);

        Assert.assertNotNull(retval.getUser());
        Assert.assertNotNull(retval.getAccessToken());
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotValidateTokenIfUsernameEmpty() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        String authHeader = "Token token=\"asdf1234\"";
        AccessToken accessToken = getFakeAccessToken();

        EasyMock.expect(mockAccessTokenService.validateToken(tokenVal))
            .andReturn(accessToken);
        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenVal))
            .andReturn(null);
        EasyMock.expect(
            mockAccessTokenService.getClientIdByTokenString(tokenVal))
            .andReturn(clientId);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        User user = getFakeUser();

        user.setPersonId("personId");
        user.setDisplayName("displayName");
        user.setCountry("country");
        user.setStatus(UserStatus.ACTIVE);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Auth retval = controller.validateAccessToken(
            response, authHeader, tokenVal);

        Assert.assertNotNull(retval.getUser());
        Assert.assertNotNull(retval.getAccessToken());
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotValidateTokenIfUserNotFound() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        String authHeader = "Token token=\"asdf1234\"";
        AccessToken accessToken = getFakeAccessToken();

        EasyMock.expect(mockAccessTokenService.validateToken(tokenVal))
            .andReturn(accessToken);
        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenVal))
            .andReturn(username);
        EasyMock.expect(
            mockAccessTokenService.getClientIdByTokenString(tokenVal))
            .andReturn(clientId);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        User user = getFakeUser();

        user.setPersonId("personId");
        user.setDisplayName("displayName");
        user.setCountry("country");
        user.setStatus(UserStatus.ACTIVE);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(null);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Auth retval = controller.validateAccessToken(
            response, authHeader, tokenVal);

        Assert.assertNotNull(retval.getUser());
        Assert.assertNotNull(retval.getAccessToken());
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = UserDisabledException.class)
    public void shouldNotValidateTokenIfUserHasBadStatus() {

        MockHttpServletResponse response = new MockHttpServletResponse();
        String authHeader = "Token token=\"asdf1234\"";
        AccessToken accessToken = getFakeAccessToken();

        EasyMock.expect(mockAccessTokenService.validateToken(tokenVal))
            .andReturn(accessToken);
        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenVal))
            .andReturn(username);
        EasyMock.expect(
            mockAccessTokenService.getClientIdByTokenString(tokenVal))
            .andReturn(clientId);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        User user = getFakeUser();
        user.setStatus(UserStatus.INACTIVE);

        user.setPersonId("personId");
        user.setDisplayName("displayName");
        user.setCountry("country");

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Auth retval = controller.validateAccessToken(
            response, authHeader, tokenVal);

        Assert.assertNotNull(retval.getUser());
        Assert.assertNotNull(retval.getAccessToken());
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotValidateTokenIfTokenIsExpired() {

        MockHttpServletResponse response = new MockHttpServletResponse();
        String authHeader = "Token token=\"asdf1234\"";
        AccessToken accessToken = getFakeAccessToken();
        accessToken.setExpiration(0);

        EasyMock.expect(mockAccessTokenService.validateToken(tokenVal))
            .andReturn(null);
        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenVal))
            .andReturn(username);
        EasyMock.expect(
            mockAccessTokenService.getClientIdByTokenString(tokenVal))
            .andReturn(clientId);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockClientService.getById(clientId)).andReturn(
            testClient);
        EasyMock.replay(mockClientService);

        User user = getFakeUser();

        user.setPersonId("personId");
        user.setDisplayName("displayName");
        user.setCountry("country");
        user.setStatus(UserStatus.ACTIVE);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Auth retval = controller.validateAccessToken(
            response, authHeader, tokenVal);

        Assert.assertNotNull(retval.getUser());
        Assert.assertNotNull(retval.getAccessToken());
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldRevokeToken() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String authHeader = "OAuth asdf1234";
        String tokenToRevoke = "asdf1234";

        setupRevokeTokenAuthorizationReturnsTrue(authHeader, tokenToRevoke,
            username);

        controller.revokeAccessToken(response, authHeader, tokenToRevoke);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockAuthZService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test
    public void shouldRevokeTokenRackspaceCompanyAuthorized() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String authHeader = "OAuth asdf1234";
        String tokenToRevoke = "asdf1234";

        String rackspace = "Rackspace";

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn(rackspace);

        try {
            setupRevokeTokenAuthorizationReturnsTrue(authHeader, tokenToRevoke,
                null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        controller.revokeAccessToken(response, authHeader, tokenToRevoke);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockAuthZService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockAccessTokenService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotRevokeTokenRackspaceCompanyAuthorized() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String authHeader = "Token token=\"asdf1234\"";
        String tokenToRevoke = "asdf1234";

        String rackspace = "Walmart";

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn(rackspace);

        try {
            setupRevokeTokenAuthorizationReturnsFalse(authHeader,
                tokenToRevoke, null);
            replayServices();

        } catch (Exception e) {

        }

        try {
            controller.revokeAccessToken(response, authHeader, tokenToRevoke);
        } catch (ApiException exp) {
            Assert
                .assertTrue(exp.getResponse().getStatus() == HttpServletResponse.SC_UNAUTHORIZED);
        }
        EasyMock.verify(mockAuthZService);
        EasyMock.verify(mockOAuthService);

    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotRevokeTokenBecauseNotAuthorized() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String authHeader = "Token token=\"asdf1234\"";
        String tokenToRevoke = "asdf1234";

        try {
            AccessToken testtoken = createTestToken(false);
            EasyMock
                .expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
                .andReturn(testtoken);
            setupRevokeTokenAuthorizationReturnsFalse(authHeader,
                tokenToRevoke, "testuser");
            replayServices();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            controller.revokeAccessToken(response, authHeader, tokenVal);
        } catch (WebApplicationException e) {
            Assert.assertEquals(e.getResponse().getStatus(),
                HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    // helpers
    private AccessToken getFakeAccessToken() {
        AccessToken accessToken = new AccessToken(tokenVal,
            MemcachedAccessTokenRepository.DATE_PARSER
                .parseDateTime("20201231210627.300Z"), username, "requestor",
            IDM_SCOPE.FULL);

        return accessToken;
    }

    private RefreshToken getFakeRefreshToken() {

        RefreshToken refreshToken = new RefreshToken(refreshTokenVal,
            LdapRefreshTokenRepository.DATE_PARSER
                .parseDateTime("20201231210627.300Z"), username, clientId);
        return refreshToken;
    }

    private AuthData getFakeAuthData() {
        AccessToken accessToken = getFakeAccessToken();
        RefreshToken refreshToken = getFakeRefreshToken();

        AuthData authData = new AuthData(accessToken, refreshToken);
        return authData;
    }

    private void setupRevokeTokenAuthorizationReturnsTrue(String authHeader,
        String tokenToRevoke, String subjectname) throws Exception {
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(subjectname);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock
            .expect(
                mockAuthZService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        EasyMock.expect(mockAuthZService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenToRevoke))
            .andReturn(username);
        mockAccessTokenService.revokeToken(tokenToRevoke, tokenToRevoke);

        EasyMock.replay(mockAuthZService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockAccessTokenService);
    }

    private void setupRevokeTokenAuthorizationReturnsFalse(String authHeader,
        String tokenToRevoke, String subjectname) throws Exception {
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(subjectname).times(2);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock
            .expect(
                mockAuthZService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        EasyMock.expect(mockAuthZService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.expect(
            mockAccessTokenService.getUsernameByTokenString(tokenToRevoke))
            .andReturn(username);
        mockAccessTokenService.revokeToken(tokenToRevoke, tokenToRevoke);

    }

    private void replayServices() {
        EasyMock.replay(mockAuthZService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockAccessTokenService);
    }

    private User getFakeUser() {
        User user = new User(username, customerId, email, new UserHumanName(
            firstname, "", lastname), new UserLocale(), new UserCredential(
            userpass, "", ""));

        return user;
    }

    private AccessToken createTestToken(boolean isTrusted) {
        String tokenString = "asdf1234";
        DateTime expiration = new DateTime().plusHours(1);
        AccessToken testToken = new AccessToken(tokenString, expiration,
            username, username, IDM_SCOPE.FULL, isTrusted);
        return testToken;
    }

    private void setupGetExpireSeconds() {
        EasyMock.expect(
            mockAccessTokenService.getDefaultTokenExpirationSeconds())
            .andReturn(3600);
        EasyMock.expect(mockAccessTokenService.getMaxTokenExpirationSeconds())
            .andReturn(86400);
        EasyMock.expect(mockAccessTokenService.getMinTokenExpirationSeconds())
            .andReturn(10);
        EasyMock.replay(mockAccessTokenService);
    }

    private TokenController createTestController() {

        Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

        PermissionConverter permissionConverter = new PermissionConverter();
        RoleConverter roleConverter = new RoleConverter(permissionConverter);
        UserConverter userConverter = new UserConverter(roleConverter);
        ClientConverter clientConverter = new ClientConverter(
            permissionConverter);
        TokenConverter tokenConverter = new TokenConverter();
        AuthConverter authConverter = new AuthConverter(tokenConverter,
            permissionConverter, clientConverter, userConverter);

        TokenController controller = new TestTokenController(
            mockAccessTokenService, mockUserService, mockClientService,
            mockAuthZService, mockOAuthService, authHeaderHelper,
            new IDMAuthorizationHelper(mockOAuthService, mockAuthZService,
                mockRoleService, mockClientService, LoggerFactory
                    .getLogger(IDMAuthorizationHelper.class)),
            new InputValidator(validator), authConverter,
            new LoggerFactoryWrapper());

        return controller;
    }

    public class TestTokenController extends TokenController {
        public TestTokenController(AccessTokenService tokenService,
            UserService userService, ClientService clientService,
            AuthorizationService authorizationService,
            OAuthService oauthService, AuthHeaderHelper authHeaderHelper,
            IDMAuthorizationHelper idmAuthHelper,
            InputValidator inputValidator, AuthConverter authConverter,
            LoggerFactoryWrapper logger) {

            super(tokenService, userService, clientService,
                authorizationService, oauthService, authHeaderHelper,
                idmAuthHelper, inputValidator, authConverter, logger);
        }

        @Override
        protected DateTime getCurrentTime() {
            return currentTime;
        }
    }
}