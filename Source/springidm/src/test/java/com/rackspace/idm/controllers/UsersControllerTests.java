package com.rackspace.idm.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.authorizationService.AuthorizationConstants;
import com.rackspace.idm.authorizationService.AuthorizationRequest;
import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.Entity;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.authorizationService.SunAuthorizationService;
import com.rackspace.idm.config.LoggerFactoryWrapper;

import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthApiKey;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;

import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.passwordcomplexity.PasswordComplexityResult;
import com.rackspace.idm.entities.passwordcomplexity.PasswordRuleResult;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.exceptions.IdmException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.jaxb.UserCredentials;
import com.rackspace.idm.jaxb.UserPassword;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.DefaultUserService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.test.stub.StubLogger;
import com.rackspace.idm.validation.InputValidator;

public class UsersControllerTests {

    UsersController controller;
    UserService mockUserService;
    OAuthService mockOAuthService;
    AuthorizationService mockAuthorizationService;
    RoleService mockRoleService;
    ClientService mockClientService;
    PasswordComplexityService mockComplexityService;
    PermissionConverter permissionConverter = new PermissionConverter();
    RoleConverter roleConverter = new RoleConverter(permissionConverter);
    UserConverter userConverter = new UserConverter(roleConverter);
    PasswordConverter passwordConverter = new PasswordConverter();
    TokenConverter tokenConverter = new TokenConverter();

    AccessTokenService mockAccessTokenService;
    IDMAuthorizationHelper authHelper;

    User testUser;
    String authHeader = "OAuth asdf1234";

    String roleName = "Admin";
    String customerId = "123456";
    String username = "testuser";
    String rackspaceUsername = "rackeruser";
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
    UserStatus status = UserStatus.ACTIVE;
    String seeAlso = "seeAlso";
    String country = "USA";
    String displayName = "Test User";
    String apiKey = "ABCDEFGHIJ";
    String personId = "RPN-111-222-333";

    String tokenString = "asdf1234";
    DateTime tokenExpiration = new DateTime(2010, 6, 1, 12, 12, 35, 200);
    String owner = "owner";
    String tStatus = "valid";
    String requestor = "requestor";

    String clientId = "ClientId";
    AccessToken restrictedToken = new AccessToken(tokenString, tokenExpiration,
        owner, requestor, IDM_SCOPE.SET_PASSWORD);
    String callbackUrl = "http://something.com";
    PasswordRecovery recoveryParam;

    PasswordComplexityResult passingResult = new PasswordComplexityResult();
    PasswordComplexityResult failingResult = new PasswordComplexityResult();

    com.rackspace.idm.jaxb.User userJaxb;

    @Before
    public void setUp() throws Exception {
        recoveryParam = new PasswordRecovery();
        recoveryParam.setCallbackUrl(callbackUrl);

        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockUserService = EasyMock.createMock(UserService.class);
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

        controller = new UsersController(mockOAuthService,
            mockAccessTokenService, mockUserService, mockRoleService,
            authHelper, mockComplexityService, userConverter, roleConverter,
            passwordConverter, tokenConverter, new InputValidator(validator),
            new LoggerFactoryWrapper());

        testUser = new User(username);
        testUser.setCustomerId(customerId);
        testUser.setPassword(password);
        testUser.setFirstname(firstname);
        testUser.setLastname(lastname);
        testUser.setEmail(email);
        testUser.setMiddlename(middlename);
        testUser.setSecretQuestion(secretQuestion);
        testUser.setSecretAnswer(secretAnswer);
        testUser.setPrefferedLang(preferredLang);
        testUser.setTimeZone(timeZone);
        testUser.setStatus(status);
        testUser.setSeeAlso(seeAlso);
        testUser.setCountry(country);
        testUser.setDisplayName(displayName);
        testUser.setApiKey(apiKey);
        testUser.setPersonId(personId);
        testUser.setIsLocked(false);

        passingResult.addPasswordRuleResult(new PasswordRuleResult(true, 1,
            "RuleName", "RuleMessage"));
        failingResult.addPasswordRuleResult(new PasswordRuleResult(false, 1,
            "RuleName", "RuleMessage"));

        userJaxb = userConverter.toUserJaxb(testUser);
    }

