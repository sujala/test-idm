package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;

public class AuthenticationServiceTests {

    UserService mockUserService;
    ApplicationService mockClientService;
    AuthorizationService mockAuthorizationService;
    AuthHeaderHelper authHeaderHelper;
    InputValidator inputValidator;
    ScopeAccessService mockScopeAccessService;
    UserDao mockUserDao;
    TenantService mockTenantService;
    AuthenticationService authenticationService;
    DefaultAuthenticationService authSpy;
    TokenService mockTokenService;
    ApplicationDao mockApplicationDao;
    AuthDao mockAuthDao;
    CustomerDao mockCustomerDao;
    List<TenantRole> tenantRoles;

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
        mockAuthorizationService = EasyMock.createNiceMock(AuthorizationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        authHeaderHelper = new AuthHeaderHelper();
        inputValidator = EasyMock.createMock(InputValidator.class);
        mockUserDao = EasyMock.createMock(UserDao.class);
        mockTenantService = mock(TenantService.class);
        mockTokenService = EasyMock.createMock(TokenService.class);
        mockApplicationDao = EasyMock.createMock(ApplicationDao.class);
        mockAuthDao = EasyMock.createMock(AuthDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);


        tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.add(new TenantRole());

        final Configuration appConfig = new PropertiesConfiguration();
        appConfig.addProperty("token.expirationSeconds", expireInSeconds);
        appConfig.addProperty("rackspace.customerId", "RACKSPACE");
        appConfig.addProperty("ldap.server.trusted", "true");
        appConfig.addProperty("idm.clientId", "TESTING");

        authenticationService = new DefaultAuthenticationService(
                mockTokenService, mockAuthDao, mockTenantService,
                mockScopeAccessService, mockApplicationDao, appConfig,
                mockUserDao, mockCustomerDao, inputValidator);
        authSpy = PowerMockito.spy(new DefaultAuthenticationService(
                mockTokenService, mockAuthDao, mockTenantService,
                mockScopeAccessService, mockApplicationDao, appConfig,
                mockUserDao, mockCustomerDao, inputValidator));
    }

