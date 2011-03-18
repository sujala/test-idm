package com.rackspace.idm.services;

import java.util.Locale;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.RefreshTokenDao;
import com.rackspace.idm.domain.dao.XdcAccessTokenDao;
import com.rackspace.idm.domain.dao.impl.MemcachedAccessTokenRepository;
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.TokenDefaultAttributes;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.CustomerService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultAccessTokenService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.util.AuthHeaderHelper;

public class AccessTokenServiceTests {

    AccessTokenDao mockTokenDao;
    RefreshTokenDao mockRefreshTokenDao;
    ClientDao mockClientDao;
    UserService mockUserService;
    CustomerService mockCustomerService;
    AccessTokenService tokenService;
    XdcAccessTokenDao mockWebClientAccessTokenDao;

    TokenDefaultAttributes defaultAttributes;

    String username = "mkovacs";
    String tokenString = "XXXXXX";
    String authHeader = "OAuth " + tokenString;
    DateTime tokenExpiration = MemcachedAccessTokenRepository.DATE_PARSER.parseDateTime("20201231210627.3Z");
    String tokenOwner = "mkovacs";
    String clientId = "SomeClientId";
    String clientDn = "inum=@!FFFF.FFFF.FFFF.FFFF!DDDD.DDDD,ou=Applications,dc=rackspace,dc=com";
    String clientSecret = "secret";
    String clientName = "testClient";

    String customerId = "123456";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";

    String middlename = "middle";
    String secretQuestion = "question";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    String userInum = "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111";
    String clientInum = "@!FFFF.FFFF.FFFF.FFFF!DDDD.DDDD";
    String clientIname = "@Rackspace.TestClient";

    int defaultTokenExpirationSeconds = 3600;
    int cloudAuthExpirationSeconds = 86400;
    int maxTokenExpirationSeconds = 86400;
    int minTokenExpirationSeconds = 10;
    String dataCenterPrefix = "DEV";
    boolean isTrustedServer = false;

    @Before
    public void setUp() throws Exception {

        mockTokenDao = EasyMock.createMock(AccessTokenDao.class);
        mockRefreshTokenDao = EasyMock.createMock(RefreshTokenDao.class);
        mockClientDao = EasyMock.createMock(ClientDao.class);
        mockUserService = EasyMock.createMock(UserService.class);
        mockCustomerService = EasyMock.createMock(CustomerService.class);
        mockWebClientAccessTokenDao = EasyMock.createMock(XdcAccessTokenDao.class);
        defaultAttributes = new TokenDefaultAttributes(defaultTokenExpirationSeconds,
            cloudAuthExpirationSeconds, maxTokenExpirationSeconds, minTokenExpirationSeconds,
            dataCenterPrefix, isTrustedServer);
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        tokenService = new DefaultAccessTokenService(mockTokenDao, mockClientDao, mockUserService, mockCustomerService,
            mockWebClientAccessTokenDao, new AuthHeaderHelper(), appConfig);
    }

