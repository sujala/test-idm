package com.rackspace.idm.rest.resources;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.UserService;

public class ApiKeyResourceTests {

    ApiKeyResource resource;
    UserService mockUserService;
    IDMAuthorizationHelper mockIDMAuthHelper;
    OAuthService mockOAuthService;
    
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
    String resetApiKey = "RESETAPIKEY";
    String personId = "RPN-111-222-333";

    @Before
    public void setUp() {
        mockUserService = EasyMock.createMock(UserService.class);
        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockIDMAuthHelper = EasyMock.createMock(IDMAuthorizationHelper.class);

        resource = new ApiKeyResource(mockUserService, mockIDMAuthHelper,
            mockOAuthService, new LoggerFactoryWrapper());
        
        setUpTestUser();
    }
    
    // GetUserApiKey Tests
    @Test
    public void UserShouldGetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.getApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test
    public void AdminShouldGetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.getApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test
    public void RackerShouldGetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkRackspaceEmployeeAuthorization(authHeader)).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.getApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(apiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test(expected = NotFoundException.class)
    public void ShouldNotGetUserApiKeyForNonExistentUser() {
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(null);
        EasyMock.replay(mockUserService);
        
        resource.getApiKey(authHeader, customerId, username);
    }
    
    @Test(expected = ForbiddenException.class)
    public void ShouldNotGetUserApiKeyIfNotAuthorized() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkRackspaceEmployeeAuthorization(authHeader)).andReturn(false);
        mockIDMAuthHelper.handleAuthorizationFailure();
        EasyMock.expectLastCall().andThrow(new ForbiddenException());
        EasyMock.replay(mockIDMAuthHelper);
        
        resource.getApiKey(authHeader, customerId, username);
    }
    
    //ResetUserApiKey Tests
    @Test
    public void UserShouldResetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey()).andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.resetApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test
    public void AdminShouldResetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey()).andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.resetApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test
    public void RackerShouldResetUserApiKey() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.expect(mockUserService.generateApiKey()).andReturn(resetApiKey);
        mockUserService.updateUser(EasyMock.anyObject(User.class));
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkRackspaceEmployeeAuthorization(authHeader)).andReturn(true);
        EasyMock.replay(mockIDMAuthHelper);
        
        Response response = resource.resetApiKey(authHeader, customerId, username);
        
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        UserApiKey key = (UserApiKey)response.getEntity();
        Assert.assertTrue(key.getApiKey().equals(resetApiKey));
        EasyMock.verify(mockUserService);
        EasyMock.verify(mockOAuthService);
        EasyMock.verify(mockIDMAuthHelper);
    }
    
    @Test(expected = NotFoundException.class)
    public void ShouldNotResetUserApiKeyForNonExistentUser() {
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(null);
        EasyMock.replay(mockUserService);
        
        resource.resetApiKey(authHeader, customerId, username);
    }
    
    @Test(expected = ForbiddenException.class)
    public void ShouldNotResetUserApiKeyIfNotAuthorized() {
        
        EasyMock.expect(mockUserService.getUser(customerId, username)).andReturn(testUser);
        EasyMock.replay(mockUserService);
        
        EasyMock.expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader)).andReturn(username);
        EasyMock.replay(mockOAuthService);
        
        EasyMock.expect(mockIDMAuthHelper.checkUserAuthorization(username, username, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkAdminAuthorizationForUser(username, customerId, "resetApiKey")).andReturn(false);
        EasyMock.expect(mockIDMAuthHelper.checkRackspaceEmployeeAuthorization(authHeader)).andReturn(false);
        mockIDMAuthHelper.handleAuthorizationFailure();
        EasyMock.expectLastCall().andThrow(new ForbiddenException());
        EasyMock.replay(mockIDMAuthHelper);
        
        resource.resetApiKey(authHeader, customerId, username);
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