    @Test
    public void ShouldAddUser() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = createTestUser(username);
        mockUserService.addUser(user);
        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);

        EasyMock.expectLastCall();
        setupAdminAuthorizationReturnsTrue(user);
        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addUser(response, authHeader, customerId, userJaxb);
        Assert.assertEquals(HttpServletResponse.SC_CREATED,
            response.getStatus());
        Assert.assertEquals("/customers/" + customerId + "/users/" + username,
            response.getHeader("Location"));
    }

    @Test
    public void ShouldAddUserWhenRequestedByRacker() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);

        mockUserService.addUser(user);
        EasyMock.expectLastCall();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(user.getUsername(), oldpassword))
            .andReturn(true);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(rackspaceUsername).times(2);

        AccessToken testtoken = createTestToken(true);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock
            .expect(mockUserService.getUser(customerId, user.getUsername()))
            .andReturn(user);
        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(user.getUsername()))
            .andReturn(roleList);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addUser(response, authHeader, customerId, userJaxb);

        Assert.assertEquals(HttpServletResponse.SC_CREATED,
            response.getStatus());

        Assert.assertEquals("/customers/" + customerId + "/users/" + username,
            response.getHeader("Location"));
    }

    @Test(expected = PasswordValidationException.class)
    public void ShouldNotAddUserForInvalidPassword() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);
        mockUserService.addUser(testUser);
        try {
            setupAdminAuthorizationReturnsTrue(user);
        } catch (Exception exp) {
        }

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(failingResult);
        EasyMock.replay(mockComplexityService);

        controller.addUser(response, authHeader, customerId, userJaxb);
    }

    @Test(expected = ForbiddenException.class)
    public void ShouldNotAddUserBecauseNotAuthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);
        mockUserService.addUser(testUser);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        try {
            setupAdminAuthorizationReturnsFalse(user);
        } catch (Exception exp) {
        }

        controller.addUser(response, authHeader, customerId, userJaxb);
    }

    @Test(expected = ForbiddenException.class)
    public void ShouldNotAddUserWhenRequestedNotByRackerNorByAdmin()
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = createTestUser(username);

        mockUserService.addUser(user);
        EasyMock.expectLastCall();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("JohnnyCash@amazon.com").times(2);
        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roleList);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addUser(response, authHeader, customerId, userJaxb);

    }

    @Test(expected = BadRequestException.class)
    public void ShouldNotAddUserIfUsernameIsEmpty() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        userJaxb.setUsername("");

        controller.addUser(response, authHeader, customerId, userJaxb);
    }

    @Test
    public void shouldDeleteUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser("admin");
        mockUserService.deleteUser(username);
        try {
            setupAdminAuthorizationReturnsTrue(user);
        } catch (Exception exp) {

        }

        controller.deleteUser(response, authHeader, customerId, username);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotDeleteSelf() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        User user = createTestUser(username);
        mockUserService.softDeleteUser(username);
        setupAdminAuthorizationReturnsTrue(user);

        controller.deleteUser(response, authHeader, customerId, username);

    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotDeleteUserBecauseNotAuthorized() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        try {
            setupAdminAuthorizationReturnsFalse(user);
        } catch (Exception exp) {

        }

        controller.deleteUser(response, authHeader, customerId, username);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotDeleteUserBecauseApplicationNotAuthorized()
        throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        EasyMock.replay(mockUserService);

        try {
            AccessToken testtoken = createTestToken(false);
            EasyMock
                .expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
                .andReturn(testtoken);
            EasyMock
                .expect(
                    mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
                .andReturn(null).atLeastOnce();
            EasyMock.replay(mockOAuthService);
        } catch (Exception exp) {

        }

        controller.deleteUser(response, authHeader, customerId, username);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteNonExistentUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.deleteUser(response, authHeader, customerId, username);
    }

    // @Test
    // public void shouldRestoreDeletedUser() throws Exception {
    //
    // MockHttpServletResponse response = new MockHttpServletResponse();
    //
    // User user = createTestUser(username);
    //
    // EasyMock.expect(
    // mockUserService.getSoftDeletedUser(customerId, username))
    // .andReturn(user);
    //
    // mockUserService.restoreSoftDeletedUser(user);
    //
    // EasyMock.replay(mockUserService);
    //
    // AccessToken testtoken = createTestToken(true);
    // EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
    // .andReturn(testtoken);
    // setupRackerAuthorizationReturnsTrue();
    //
    // controller.restoreSoftDeletedUser(response, authHeader, customerId,
    // username);
    // Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    // }
    //
    // @Test(expected = ForbiddenException.class)
    // public void shouldNotRestoreDeletedUserWhenNotAuthorized() throws
    // Exception {
    //
    // MockHttpServletResponse response = new MockHttpServletResponse();
    //
    // User user = createTestUser(username);
    //
    // EasyMock.expect(
    // mockUserService.getSoftDeletedUser(customerId, username))
    // .andReturn(user);
    // AccessToken testtoken = createTestToken(false);
    // EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
    // .andReturn(testtoken);
    //
    // mockUserService.restoreSoftDeletedUser(user);
    //
    // EasyMock.replay(mockUserService);
    //
    // setupRackerAuthorizationReturnsFalse();
    //
    // controller.restoreSoftDeletedUser(response, authHeader, customerId,
    // username);
    // }
    //
    // @Test(expected = NotFoundException.class)
    // public void ShouldNotRestoreDeletedUserIfUserDoesNotExist() {
    // MockHttpServletResponse response = new MockHttpServletResponse();
    //
    // User user = createTestUser(username);
    //
    // EasyMock.expect(
    // mockUserService.getSoftDeletedUser(customerId, username))
    // .andReturn(null);
    //
    // mockUserService.restoreSoftDeletedUser(user);
    //
    // EasyMock.replay(mockUserService);
    //
    // AccessToken testtoken = createTestToken(true);
    // EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
    // .andReturn(testtoken);
    // setupRackerAuthorizationReturnsTrue();
    //
    // controller.restoreSoftDeletedUser(response, authHeader, customerId,
    // username);
    // }

    @Test
    public void shouldGetUserPasswordWhenRequestedByAdmin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = createTestUser(username);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("Admin");

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);
        EasyMock.replay(mockUserService);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        com.rackspace.idm.jaxb.UserPassword password = controller
            .getUserPassword(response, authHeader, customerId, username);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertEquals(user.getPasswordNoPrefix(), password.getPassword());
    }

    @Test
    public void shouldGetUserPasswordWhenRequestedByRackspaceApplication()
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = createTestUser(username);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace");

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace");

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        Client client = new Client();

        // Give wrong permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/" + user.getCustomerId()
            + "/users/" + user.getUsername() + "/password/recoverytoken";

        permissions.add(new Permission("GetUserPasswordURI", "IDM",
            permissionURI, "Rackspace"));

        client.setPermissions(permissions);

        EasyMock.expect(mockClientService.getById("rackspace")).andReturn(
            client);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockUserService);

        controller.getUserPassword(response, authHeader, customerId, username);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void shouldGetUserPasswordWhenRequestedByApplicationWithCorrectPermission()
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = createTestUser(username);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace");

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace");

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        Client client = new Client();

        // Give correct permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "GET " + "/customers/" + user.getCustomerId()
            + "/users/" + user.getUsername() + "/password";

        permissions.add(new Permission("GetUserPasswordURI", "IDM",
            permissionURI, "Rackspace"));

        client.setPermissions(permissions);

        EasyMock.expect(mockClientService.getById("rackspace")).andReturn(
            client);

        EasyMock.replay(mockOAuthService);
        EasyMock.replay(mockUserService);
        EasyMock.replay(mockClientService);

        controller.getUserPassword(response, authHeader, customerId, username);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetUserPasswordBecauseNotAuthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("Admin").times(2);
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);
        EasyMock.replay(mockUserService);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);
        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        controller.getUserPassword(response, authHeader, customerId, username);
    }

    @Test
    public void shouldGetResetPasswordTokenForUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock
            .expect(mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace").times(2);

        Client client = new Client();

        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/" + user.getCustomerId()
            + "/users/" + user.getUsername() + "/password/recoverytoken";

        permissions.add(new Permission("RestrictedTokenPermission", "IDM",
            permissionURI, "Rackspace"));

        client.setPermissions(permissions);

        EasyMock.expect(mockClientService.getById("rackspace")).andReturn(
            client);

        AccessToken token = new AccessToken();
        token.setExpiration(1000);

        EasyMock.expect(
            mockAccessTokenService.createPasswordResetAccessTokenForUser(
                username, "rackspace")).andReturn(token);

        EasyMock.replay(mockUserService);

        EasyMock.replay(mockOAuthService);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.replay(mockClientService);

        com.rackspace.idm.jaxb.Token outputToken = controller
            .getPasswordResetToken(response, authHeader, customerId, username);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertEquals(token.getTokenString(), outputToken.getId());
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetResetPasswordTokenForUserWhenNotAuthorized()
        throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = createTestUser(username);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock
            .expect(mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn("rackspace").times(2);

        Client client = new Client();

        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/" + user.getCustomerId()
            + "/users/" + user.getUsername()
            + "/password/NoRecoveryTokenForYou";

        permissions.add(new Permission("RestrictedTokenPermission", "IDM",
            permissionURI, "Rackspace"));

        client.setPermissions(permissions);

        EasyMock.expect(mockClientService.getById("rackspace")).andReturn(
            client);

        AccessToken token = new AccessToken();

        EasyMock.expect(
            mockAccessTokenService.createPasswordResetAccessTokenForUser(
                username, "rackspace")).andReturn(token);

        EasyMock.replay(mockUserService);

        EasyMock.replay(mockOAuthService);

        EasyMock.replay(mockAccessTokenService);

        EasyMock.replay(mockClientService);

        controller.getPasswordResetToken(response, authHeader, customerId,
            username);
    }

    @Test
    public void shouldGetUserWhenQueriedByUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setupUserAuthorizationReturnsTrue();
        } catch (Exception exp) {

        }

        com.rackspace.idm.jaxb.User user = controller.getUser(response,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertEquals(username, user.getUsername());
        Assert.assertEquals(customerId, user.getCustomerId());
    }

    @Test
    public void shouldGetUserWithNoSecrets() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setupUserAuthorizationReturnsTrue();
        } catch (Exception exp) {

        }

        com.rackspace.idm.jaxb.User addedUser = controller.getUser(response,
            authHeader, customerId, username);

        String userObjectString = addedUser.toString();

        int secretQuestionIndex = userObjectString.indexOf("secretQuestion");
        int secretAnswerIndex = userObjectString.indexOf("secretAnswer");

        Assert.assertEquals(-1, secretQuestionIndex);
        Assert.assertEquals(-1, secretAnswerIndex);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldGetUserWhenQueriedByAdmin() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        EasyMock.replay(mockUserService);

        setupAuthorizationforMultiplePolicyChecks(false, true);

        controller.getUser(response, authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetUserBecauseNotAuthorized() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        EasyMock.replay(mockUserService);

        setupAuthorizationforMultiplePolicyChecks(false, false);

        controller.getUser(response, authHeader, customerId, username);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetUserBecauseApplicationNotAuthorized()
        throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        EasyMock.replay(mockUserService);

        try {
            EasyMock
                .expect(
                    mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
                .andReturn(null).times(2);
            EasyMock.replay(mockOAuthService);
        } catch (Exception exp) {

        }

        controller.getUser(response, authHeader, customerId, username);
    }

    @Test(expected = NotFoundException.class)
    public void shouldReturnNotFoundForNonExistentUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.getUser(response, authHeader, customerId, username);
    }

    @Test
    public void shouldResetUserPassword() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        mockUserService.updateUser(testUser);
        EasyMock.expect(
            mockAccessTokenService.getTokenByTokenString(tokenString))
            .andReturn(restrictedToken);
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);
        EasyMock.replay(mockAccessTokenService);

        setupAdminAuthorizationReturnsTrue(testUser);

        com.rackspace.idm.jaxb.UserPassword password = controller
            .resetUserPassword(response, authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertTrue(!testUser.getPasswordObj().equals(userpass));
        Assert.assertEquals(testUser.getPasswordNoPrefix(),
            password.getPassword());
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotResetUserPasswordBecauseNotAuthorized()
        throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn("Rackspace");
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);

        setupAuthorizationforMultiplePolicyChecks(false, false);

        controller
            .resetUserPassword(response, authHeader, customerId, username);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotResetUserPasswordIfUserNotFound() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockAccessTokenService.getTokenByTokenString(tokenString))
            .andReturn(restrictedToken);
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockAccessTokenService, mockOAuthService,
            mockUserService);

        controller
            .resetUserPassword(response, authHeader, customerId, username);
    }

    @Test
    public void shouldSetUserPassword() throws Exception {
        String oldpassword = password;
        String newpassword = "newpass";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        setupUserAuthorizationReturnsTrue();

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, false);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldSetUserPasswordForRecovery() throws Exception {
        String oldpassword = password;
        String newpassword = "newpass";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);
        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(passingResult);
        EasyMock.expect(
            mockAccessTokenService.getTokenByTokenString(tokenString))
            .andReturn(restrictedToken);
        EasyMock.replay(mockComplexityService, mockAccessTokenService);

        setupUserAuthorizationReturnsTrue();

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, true);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldSendPasswordRecoveryEmail() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(clientId);

        try {
            setupRackspaceCompanyAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        EasyMock.replay(mockOAuthService);

        EasyMock.expect(
            mockAccessTokenService.createPasswordResetAccessTokenForUser(
                username, clientId)).andReturn(restrictedToken);
        EasyMock.replay(mockAccessTokenService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.sendRecoveryEmail(username, email, recoveryParam,
            tokenString);
        EasyMock.replay(mockUserService);

        controller.sendRecoveryEmail(response, authHeader, customerId,
            username, recoveryParam);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockAccessTokenService);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSendPasswordRecoveryEmailBecauseNotAuthorized() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(clientId);

        try {
            setupRackspaceCompanyAuthorizationReturnsFalse();
        } catch (Exception exp) {
        }

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.sendRecoveryEmail(username, email, recoveryParam,
            tokenString);
        EasyMock.replay(mockUserService);

        controller.sendRecoveryEmail(response, authHeader, customerId,
            username, recoveryParam);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSendPasswordRecoveryEmailForInactiveUser() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setStatus(UserStatus.INACTIVE);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        EasyMock.replay(mockUserService);

        try {
            setupRackspaceCompanyAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        EasyMock.replay(mockOAuthService);

        controller.sendRecoveryEmail(response, authHeader, customerId,
            username, recoveryParam);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSendPasswordRecoveryEmailForLockedUser() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setIsLocked(true);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        try {
            setupRackspaceCompanyAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        EasyMock.replay(mockOAuthService);

        controller.sendRecoveryEmail(response, authHeader, customerId,
            username, recoveryParam);
    }

    @Test(expected = IdmException.class)
    public void shouldNotSendPasswordRecoveryEmailForNoEmail() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setEmail("");

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        try {
            setupRackspaceCompanyAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        EasyMock.replay(mockOAuthService);

        controller.sendRecoveryEmail(response, authHeader, customerId,
            username, recoveryParam);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotSetPasswordIfFailsComplexityCheck() throws Exception {
        String oldpassword = password;
        String newpassword = "newpass";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(failingResult);
        EasyMock.replay(mockComplexityService);

        setupUserAuthorizationReturnsTrue();

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, false);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldDenySetUserPasswordBecauseNotAuthorized()
        throws Exception {

        String oldpassword = password;
        String newpassword = "newpass";
        String requestingUsername = "requsername";
        String actionName = "setUserPassword";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);
        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(requestingUsername).times(2);
        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        EasyMock.replay(mockOAuthService);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();

        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.updateUser(testUser);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, false);
    }

    @Test
    public void shouldGetRolesForUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        Role role = new Role();
        role.setName(roleName);
        role.setCustomerId(customerId);

        List<Role> roles = new ArrayList<Role>();
        roles.add(role);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roles);
        EasyMock.replay(mockRoleService);

        com.rackspace.idm.jaxb.Roles jaxbRoles = controller.getRoles(response,
            customerId, username);

        List<com.rackspace.idm.jaxb.Role> returnedRoles = jaxbRoles.getRoles();

        Assert.assertTrue(returnedRoles.size() == 1);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockUserService);
        EasyMock.verify(mockRoleService);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetRolesForNonExistentUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        Role role = new Role();
        role.setName(roleName);
        role.setCustomerId(customerId);

        List<Role> roles = new ArrayList<Role>();
        roles.add(role);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.getRoles(response, customerId, username);
    }

    @Test
    public void shouldSetRole() throws Exception {

        Role role2 = new Role();
        role2.setName(roleName);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(testUser.getUsername());

        setupAdminAuthorizationReturnsTrue(testUser);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(role2);

        List<Role> roles = new ArrayList<Role>();
        roles.add(role2);

        EasyMock
            .expect(mockRoleService.getRolesForUser(testUser.getUsername()))
            .andReturn(roles);

        mockRoleService.addUserToRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller
            .setRole(response, authHeader, customerId, username, roleName);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldDenySetRoleBecauseNotAuthorized() throws Exception {

        Role role2 = new Role();
        role2.setName(roleName);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(testUser.getUsername());

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAdminAuthorizationReturnsFalse(testUser);

        List<Role> roles = new ArrayList<Role>();
        roles.add(role2);

        EasyMock
            .expect(mockRoleService.getRolesForUser(testUser.getUsername()))
            .andReturn(roles);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(role2);
        mockRoleService.addUserToRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller
            .setRole(response, authHeader, customerId, username, roleName);

    }

    @Test(expected = BadRequestException.class)
    public void shouldDenySetRoleForBlankRoleName() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.setRole(response, authHeader, customerId, username, "");
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotSetRoleForNonExistentUser() throws Exception {
        String oldpassword = password;
        String newpassword = "newpass";
        String role = "admin";
        Role role2 = new Role();
        role2.setName(roleName);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();

        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roleList);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(role2);
        mockRoleService.addUserToRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller
            .setRole(response, authHeader, customerId, username, roleName);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotSetRoleForNonExistentRole() throws Exception {
        String oldpassword = password;
        Role role2 = new Role();
        role2.setName(roleName);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();

        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roleList);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(null);
        mockRoleService.addUserToRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller
            .setRole(response, authHeader, customerId, username, roleName);
    }

    @Test
    public void shouldDeleteRole() throws Exception {

        String roleToDelete = "deleteThisRole";

        Role role2 = new Role();
        role2.setName(roleToDelete);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        setupAdminAuthorizationReturnsTrue(testUser);

        EasyMock.expect(mockRoleService.getRole(roleToDelete, customerId))
            .andReturn(role2);
        mockRoleService.deleteUserFromRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller.deleteRole(response, authHeader, customerId, username,
            roleToDelete);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldDeleteAdminRoleOfOthers() throws Exception {

        String roleToDelete = "admin";

        Role role2 = new Role();
        role2.setName(roleToDelete);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setUsername("dev");
        setupAdminAuthorizationReturnsTrue(testUser);

        EasyMock.expect(mockRoleService.getRole(roleToDelete, customerId))
            .andReturn(role2);
        mockRoleService.deleteUserFromRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller.deleteRole(response, authHeader, customerId, username,
            roleToDelete);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldDeleteNonAdminRoleOfSelf() throws Exception {

        String roleToDelete = "Racker";

        Role role2 = new Role();
        role2.setName(roleToDelete);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        setupAdminAuthorizationReturnsTrue(testUser);

        EasyMock.expect(mockRoleService.getRole(roleToDelete, customerId))
            .andReturn(role2);
        mockRoleService.deleteUserFromRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller.deleteRole(response, authHeader, customerId, username,
            roleToDelete);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldDenyDeleteRoleBecauseNotAuthorized() throws Exception {

        Role role2 = new Role();
        role2.setName(roleName);
        role2.setCustomerId(customerId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAdminAuthorizationReturnsFalse(testUser);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(role2);
        mockRoleService.addUserToRole(testUser, role2);
        EasyMock.replay(mockRoleService);

        controller.deleteRole(response, authHeader, customerId, username,
            roleName);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteRoleForNonExistentUser() throws Exception {

        String role = "admin";
        Role testrole = new Role();
        testrole.setName(role);
        testrole.setCustomerId(testUser.getCustomerId());

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.deleteRole(response, null, customerId, username, role);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteRoleForNonExistentRole() throws Exception {
        String role = "roleToDelete";
        Role testrole = new Role();
        testrole.setName(role);
        testrole.setCustomerId(testUser.getCustomerId());

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock
            .expect(mockRoleService.getRole(role, testrole.getCustomerId()))
            .andReturn(null);

        setupAdminAuthorizationReturnsTrue(testUser);
        EasyMock.replay(mockRoleService);
        controller.deleteRole(response, authHeader, customerId, username, role);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotSetUserPasswordIfUserNotFound() throws Exception {

        String oldpassword = password;
        String newpassword = "newpass";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, false);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotSetUserPasswordWithBadOldPassword() throws Exception {

        String oldpassword = password;
        String newpassword = "newpass";

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockOAuthService.authenticateAuthHeader(authHeader))
            .andReturn(true);
        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(false);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        mockUserService.updateUser(testUser);

        EasyMock.expect(mockComplexityService.checkPassword(newpassword))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        EasyMock.replay(mockUserService);

        UserCredentials userCredentials = new UserCredentials();

        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword(oldpassword);
        userCredentials.setCurrentPassword(currentPassword);

        UserPassword newPassword = new UserPassword();
        newPassword.setPassword(newpassword);
        userCredentials.setNewPassword(newPassword);

        controller.setUserPassword(response, authHeader, customerId, username,
            userCredentials, false);
    }

    @Test
    public void shouldSetUserStatusByAdmin() {

        String status = "inactive";

        MockHttpServletResponse response = new MockHttpServletResponse();

        com.rackspace.idm.jaxb.User outputUser = null;
        try {
            EasyMock.expect(mockUserService.getUser(customerId, username))
                .andReturn(testUser);
            mockUserService.updateUserStatus(testUser, status.toUpperCase());
            setupAdminAuthorizationReturnsTrue(testUser);
            EasyMock.replay(mockRoleService);

            com.rackspace.idm.jaxb.User inputUser = new com.rackspace.idm.jaxb.User();

            inputUser.setStatus(com.rackspace.idm.jaxb.UserStatus
                .fromValue(status.toUpperCase()));

            outputUser = controller.setUserStatus(response, authHeader,
                customerId, username, inputUser);
        } catch (Exception exp) {
            exp.printStackTrace();
        }

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);

        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldSetUserStatusByCompany() {

        String status = "inactive";

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            EasyMock.expect(mockUserService.getUser(customerId, username))
                .andReturn(testUser);
            mockUserService.updateUserStatus(testUser, status.toUpperCase());

            setupAdminAuthorizationReturnsTrue(testUser);
            EasyMock.replay(mockRoleService);

            com.rackspace.idm.jaxb.User inputUser = new com.rackspace.idm.jaxb.User();

            inputUser.setStatus(com.rackspace.idm.jaxb.UserStatus
                .fromValue(status.toUpperCase()));

            controller.setUserStatus(response, authHeader, customerId,
                username, inputUser);
        } catch (Exception exp) {
        }

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetUserStatusBecauseNotAuthorized() {

        String status = "inactive";

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            AccessToken testtoken = createTestToken(false);
            EasyMock
                .expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
                .andReturn(testtoken);

            mockUserService.updateUser(testUser);
            setupAdminAuthorizationReturnsFalse(testUser);
            EasyMock.replay(mockRoleService);

        } catch (Exception exp) {
        }

        com.rackspace.idm.jaxb.User inputUser = new com.rackspace.idm.jaxb.User();

        com.rackspace.idm.jaxb.UserStatus userStatus = com.rackspace.idm.jaxb.UserStatus
            .fromValue(status.toUpperCase());

        inputUser.setStatus(userStatus);

        controller.setUserStatus(response, authHeader, customerId, username,
            inputUser);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotSetUserStatusIfUserNotFound() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        String status = "inactive";

        MockHttpServletResponse response = new MockHttpServletResponse();

        com.rackspace.idm.jaxb.User inputUser = new com.rackspace.idm.jaxb.User();

        com.rackspace.idm.jaxb.UserStatus userStatus = com.rackspace.idm.jaxb.UserStatus
            .fromValue(status.toUpperCase());

        inputUser.setStatus(userStatus);

        controller.setUserStatus(response, authHeader, customerId, username,
            inputUser);
    }

    @Test
    public void shouldSetUserLockWhenHavingCorrectPermissions() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setIsLocked(true);

        Client client = new Client();

        // Give correct permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "PUT " + "/customers/"
            + testUser.getCustomerId() + "/users/" + testUser.getUsername()
            + "/lock";

        permissions.add(new Permission("UserLockURI", "IDM", permissionURI,
            "Rackspace"));

        client.setPermissions(permissions);

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(client.getClientId());
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockClientService.getById(client.getClientId()))
            .andReturn(client);
        EasyMock.replay(mockClientService);

        try {
            EasyMock.expect(mockUserService.getUser(customerId, username))
                .andReturn(testUser);

            mockUserService.updateUser(testUser);

            EasyMock.replay(mockUserService);

            com.rackspace.idm.jaxb.User inputUser = userConverter
                .toUserJaxb(testUser);

            com.rackspace.idm.jaxb.User outputUser = controller.setUserLock(
                response, authHeader, customerId, username, inputUser);

            Assert.assertTrue(outputUser.isLocked() == true);

        } catch (Exception exp) {
            exp.printStackTrace();
            Assert.assertFalse(false);
        }

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldSetUserLockForRacker() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setIsLocked(true);

        Client client = new Client();

        // Give wrong permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/"
            + testUser.getCustomerId() + "/users/" + testUser.getUsername()
            + "/lock";

        permissions.add(new Permission("UserLockURI", "IDM", permissionURI,
            "Rackspace"));

        client.setPermissions(permissions);

        AccessToken trustedToken = createTestToken(true);

        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(trustedToken);

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(client.getClientId());
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockClientService.getById(client.getClientId()))
            .andReturn(client);
        EasyMock.replay(mockClientService);

        try {
            EasyMock.expect(mockUserService.getUser(customerId, username))
                .andReturn(testUser);

            mockUserService.updateUser(testUser);

            EasyMock.replay(mockUserService);

            com.rackspace.idm.jaxb.User inputUser = userConverter
                .toUserJaxb(testUser);

            controller.setUserLock(response, authHeader, customerId, username,
                inputUser);
        } catch (Exception exp) {
            exp.printStackTrace();
            Assert.assertFalse(false);
        }

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test
    public void shouldSetUserLockForRackspaceApplication() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setIsLocked(true);

        Client client = new Client();
        // Set the client as Rackspace Company Client.
        String companyId = AuthorizationConstants.RACKSPACE_COMPANY_ID;
        client.setClientId(companyId);

        // Give wrong permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/"
            + testUser.getCustomerId() + "/users/" + testUser.getUsername()
            + "/lock";

        permissions.add(new Permission("UserLockURI", "IDM", permissionURI,
            "Rackspace"));

        client.setPermissions(permissions);

        try {
            setupRackspaceCompanyAuthorizationReturnsTrue();
        } catch (Exception exp) {
            Assert.assertFalse("Should not come here.", false);
        }

        // The token is not trusted.
        AccessToken trustedToken = createTestToken(false);

        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(trustedToken);

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(client.getClientId());

        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockClientService.getById(client.getClientId()))
            .andReturn(client);
        EasyMock.replay(mockClientService);

        try {
            EasyMock.expect(mockUserService.getUser(customerId, username))
                .andReturn(testUser);

            mockUserService.updateUser(testUser);

            EasyMock.replay(mockUserService);

            com.rackspace.idm.jaxb.User inputUser = userConverter
                .toUserJaxb(testUser);

            controller.setUserLock(response, authHeader, customerId, username,
                inputUser);
        } catch (Exception exp) {
            exp.printStackTrace();
            Assert.assertFalse(false);
        }

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetUserLockBecauseNotAuthorized() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        testUser.setIsLocked(true);

        Client client = new Client();
        // Set the client as Rackspace Company Client.
        String companyId = AuthorizationConstants.RACKSPACE_COMPANY_ID;
        client.setClientId(companyId);

        // Give wrong permission
        List<Permission> permissions = new ArrayList<Permission>();
        String permissionURI = "POST " + "/customers/"
            + testUser.getCustomerId() + "/users/" + testUser.getUsername()
            + "/lock";

        permissions.add(new Permission("UserLockURI", "IDM", permissionURI,
            "Rackspace"));

        client.setPermissions(permissions);

        try {
            setupRackspaceCompanyAuthorizationReturnsFalse();
        } catch (Exception exp) {
            Assert.assertFalse("Should not come here.", false);
        }

        // The token is not trusted.
        AccessToken trustedToken = createTestToken(false);

        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(trustedToken);

        EasyMock.expect(
            mockOAuthService.getClientIdFromAuthHeaderToken(authHeader))
            .andReturn(client.getClientId());

        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockClientService.getById(client.getClientId()))
            .andReturn(client);
        EasyMock.replay(mockClientService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        mockUserService.updateUser(testUser);

        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.User inputUser = userConverter
            .toUserJaxb(testUser);

        controller.setUserLock(response, authHeader, customerId, username,
            inputUser);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotLockUserIfUserNotFound() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        MockHttpServletResponse response = new MockHttpServletResponse();

        com.rackspace.idm.jaxb.User inputUser = new com.rackspace.idm.jaxb.User();

        inputUser.setLocked(true);

        controller.setUserLock(response, authHeader, customerId, username,
            inputUser);
    }

    @Test
    public void shouldSetUserSecret() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setupUserAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        com.rackspace.idm.jaxb.UserSecret userSecret = new com.rackspace.idm.jaxb.UserSecret();
        userSecret.setSecretQuestion(secretQuestion);
        userSecret.setSecretAnswer(secretAnswer);

        com.rackspace.idm.jaxb.UserSecret returnedSecret = controller
            .setUserSecret(response, authHeader, customerId, username,
                userSecret);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertEquals(userSecret.getSecretQuestion(),
            returnedSecret.getSecretQuestion());
        Assert.assertEquals(userSecret.getSecretAnswer(),
            returnedSecret.getSecretAnswer());
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotSetUserSecretBecauseNotAuthorized() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            AccessToken testtoken = createTestToken(false);
            EasyMock
                .expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
                .andReturn(testtoken);
            setupUserAuthorizationReturnsFalse();
        } catch (Exception exp) {
        }

        com.rackspace.idm.jaxb.UserSecret userSecret = new com.rackspace.idm.jaxb.UserSecret();
        userSecret.setSecretQuestion(secretQuestion);
        userSecret.setSecretAnswer(secretAnswer);
        controller.setUserSecret(response, authHeader, customerId, username,
            userSecret);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotSetUserSecretIfUserNotFound() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        MockHttpServletResponse response = new MockHttpServletResponse();
        com.rackspace.idm.jaxb.UserSecret userSecret = new com.rackspace.idm.jaxb.UserSecret();
        userSecret.setSecretQuestion(secretQuestion);
        userSecret.setSecretAnswer(secretAnswer);

        controller.setUserSecret(response, authHeader, customerId, username,
            userSecret);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotSetUserSecretIfQuestionBlank() {

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.updateUser(testUser);
        EasyMock.replay(mockUserService);

        MockHttpServletResponse response = new MockHttpServletResponse();

        com.rackspace.idm.jaxb.UserSecret userSecret = new com.rackspace.idm.jaxb.UserSecret();
        userSecret.setSecretAnswer(secretAnswer);
        controller.setUserSecret(response, authHeader, customerId, username,
            userSecret);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotSetUserSecretIfAnswerBlank() {

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.updateUser(testUser);
        EasyMock.replay(mockUserService);

        MockHttpServletResponse response = new MockHttpServletResponse();

        com.rackspace.idm.jaxb.UserSecret userSecret = new com.rackspace.idm.jaxb.UserSecret();
        controller.setUserSecret(response, authHeader, customerId, username,
            userSecret);
    }

    @Test
    public void shouldUpdateUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);

        String newFirstname = "newFirstname";
        String newLastname = "newLastname";
        String newMiddlename = "newMiddlename";
        String newEmail = "newEmail";

        testUser.setFirstname(newFirstname);
        testUser.setLastname(newLastname);
        testUser.setMiddlename(newMiddlename);
        testUser.setEmail(newEmail);

        com.rackspace.idm.jaxb.User inputUser = userConverter
            .toUserJaxb(testUser);

        try {
            setupUserAuthorizationReturnsTrue();
        } catch (Exception exp) {
        }

        com.rackspace.idm.jaxb.User outputUser = controller.updateUser(
            response, authHeader, customerId, username, inputUser);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertEquals(inputUser.getLastName(), outputUser.getLastName());
        Assert.assertEquals(inputUser.getMiddleName(),
            outputUser.getMiddleName());
        Assert.assertEquals(inputUser.getEmail(), outputUser.getEmail());

    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotUpdateUserBecauseNotAuthorized() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        populateUserFields();

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAuthorizationforMultiplePolicyChecks(false, false);

        com.rackspace.idm.jaxb.User inputUser = userConverter
            .toUserJaxb(testUser);

        controller.updateUser(response, authHeader, customerId, username,
            inputUser);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotUpdateNonExistentUser() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.User inputUser = userConverter
            .toUserJaxb(testUser);
        controller.updateUser(response, authHeader, customerId, username,
            inputUser);
    }

    @Test
    public void shouldResetApiKey() {
        EasyMock.expect(mockUserService.generateApiKey()).andReturn(
            new DefaultUserService(null, null, null, null, null, null, null,
                null, false, new StubLogger()).generateApiKey());
        mockUserService.updateUser(testUser);
        setupAdminAuthorizationReturnsTrue(testUser);
        UserApiKey apiKeyObj = controller.resetApiKey(authHeader, customerId,
            username);

        Assert.assertTrue(!testUser.getApiKey().equals(apiKey));
        Assert.assertNotNull(apiKeyObj);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotResetApiKeyWhenNotAuthorized()
        throws XACMLRequestCreationException {

        EasyMock.expect(mockUserService.generateApiKey()).andReturn(
            new DefaultUserService(null, null, null, null, null, null, null,
                null, false, new StubLogger()).generateApiKey());

        mockUserService.updateUser(testUser);

        populateUserFields();

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAuthorizationforMultiplePolicyChecks(false, false);

        controller.resetApiKey(authHeader, customerId, username);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotResetApiKeyIfUserNotFound() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.resetApiKey(authHeader, customerId, username);
    }
    
    @Test
    public void shouldGetApiKey() {
        setupAdminAuthorizationReturnsTrue(testUser);
        UserApiKey apiKeyObj = controller.getApiKey(authHeader, customerId,
            username);

        Assert.assertTrue(testUser.getApiKey().equals(apiKey));
        Assert.assertNotNull(apiKeyObj);
        EasyMock.verify(mockUserService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetApiKeyWhenNotAuthorized()
        throws XACMLRequestCreationException {

        populateUserFields();

        AccessToken testtoken = createTestToken(false);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAuthorizationforMultiplePolicyChecks(false, false);

        controller.getApiKey(authHeader, customerId, username);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetApiKeyIfUserNotFound() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        controller.getApiKey(authHeader, customerId, username);
    }

    // private helpers
    private void populateUserFields() {
        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        String newFirstname = "newFirstname";
        String newLastname = "newLastname";
        String newMiddlename = "newMiddlename";
        String newEmail = "newEmail";

        testUser.setFirstname(newFirstname);
        testUser.setLastname(newLastname);
        testUser.setMiddlename(newMiddlename);
        testUser.setEmail(newEmail);
    }

    private void setupRackerAuthorizationReturnsTrue() {

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("Racker@rackspace.com");

        EasyMock.replay(mockOAuthService);
    }

    private void setupRackerAuthorizationReturnsFalse() {

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("Googler@gmail.com");

        EasyMock.replay(mockOAuthService);
    }

    private void setupRackspaceCompanyAuthorizationReturnsTrue()
        throws Exception {

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn("Rackspace");

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);
    }

    private void setupRackspaceCompanyAuthorizationReturnsFalse()
        throws Exception {

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn("Rackspace");

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);
    }

    private void setupUserAuthorizationReturnsTrue() throws Exception {
        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.updateUser(testUser);
        EasyMock.replay(mockUserService);
    }

    private void setupUserAuthorizationReturnsFalse() throws Exception {
        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username).times(2);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        mockUserService.updateUser(testUser);
        EasyMock.replay(mockUserService);
    }

    protected void setupAdminAuthorizationReturnsTrue(User user) {

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username).atLeastOnce();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);
        EasyMock.replay(mockUserService);

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roleList);
    }

    private void setupAdminAuthorizationReturnsFalse(User user)
        throws XACMLRequestCreationException {

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(username).times(2);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(user);

        EasyMock.replay(mockUserService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser(username)).andReturn(
            roleList);
    }

    private void setupAuthorizationforMultiplePolicyChecks(
        boolean expectedResult1, boolean expectedResult2) {
        try {
            AuthorizationRequest authorizationRequest = new AuthorizationRequest();

            EasyMock.expect(
                mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
                .andReturn(username);

            EasyMock.expect(
                mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
                .andReturn(username);

            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
            EasyMock.expect(
                mockAuthorizationService.doAuthorization(authorizationRequest))
                .andReturn(expectedResult1);

            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
            EasyMock.expect(
                mockAuthorizationService.doAuthorization(authorizationRequest))
                .andReturn(expectedResult2);
            EasyMock.replay(mockAuthorizationService);
            EasyMock.replay(mockOAuthService);
        } catch (Exception exp) {

        }
    }

    private User createTestUser(String testusername) {
        User user = new User(testusername, customerId, email,
            new UserHumanName(firstname, middlename, lastname),
            new UserLocale(), new UserCredential(userpass, secretQuestion,
                secretAnswer));
        user.setCountry("USA");
        user.setDisplayName(displayName);
        user.setPersonId(personId);
        user.setDefaults();
        user.setIsLocked(false);
        return user;
    }

    private AccessToken createTestToken(boolean isTrusted) {
        String tokenString = "asdf1234";
        DateTime expiration = new DateTime().plusHours(1);
        AccessToken testToken = new AccessToken(tokenString, expiration, owner,
            username, IDM_SCOPE.FULL, isTrusted);
        return testToken;
    }
}