    @Test
    public void shouldGetTokenByTokenString() {

        AccessToken token = getFakeUserToken();
        EasyMock.expect(mockTokenDao.findByTokenString(tokenString)).andReturn(token);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenByTokenString(tokenString);

        Assert.assertTrue(retrievedToken.getTokenString().equals(tokenString));
        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldReturnNullForNonExistentTokenString() {
        EasyMock.expect(mockTokenDao.findByTokenString(tokenString)).andReturn(null);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenByTokenString(tokenString);

        Assert.assertTrue(retrievedToken == null);
        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldCreateAccessTokenForClient() {
        AccessToken token = getFakeClientToken();
        Client client = getFakeClient();

        mockTokenDao.save(token);

        AccessToken returnToken = tokenService.createAccessTokenForClient(client);

        Assert.assertTrue(returnToken.getOwner().equals(clientId));

        String tokenPrefix = returnToken.getTokenString().substring(0, 3);
        Assert.assertTrue(tokenPrefix.equals(dataCenterPrefix));
    }

    @Test
    public void shouldCreateAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(getFakeUser());
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(getFakeClient());
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        AccessToken returnToken = tokenService.createAccessTokenForUser(username, clientId);

        Assert.assertTrue(returnToken.getOwner().equals(username));

        String tokenPrefix = returnToken.getTokenString().substring(0, 3);
        Assert.assertTrue(tokenPrefix.equals(dataCenterPrefix));

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldCreateRestricedAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        token.setRestrictedToSetPassword();

        User user = getFakeUser();

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(getFakeClient());
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        AccessToken returnToken = tokenService.createPasswordResetAccessTokenForUser(user, clientId);

        Assert.assertTrue(returnToken.getOwner().equals(tokenOwner));
        EasyMock.verify(mockClientDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorForOwnerNullCreateRestricedAccessTokenForUser() {
        User nouser = null;
        EasyMock.expect(mockUserService.getUser(username)).andReturn(nouser);
        EasyMock.replay(mockUserService);

        tokenService.createPasswordResetAccessTokenForUser(nouser, clientId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowErrorForOwneUserNullCreateRestricedAccessTokenForUser() {
        User user = getFakeUser();
        user.setUsername("");
        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        tokenService.createPasswordResetAccessTokenForUser(user, clientId);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorForClientNullCreateRestricedAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        User user = getFakeUser();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        tokenService.createPasswordResetAccessTokenForUser(user, clientId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowErrorForClientInumNullCreateRestricedAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(getFakeUser());
        EasyMock.replay(mockUserService);

        Client client = getFakeClient();
        client.setInum("");
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        tokenService.createPasswordResetAccessTokenForUser(getFakeUser(), clientId);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorForOwnerNullCreateAccessTokenForUser() {
        EasyMock.expect(mockUserService.getUser(username)).andReturn(null);
        EasyMock.replay(mockUserService);

        tokenService.createAccessTokenForUser(username, clientId);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorForClientNullCreateAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(getFakeUser());
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        tokenService.createAccessTokenForUser(username, clientId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowErrorForClientIdNullCreateAccessTokenForUser() {
        AccessToken token = getFakeUserToken();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(getFakeUser());
        EasyMock.replay(mockUserService);

        Client client = getFakeClient();
        client.setClientId("");
        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(client);
        EasyMock.replay(mockClientDao);

        mockTokenDao.save(token);

        tokenService.createAccessTokenForUser(username, clientId);
    }

    @Test
    public void shouldGetTokenForUser() {
        AccessToken token = getFakeUserToken();
        User user = getFakeUser();
        DateTime current = new DateTime();

        EasyMock.expect(mockTokenDao.findTokenForOwner(username, clientId)).andReturn(token);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenForUser(user, getFakeClient(), current);
        Assert.assertTrue(retrievedToken.getTokenString().equals(tokenString));
        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldNotGetTokenForUser() {
        User user = getFakeUser();
        DateTime current = new DateTime();

        EasyMock.expect(mockTokenDao.findTokenForOwner(username, clientId)).andReturn(null);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenForUser(user, getFakeClient(), current);
        Assert.assertNull(retrievedToken);
        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldGetTokenForClient() {
        AccessToken token = getFakeClientToken();
        Client client = getFakeClient();
        DateTime current = new DateTime();

        EasyMock.expect(mockTokenDao.findTokenForOwner(EasyMock.eq(clientId), EasyMock.eq(clientId)))
            .andReturn(token);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenForClient(client, current);
        Assert.assertTrue(retrievedToken.getTokenString().equals(tokenString));
        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldNotGetTokenForClient() {
        DateTime current = new DateTime();

        EasyMock.expect(mockTokenDao.findTokenForOwner(EasyMock.eq(clientId), EasyMock.eq(clientId)))
            .andReturn(null);
        EasyMock.replay(mockTokenDao);

        AccessToken retrievedToken = tokenService.getAccessTokenForClient(getFakeClient(), current);
        Assert.assertNull(retrievedToken);
        EasyMock.verify(mockTokenDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThowErrorForGetTokenIfUserIsNull() {
        DateTime current = new DateTime();

        EasyMock.expect(mockUserService.getUser(username)).andReturn(null);
        EasyMock.replay(mockUserService);

        tokenService.getAccessTokenForUser(null, getFakeClient(), current);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThowErrorForGetTokenIfUserIsNotNullButClientIsNull() {
        User user = getFakeUser();
        DateTime current = new DateTime();

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        tokenService.getAccessTokenForUser(user, null, current);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowErrorForGetTokenIfClientIsNull() {
        DateTime current = new DateTime();

        EasyMock.expect(mockClientDao.findByClientId(clientId)).andReturn(null);
        EasyMock.replay(mockClientDao);

        AccessToken retrievedToken = tokenService.getAccessTokenForClient(null, current);
        Assert.assertNull(retrievedToken);
        EasyMock.verify(mockClientDao);
    }

    @Test
    public void shouldValidateToken() {
        AccessToken userToken = getFakeUserToken();
        EasyMock.expect(mockTokenDao.findByTokenString(userToken.getTokenString())).andReturn(userToken);
        EasyMock.replay(mockTokenDao);
        AccessToken token = tokenService.validateToken(tokenString);

        Assert.assertTrue(token.getExpiration() >= 3599);
    }

    @Test
    public void shouldNotValidateNonExistentToken() {
        AccessToken userToken = getFakeUserToken();
        EasyMock.expect(mockTokenDao.findByTokenString(userToken.getTokenString())).andReturn(null);
        EasyMock.replay(mockTokenDao);

        AccessToken token = tokenService.validateToken(tokenString);

        Assert.assertNull(token);

        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldNotValidateExpiredToken() {
        AccessToken userToken = getFakeUserToken();
        userToken.setExpiration(-3600);

        EasyMock.expect(mockTokenDao.findByTokenString(userToken.getTokenString())).andReturn(userToken);
        EasyMock.replay(mockTokenDao);

        AccessToken token = tokenService.validateToken(tokenString);

        Assert.assertNull(token);

        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldAuthenticateHeaderWithFlowTypeToken() {
        EasyMock.expect(mockTokenDao.findByTokenString(tokenString)).andReturn(getFakeUserToken());
        EasyMock.replay(mockTokenDao);

        AccessToken token = tokenService.getAccessTokenByAuthHeader(authHeader);
        Assert.assertNotNull(token);

        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldAuthenticateToken() throws Exception {
        EasyMock.expect(mockTokenDao.findByTokenString(tokenString)).andReturn(getFakeUserToken());
        EasyMock.replay(mockTokenDao);
        
        Customer customer = new Customer();
        customer.setPasswordRotationEnabled(false);
        
        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(customer);
        EasyMock.replay(mockCustomerService);
        
        User user = getFakeUser();
        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);
        EasyMock.replay(mockUserService);

        boolean isAuthenticated = tokenService.authenticateAccessToken(tokenString);
        Assert.assertTrue(isAuthenticated);

        EasyMock.verify(mockTokenDao);
    }

    @Test
    public void shouldNotAuthenticateTokenThatDoesNotExist() throws Exception {
        String badToken = "badtoken";

        EasyMock.expect(mockTokenDao.findByTokenString(badToken)).andReturn(null);
        EasyMock.replay(mockTokenDao);

        boolean isAuthenticated = tokenService.authenticateAccessToken(badToken);
        Assert.assertFalse(isAuthenticated);

        EasyMock.verify(mockTokenDao);
    }

    private AccessToken getFakeUserToken() {
        return new AccessToken(tokenString, tokenExpiration, getFakeUser(), getFakeClient(), IDM_SCOPE.FULL);
    }

    private AccessToken getFakeClientToken() {
        return new AccessToken(tokenString, tokenExpiration, null, getFakeClient(), IDM_SCOPE.FULL);
    }

    private User getFakeUser() {

        UserHumanName name = new UserHumanName(firstname, middlename, lastname);
        UserLocale pref = new UserLocale(new Locale(preferredLang), DateTimeZone.forID(timeZone));
        UserCredential cred = new UserCredential(userpass, secretQuestion, secretAnswer);
        User user = new User(username, customerId, email, name, pref, cred);
        user.setInum(userInum);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Client getFakeClient() {
        Client client = new Client(clientId, ClientSecret.newInstance(clientSecret), clientName, clientInum,
            clientIname, "RCN-123-456-789", ClientStatus.ACTIVE);
        return client;
    }
}
