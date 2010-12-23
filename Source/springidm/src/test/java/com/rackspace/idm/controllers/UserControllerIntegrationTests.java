package com.rackspace.idm.controllers;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.authorizationService.AuthorizationRequest;
import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.Entity;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.authorizationService.SunAuthorizationService;
import com.rackspace.idm.config.LdapConfiguration;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.config.PropertyFileConfiguration;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.AuthDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.LdapClientRepository;
import com.rackspace.idm.dao.LdapConnectionPools;
import com.rackspace.idm.dao.LdapCustomerRepository;
import com.rackspace.idm.dao.LdapUserRepository;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.passwordcomplexity.PasswordComplexityResult;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.DefaultUserService;
import com.rackspace.idm.services.EmailService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.validation.InputValidator;

public class UserControllerIntegrationTests {

    // Real entities
    UsersController controller;
    UserService userService;
    UserDao userDao;

    private LdapUserRepository repo;
    private LdapConnectionPools connPools;

    // Mock entities
    OAuthService mockOAuthService;
    AuthorizationService mockAuthorizationService;
    RoleService mockRoleService;
    ClientService mockClientService;
    PasswordComplexityService mockComplexityService;
    AccessTokenService mockAccessTokenService;
    IDMAuthorizationHelper authHelper;

    CustomerDao customerDao;
    AccessTokenDao mockTokenDao;
    RefreshTokenDao mockRefreshTokenDao;
    ClientDao clientDao;
    EmailService mockEmailService;
    AuthDao mockRackerDao;

    String customerId = "RCN-000-000-000";
    String username = null;

    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = null;

    String middlename = "middle";
    String secretQuestion = "question";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    UserStatus status = UserStatus.ACTIVE;
    String seeAlso = "seeAlso";
    String country = "USA";
    String displayName = "Test User";
    String apiKey = "ABCDEFGHIJ";
    String personId = "RPN-111-222-333";

    String tokenString = "asdf1234";
    String authHeader = "OAuth asdf1234";
    String adminUsername = "AdminUser@rackspace.com";
    PasswordComplexityResult passingResult = new PasswordComplexityResult();
    PermissionConverter permissionConverter = new PermissionConverter();
    RoleConverter roleConverter = new RoleConverter(permissionConverter);
    UserConverter userConverter = new UserConverter(roleConverter);
    PasswordConverter passwordConverter = new PasswordConverter();
    TokenConverter tokenConverter = new TokenConverter();

    com.rackspace.idm.jaxb.User userJaxb;

    @Before
    public void setUp() throws Exception {

        username = "testuser" + (new Random()).nextInt();
        email = username;

        mockTokenDao = EasyMock.createMock(AccessTokenDao.class);
        mockRefreshTokenDao = EasyMock.createMock(RefreshTokenDao.class);

        mockEmailService = EasyMock.createMock(EmailService.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);

        mockRoleService = EasyMock.createMock(RoleService.class);
        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockAuthorizationService = EasyMock
            .createMock(SunAuthorizationService.class);
        mockRoleService = EasyMock.createMock(RoleService.class);
        mockClientService = EasyMock.createMock(ClientService.class);
        mockComplexityService = EasyMock
            .createMock(PasswordComplexityService.class);
        mockAccessTokenService = EasyMock.createMock(AccessTokenService.class);

        Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

        IDMAuthorizationHelper authHelper = new IDMAuthorizationHelper(
            mockOAuthService, mockAuthorizationService, mockRoleService,
            mockClientService, new StubLogger());

        connPools = new LdapConfiguration(new PropertyFileConfiguration()
            .getConfigFromClasspath(), new StubLogger()).connectionPools();
        userDao = new LdapUserRepository(connPools, new StubLogger());
        customerDao = new LdapCustomerRepository(connPools, new StubLogger());
        clientDao = new LdapClientRepository(connPools, new StubLogger());

        userService = new DefaultUserService(userDao, mockRackerDao,
            customerDao, mockTokenDao, mockRefreshTokenDao, clientDao,
            mockEmailService, mockRoleService, false, new StubLogger());

        controller = new UsersController(mockOAuthService,
            mockAccessTokenService, userService, mockRoleService, authHelper,
            mockComplexityService, userConverter, roleConverter,
            passwordConverter, tokenConverter, new InputValidator(validator),
            new LoggerFactoryWrapper());
    }

    @Test
    public void shouldAddDeleteAndThenRestoreDeletedUser() {

        MockHttpServletResponse response = new MockHttpServletResponse();
        com.rackspace.idm.jaxb.User user = createTestJaxbUser(username);

        AccessToken testToken = createTestToken(true);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testToken);

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            null);

        addUser(user, response);
        deleteUser(user, response);
        restoreDeletedUser(user, response);

        // Cleanup
        userService.deleteUser(user.getUsername());
    }

    @Test
    // This tests whether our system supports UTF-8.
    public void shouldAddUserWithLatinName() {

        String latinUserName = "\u0227";
        MockHttpServletResponse response = new MockHttpServletResponse();
        com.rackspace.idm.jaxb.User user = createTestJaxbUser(latinUserName);

        AccessToken testToken = createTestToken(true);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testToken);

        EasyMock.expect(mockRoleService.getRolesForUser(latinUserName))
            .andReturn(null);

        setupAdminAuthorizationReturnsTrue(user);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        user.setUsername("\u0227");

        com.rackspace.idm.jaxb.User addedUser = controller.addUser(response,
            authHeader, customerId, user);

        Assert.assertEquals(user.getUsername(), addedUser.getUsername());

        // Cleanup
        userService.deleteUser(user.getUsername());

    }

    private void addUser(com.rackspace.idm.jaxb.User user,
        MockHttpServletResponse response) {

        setupAdminAuthorizationReturnsTrue(user);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addUser(response, authHeader, customerId, user);
    }

    private void deleteUser(com.rackspace.idm.jaxb.User user,
        MockHttpServletResponse response) {

        user.setSoftDeleted(true);
        // controller.deleteUser(response, authHeader, customerId, username);
        controller.setUserSoftDelete(response, authHeader, customerId,
            username, user);
    }

    private void restoreDeletedUser(com.rackspace.idm.jaxb.User user,
        MockHttpServletResponse response) {

        user.setSoftDeleted(false);
        // controller.restoreSoftDeletedUser(response, authHeader, customerId,
        // username);
        controller.setUserSoftDelete(response, authHeader, customerId,
            username, user);

        Assert.assertEquals(HttpServletResponse.SC_CREATED, response
            .getStatus());
    }

    private User createTestUser(String testusername) {
        User user = new User(testusername, customerId, email,
            new UserHumanName(firstname, middlename, lastname),
            new UserLocale(), new UserCredential(userpass, secretQuestion,
                secretAnswer));
        user.setSoftDeleted(false);
        user.setRegion("ORD");
        user.setDefaults();
        return user;
    }

    private com.rackspace.idm.jaxb.User createTestJaxbUser(String testusername) {
        return userConverter.toUserJaxb(createTestUser(testusername));
    }

    protected void setupAdminAuthorizationReturnsTrue(
        com.rackspace.idm.jaxb.User user) {

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(adminUsername).atLeastOnce();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest).atLeastOnce();
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true).atLeastOnce();

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(adminUsername))
            .andReturn(roleList).atLeastOnce();

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockRoleService);
    }

    private AccessToken createTestToken(boolean isTrusted) {
        String tokenString = "asdf1234";
        DateTime expiration = new DateTime().plusHours(1);
        AccessToken testToken = new AccessToken(tokenString, expiration,
            username, username, IDM_SCOPE.FULL, isTrusted);
        return testToken;
    }
}
