package com.rackspace.idm.rest.resources;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.UserService;

public class ApiKeyResourceTests {

    ApiKeyResource resource;
    UserService mockUserService;
    AuthorizationService mockAuthorizationService;
    UriInfo mockUriInfo;
    Request mockRequest;

    User testUser;

    String verb = "VERB";
    String uri = "uri";

    String authHeader = "OAuth asdf1234";
    String customerId = "123456";
    String username = "testuser";
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
    String resetApiKey = "RESETAPIKEY";
    String personId = "RPN-111-222-333";

    @Before
    public void setUp() {
        mockUserService = EasyMock.createMock(UserService.class);
        mockAuthorizationService = EasyMock
            .createMock(AuthorizationService.class);

        resource = new ApiKeyResource(mockUserService,
            mockAuthorizationService, new LoggerFactoryWrapper());
        mockUriInfo = EasyMock.createMock(UriInfo.class);
        mockRequest = EasyMock.createMock(Request.class);

        setUpTestUser();
    }

    // GetUserApiKey Tests
    @Test
    public void UserShouldGetUserApiKeyWithRackerToken() {

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        Response response = resource.getApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
    }
    
    @Test
    public void UserShouldGetUserApiKeyWithClientToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        Response response = resource.getApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void UserShouldGetUserApiKeyWithAdminToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        Response response = resource.getApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void UserShouldGetUserApiKeyWithUserToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeUser(authHeader, customerId, username)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        Response response = resource.getApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }

    @Test(expected = NotFoundException.class)
    public void ShouldNotGetUserApiKeyForNonExistentUser() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        resource.getApiKey(mockRequest, mockUriInfo, authHeader, customerId,
            username);
    }

    @Test(expected = ForbiddenException.class)
    public void ShouldNotGetUserApiKeyIfNotAuthorized() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeUser(authHeader, customerId, username)).andReturn(false);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.replay(mockUserService);

        Response response = resource.getApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);
    }

    // ResetUserApiKey Tests
    @Test
    public void UserShouldResetUserApiKeyWithUserToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeUser(authHeader, customerId, username)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey())
            .andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);

        Response response = resource.resetApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void UserShouldResetUserApiKeyWithAdminToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey())
            .andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);

        Response response = resource.resetApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void UserShouldResetUserApiKeyWithClientToken() {

        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey())
            .andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);

        Response response = resource.resetApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
        EasyMock.verify(mockRequest);
        EasyMock.verify(mockUriInfo);
    }
    
    @Test
    public void UserShouldResetUserApiKeyWithRackerToken() {

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey())
            .andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);

        Response response = resource.resetApiKey(mockRequest, mockUriInfo,
            authHeader, customerId, username);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey) response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockAuthorizationService);
    }

    @Test(expected = NotFoundException.class)
    public void ShouldNotResetUserApiKeyForNonExistentUser() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(true);
        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(mockUserService.getUser(customerId, username))
            .andReturn(null);
        EasyMock.replay(mockUserService);

        resource.resetApiKey(mockRequest, mockUriInfo, authHeader, customerId,
            username);
    }

    @Test(expected = ForbiddenException.class)
    public void ShouldNotResetUserApiKeyIfNotAuthorized() {
        EasyMock.expect(mockRequest.getMethod()).andReturn(verb);
        EasyMock.replay(mockRequest);

        EasyMock.expect(mockUriInfo.getPath()).andReturn(uri);
        EasyMock.replay(mockUriInfo);

        EasyMock.expect(
            mockAuthorizationService.authorizeRacker(authHeader)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeClient(authHeader, verb, uri)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeAdmin(authHeader, customerId)).andReturn(false);
        EasyMock.expect(
            mockAuthorizationService.authorizeUser(authHeader, customerId, username)).andReturn(false);
        EasyMock.replay(mockAuthorizationService);

        resource.resetApiKey(mockRequest, mockUriInfo, authHeader, customerId,
            username);
    }

    private void setUpTestUser() {
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
    }
}