    @Test
    public void shouldAuthenticateWithPasswordGrantType() {
        //setup
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        final User user = getFakeUser();
        final Application testClient = getTestClient();
        final UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockUserDao.authenticate(authCredentials.getUsername(), userpass.getValue())).andReturn(uaResult);
        EasyMock.expect(mockUserDao.getUserByUsername(authCredentials.getUsername())).andReturn(null);
        EasyMock.expect(mockScopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), testClient.getClientId())).andReturn(getFakeUserScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        final AuthData authData = authenticationService.authenticate(authCredentials);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getUser());

        EasyMock.verify(mockScopeAccessService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfFailUserAuthentication()
            throws Exception {

        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        final User testuser = getFakeUser();
        final Application testClient = getTestClient();
        final UserAuthenticationResult uaResult = new UserAuthenticationResult(testuser, false);
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockUserService.getUser(authCredentials.getUsername())).andReturn(testuser);
        EasyMock.expect(mockUserDao.authenticate(authCredentials.getUsername(), userpass.getValue())).andReturn(uaResult);

        EasyMock.replay(mockApplicationDao, mockUserDao);

        authenticationService.authenticate(authCredentials);
    }

    @Test
    public void shouldAuthenticateWithPasswordGrantType_AndDisplayDaysTillPasswordExpiration() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        final User user = getFakeUser();
        user.setPasswordObj(Password.existingInstance(userpass.getValue(), new DateTime().minusDays(5), false));

        final Customer customer = getFakeCustomer();
        customer.setPasswordRotationDuration(10);

        final Application testClient = getTestClient();
        final UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockUserDao.authenticate(authCredentials.getUsername(), userpass.getValue())).andReturn(uaResult);
        EasyMock.expect(mockUserDao.getUserByUsername(authCredentials.getUsername())).andReturn(user);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(customer);
        EasyMock.expect(mockScopeAccessService.getUserScopeAccessForClientId(user.getUniqueId(), testClient.getClientId())).andReturn(getFakeUserScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService, mockCustomerDao);

        final AuthData authData = authenticationService.authenticate(authCredentials);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getUser());
        Assert.assertNotNull(authData.getPasswordExpirationDate());
        Assert.assertEquals(5, authData.getDaysUntilPasswordExpiration());
        EasyMock.verify(mockScopeAccessService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTokenWithInvalidGrantType() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("none");

        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(getTestClient(), true);
        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getUserScopeAccessForClientId(uniqueId, clientId)).andReturn(getFakeUserScopeAccess());

        EasyMock.replay(mockApplicationDao, mockScopeAccessService);

        authenticationService.authenticate(authCredentials);
    }

    @Test
    public void shouldAuthenticateWithClientCredentialsGrantType() {
        //setup
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("client-credentials");

        final User user = getFakeUser();
        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getClientScopeAccessForClientId(user.getUniqueId(), testClient.getClientId())).andReturn(getFakeClientScopeAccess());
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        final AuthData authData = authenticationService.authenticate(authCredentials);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getApplication());

        EasyMock.verify(mockScopeAccessService);
    }

    @Test
    public void shouldGetPasswordResetTokenWhenPasswordRotationDurationHasExpired() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("password");

        final User user = getFakeUser();
        user.setPasswordObj(Password.existingInstance(userpass.getValue(), new DateTime().minusDays(5), false));

        final Customer customer = getFakeCustomer();
        final Application testClient = getTestClient();
        final UserAuthenticationResult uaResult = new UserAuthenticationResult(user, true);
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockUserDao.authenticate(authCredentials.getUsername(), userpass.getValue())).andReturn(uaResult);
        EasyMock.expect(mockUserDao.getUserByUsername(authCredentials.getUsername())).andReturn(user);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId)).andReturn(customer);
        EasyMock.expect(mockScopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user)).andReturn(getFakePasswordResetScopeAccess());

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService, mockCustomerDao);

        final AuthData authData = authenticationService.authenticate(authCredentials);

        Assert.assertNotNull(authData);
        Assert.assertNull(authData.getRefreshToken());
        Assert.assertNull(authData.getUser());
        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertTrue(authData.isPasswordResetOnlyToken());
        Assert.assertNotNull(authData.getPasswordExpirationDate());
        Assert.assertEquals(0, authData.getDaysUntilPasswordExpiration());
    }


    @Test
    public void shouldCreateUserAccessTokenWithValidRefreshToken() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);
        final User user = getFakeUser();

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(getFakeUserScopeAccess());
        EasyMock.expect(mockUserDao.getUserById(username)).andReturn(user);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        final AuthData authData = authenticationService.authenticate(authCredentials);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getUser());

        EasyMock.verify(mockScopeAccessService);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfInvalidRefreshToken() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);
        final User user = getFakeUser();

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(null);
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        authenticationService.authenticate(authCredentials);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotGetAccessTokenIfRefreshTokenIsExpired() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        final UserScopeAccess usa = getFakeUserScopeAccess();
        usa.setRefreshTokenExp(new DateTime().minusDays(1).toDate());

        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);
        final User user = getFakeUser();

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(usa);
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        authenticationService.authenticate(authCredentials);
    }

    @Test(expected = UserDisabledException.class)
    public void shouldNotGetAccessTokenIfUserDisabled() {
        final AuthCredentials authCredentials = getTestAuthCredentials();
        authCredentials.setGrantType("refresh-token");

        final User user = getFakeUser();
        user.setEnabled(false);

        final UserScopeAccess usa = getFakeUserScopeAccess();
        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockScopeAccessService.getScopeAccessByRefreshToken(refreshTokenVal)).andReturn(usa);
        EasyMock.expect(mockUserDao.getUserById(username)).andReturn(user);
        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));

        EasyMock.replay(mockApplicationDao, mockUserDao, mockScopeAccessService);

        authenticationService.authenticate(authCredentials);
    }

    @Test
    @PrepareForTest(AuthenticationService.class)
    public void shouldAuthenticateAsRacker() throws Exception {
        //setup
        final RackerCredentials authCredentials = getTestRackerCredentials();
        authCredentials.setGrantType("password");

        final Racker racker = getFakeRacker();
        final Application testClient = getTestClient();
        final ClientAuthenticationResult caResult = new ClientAuthenticationResult(testClient, true);

        EasyMock.expect(mockApplicationDao.authenticate(clientId, clientSecret)).andReturn(caResult);
        EasyMock.expect(mockAuthDao.authenticate(authCredentials.getUsername(), userpass.getValue())).andReturn(true);
        EasyMock.expect(mockUserDao.getRackerByRackerId(authCredentials.getUsername())).andReturn(racker);
        EasyMock.expect(mockScopeAccessService.getRackerScopeAccessForClientId(racker.getUniqueId(), testClient.getClientId())).andReturn(getFakeRackerScopeAcces());

        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("Racker");
        tenantRole.setClientId(testClient.getClientId());
        List<TenantRole> testTenantRoles = new ArrayList<TenantRole>();
        testTenantRoles.add(tenantRole);
        ClientRole clientRole = new ClientRole();
        clientRole.setId("5");
        clientRole.setName("Racker");
        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(clientRole);
        PowerMockito.when(mockTenantService.getTenantRolesForScopeAccess(Matchers.<ScopeAccess>anyObject())).thenReturn(testTenantRoles);
        PowerMockito.doNothing().when(mockTenantService).addTenantRoleToUser(any(User.class),any(TenantRole.class));
        EasyMock.expect(mockApplicationDao.getClientRolesByClientId("TESTING")).andReturn(clientRoles);

        mockScopeAccessService.updateScopeAccess(EasyMock.anyObject(ScopeAccess.class));
        EasyMock.replay(mockApplicationDao, mockUserDao, mockAuthDao, mockScopeAccessService);
        final AuthData authData = authSpy.authenticate(authCredentials);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getRacker());

        EasyMock.verify(mockScopeAccessService);
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToUser() {
        //setup
        final ScopeAccess scopeAccess = getFakeUserScopeAccess();

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        PowerMockito.when(mockTenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoles);

        EasyMock.replay(mockScopeAccessService);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getUser());
        Assert.assertEquals(tenantRoles, authData.getUser().getRoles());
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToApplication() {
        //setup
        final ScopeAccess scopeAccess = getFakeClientScopeAccess();

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        PowerMockito.when(mockTenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoles);

        EasyMock.replay(mockScopeAccessService);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getApplication());
        Assert.assertEquals(tenantRoles, authData.getApplication().getRoles());
    }

    @Test
    public void shouldGetAuthDataFromToken_WhenTokenBelongsToRacker() {
        //setup
        final ScopeAccess scopeAccess = getFakeRackerScopeAcces();
        final List<String> rackerRoles = Arrays.asList("role1", "role2");

        EasyMock.expect(mockScopeAccessService.loadScopeAccessByAccessToken(tokenVal)).andReturn(scopeAccess);
        EasyMock.expect(mockAuthDao.getRackerRoles(rackerId)).andReturn(rackerRoles);

        EasyMock.replay(mockScopeAccessService, mockAuthDao);

        final AuthData authData = authenticationService.getAuthDataFromToken(tokenVal);

        Assert.assertNotNull(authData.getAccessToken());
        Assert.assertNotNull(authData.getRefreshToken());
        Assert.assertNotNull(authData.getRacker());
        Assert.assertEquals(rackerRoles, authData.getRacker().getRackerRoles());
    }

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
        usa.setUserRsId(username);
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

    private RackerScopeAccess getFakeRackerScopeAcces() {
        final RackerScopeAccess csa = new RackerScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setRefreshTokenString(refreshTokenVal);
        csa.setRefreshTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setRackerId(rackerId);
        return csa;
    }

    private PasswordResetScopeAccess getFakePasswordResetScopeAccess() {
        final PasswordResetScopeAccess csa = new PasswordResetScopeAccess();
        csa.setAccessTokenString(tokenVal);
        csa.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        csa.setClientId(clientId);
        csa.setClientRCN(customerId);
        csa.setUsername(username);
        csa.setUserRCN(customerId);
        return csa;
    }

    private User getFakeUser() {
        final User user = new User(username, customerId, useremail,
                new UserHumanName(firstname, "", lastname), new UserLocale(),
                new UserCredential(userpass, "", ""));
        user.setApiKey("1234567890");
        user.setUniqueId(uniqueId);
        user.setEnabled(true);
        return user;
    }

    private Racker getFakeRacker() {
        Racker racker = new Racker();
        racker.setUniqueId(uniqueId);
        racker.setUsername(username);
        racker.setEnabled(true);
        return racker;
    }

    private Customer getFakeCustomer() {
        final Customer customer = new Customer();
        customer.setRCN(customerId);
        customer.setPasswordRotationEnabled(true);
        customer.setPasswordRotationDuration(2);
        return customer;
    }

    private Application getTestClient() {
        final Application client = new Application(clientId, ClientSecret
                .newInstance(clientSecret), "DELETE_My_Name", customerId,
                ClientStatus.ACTIVE);
        client.setUniqueId(uniqueId);
        return client;
    }

    private AuthCredentials getTestAuthCredentials() {
        final AuthCredentials authCredentials = new AuthCredentials();
        authCredentials.setClientId(clientId);
        authCredentials.setClientSecret(clientSecret);
        authCredentials.setExpirationInSec(expireInSeconds);
        authCredentials.setGrantType("NONE");
        authCredentials.setUsername(username);
        authCredentials.setPassword(userpass.getValue());
        authCredentials.setRefreshToken(refreshTokenVal);

        return authCredentials;
    }

    private RackerCredentials getTestRackerCredentials() {
        final RackerCredentials rackerAuthCredentials = new RackerCredentials();
        rackerAuthCredentials.setClientId(clientId);
        rackerAuthCredentials.setClientSecret(clientSecret);
        rackerAuthCredentials.setExpirationInSec(expireInSeconds);
        rackerAuthCredentials.setGrantType("NONE");
        rackerAuthCredentials.setUsername(username);
        rackerAuthCredentials.setPassword(userpass.getValue());
        rackerAuthCredentials.setRefreshToken(refreshTokenVal);

        return rackerAuthCredentials;
    }
}