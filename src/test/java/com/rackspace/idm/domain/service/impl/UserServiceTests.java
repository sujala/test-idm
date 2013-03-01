package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.Validator;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.unboundid.ldap.sdk.Filter;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserServiceTests {

    UserDao mockUserDao;
    CustomerService mockCustomerService;
    UserService userService;
    UserService trustedUserService;
    DomainService mockDomainService;
    AuthDao mockRackerDao;
    ApplicationService mockClientService;
    ScopeAccessService mockScopeAccessService;
    PasswordComplexityService mockPasswordComplexityService;
    CloudRegionService cloudRegionService;
    TenantService tenantService;
    Validator validator = new Validator();

    String customerId = "123456";
    String username = "testuser";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";
    String apiKey = "1234567890";

    String middlename = "middle";
    String secretQuestion = "question";
    String secretQuestionId = "id";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    
    String id = "XXX";
    
    String secureId = "XXX";

    String customerDN = "o=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA,o=rackspace,dc=rackspace,dc=com";

    String nastId = "nastId";
    int mossoId = 6676;
    String domainId = "";

    int limit = 100;

    String rackerId = "rackerId";

    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockCustomerService = EasyMock.createMock(CustomerService.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);
        mockClientService = EasyMock.createMock(ApplicationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        mockDomainService = EasyMock.createMock(DomainService.class);
        mockPasswordComplexityService = EasyMock.createMock(PasswordComplexityService.class);
        cloudRegionService = EasyMock.createMock(DefaultCloudRegionService.class);
        validator = EasyMock.createMock(Validator.class);
        tenantService = EasyMock.createMock(TenantService.class);


        Configuration appConfig = new PropertyFileConfiguration().getConfig();
        appConfig.setProperty("ldap.server.trusted", false);

        userService = new DefaultUserService();
        userService.setUserDao(mockUserDao);
        userService.setAuthDao(mockRackerDao);
        userService.setScopeAccessService(mockScopeAccessService);
        userService.setApplicationService(mockClientService);
        userService.setConfig(appConfig);
        userService.setPasswordComplexityService(mockPasswordComplexityService);
        userService.setCloudRegionService(cloudRegionService);
        userService.setValidator(validator);
        userService.setTenantService(tenantService);

        Configuration appConfig2 = new PropertyFileConfiguration().getConfig();
        
        appConfig2.setProperty("ldap.server.trusted", true);

        trustedUserService =  new DefaultUserService();

        trustedUserService.setUserDao(mockUserDao);
        trustedUserService.setAuthDao(mockRackerDao);
        trustedUserService.setScopeAccessService(mockScopeAccessService);
        trustedUserService.setApplicationService(mockClientService);
        trustedUserService.setConfig(appConfig2);
        trustedUserService.setPasswordComplexityService(mockPasswordComplexityService);
    }

    @Test
    public void shouldAddRacker() {
        final Racker racker = new Racker();
        racker.setRackerId(rackerId);
        EasyMock.expect(mockUserDao.getRackerByRackerId(rackerId)).andReturn(null);
        mockUserDao.addRacker(racker);
        EasyMock.replay(mockUserDao);
        userService.addRacker(racker);
        EasyMock.verify(mockUserDao);
    }

    @Test(expected=DuplicateException.class)
    public void shouldNotAddRackerForDuplicate() {
        final Racker racker = new Racker();
        racker.setRackerId(rackerId);
        EasyMock.expect(mockUserDao.getRackerByRackerId(rackerId)).andReturn(racker);
        EasyMock.replay(mockUserDao);
        userService.addRacker(racker);
    }

    @Test
    public void shouldGetRacker() {
        final Racker racker = new Racker();
        racker.setRackerId(rackerId);
        EasyMock.expect(mockUserDao.getRackerByRackerId(rackerId)).andReturn(racker);
        EasyMock.replay(mockUserDao);
        userService.getRackerByRackerId(rackerId);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldDeleteRacker() {
        mockUserDao.deleteRacker(rackerId);
        EasyMock.replay(mockUserDao);
        userService.deleteRacker(rackerId);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldAddUser() throws DuplicateException {
    	//setup
        final UserScopeAccess usa = getFakeUserScopeAccess();
        final User user = getFakeUser();
        final Domain domain = getFakeDomain();
        final Customer customer = getFakeCustomer();
        customer.setUniqueId(customerDN);
        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername())).andReturn(true);
        EasyMock.expect(mockUserDao.getNextUserId()).andReturn(id);
        EasyMock.expect(mockPasswordComplexityService.checkPassword(user.getPassword())).andReturn(new PasswordComplexityResult());
        EasyMock.expect(validator.isEmpty(EasyMock.anyObject(String.class))).andReturn(true);
        mockUserDao.addUser(user);

        EasyMock.expect(mockScopeAccessService.addDirectScopeAccess(EasyMock.anyObject(String.class), EasyMock.anyObject(ScopeAccess.class))).andReturn(getFakeUserScopeAccess());
        EasyMock.expect(mockScopeAccessService.addDirectScopeAccess(EasyMock.anyObject(String.class), EasyMock.anyObject(ScopeAccess.class))).andReturn(getFakeUserScopeAccess());

        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockPasswordComplexityService);
        EasyMock.replay(mockScopeAccessService);
        EasyMock.replay(validator);

        Region region = new Region();
        region.setName("DFW");
        EasyMock.expect(cloudRegionService.getDefaultRegion(EasyMock.anyObject(String.class))).andReturn(region);
        EasyMock.replay(cloudRegionService);

        //executions
        userService.addUser(user);
    }

    @Test(expected = DuplicateUsernameException.class)
    public void shouldNotAddDuplicateUser() throws DuplicateException {
        final User user = getFakeUser();

        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
        .andReturn(false);
        EasyMock.expect(validator.isEmpty(EasyMock.anyObject(String.class))).andReturn(true);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(validator);

        userService.addUser(user);
    }

    @Test
    public void shouldGetUser() {
        final User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);

        final User retrievedUser = userService.getUser(username);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByNastId() {
        final User user = getFakeUser();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("someName");
        tenantRole.setUserId("1");
        tenantRoles.add(tenantRole);
        List<Filter> filterList = new ArrayList<Filter>();
        filterList.add(Filter.createEqualityFilter("rsId", "1"));

        EasyMock.expect(mockUserDao.getUsers(filterList)).andReturn(users);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(tenantService.getTenantRolesForTenant("nastId")).andReturn(tenantRoles);
        EasyMock.replay(tenantService);

        final User retrievedUser = userService.getUserByTenantId(nastId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }
    
    @Test
    public void shouldGetUserBySecureId() {
        final User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserBySecureId(secureId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        final User retrievedUser = userService.getUserBySecureId(secureId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByMossoId() {
        final User user = getFakeUser();
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("someName");
        tenantRole.setUserId("1");
        tenantRoles.add(tenantRole);
        List<Filter> filterList = new ArrayList<Filter>();
        filterList.add(Filter.createEqualityFilter("rsId", "1"));

        EasyMock.expect(mockUserDao.getUsers(filterList)).andReturn(users);
        EasyMock.replay(mockUserDao);
        EasyMock.expect(tenantService.getTenantRolesForTenant("6676")).andReturn(tenantRoles);
        EasyMock.replay(tenantService);

        final User retrievedUser = userService.getUserByTenantId(String.valueOf(mossoId));

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByRPN() {
        final User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByRPN(user.getPersonId())).andReturn(user);
        EasyMock.replay(mockUserDao);

        final User retrievedUser = userService.getUserByRPN(user.getPersonId());

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void testingDateTime() {

        final DateTime d = new DateTime();
        final DateTime newD = d.plusDays(1000);

        d.toString();
        newD.toString();


        //Assert.assertEquals(year, passwordExpirationDate.getYear());
        //Assert.assertEquals(monthOfYear, passwordExpirationDate.getMonthOfYear());
        //Assert.assertEquals(dayOfMonth + 10, passwordExpirationDate.getDayOfMonth());
    }



    @Test
    public void shouldAuthenticateUser() {
        EasyMock.expect(mockUserDao.authenticate(username, password)).andReturn(
                this.getTrueAuthenticationResult());

        EasyMock.replay(mockUserDao);
        final UserAuthenticationResult uaResult = userService.authenticate(username, password);
        Assert.assertTrue(uaResult.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldNotAuthenticateUserThatDoestNotExist() {
        final String badUsername = "badusername";

        EasyMock.expect(mockUserDao.authenticate(badUsername, password)).andReturn(
                this.getFalseAuthenticationResult());
        EasyMock.replay(mockUserDao);

        final UserAuthenticationResult uaResult = userService.authenticate(badUsername, password);
        Assert.assertFalse(uaResult.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

//    @Test
//    public void shouldAuthenticateRacker() {
//
//        final Racker racker = new Racker();
//        racker.setRackerId(rackerId);
//        racker.setUniqueId(uniqueId);
//        EasyMock.expect(mockRackerDao.authenticate(rackerId, password))
//        .andReturn(true);
//        EasyMock.replay(mockRackerDao);
//
//        EasyMock.expect(mockUserDao.getRackerByRackerId(rackerId)).andReturn(racker);
//        EasyMock.replay(mockUserDao);
//
//
//        final UserAuthenticationResult uaResult = trustedUserService.authenticateRacker(rackerId, password);
//        Assert.assertTrue(uaResult.isAuthenticated());
//        EasyMock.verify(mockRackerDao, mockUserDao);
//    }

    @Test
    public void shouldAuthenticateUserByApiKey() {
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
        .andReturn(getTrueAuthenticationResult());



        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        final UserAuthenticationResult authenticated = userService
        .authenticateWithApiKey(username, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

//    @Test
//    public void shouldGetUsersByCustomerId() {
//        final List<User> users = new ArrayList<User>();
//        users.add(getFakeUser());
//        users.add(getFakeUser());
//
//        final Users returnedUsers = new Users();
//        returnedUsers.setLimit(limit);
//        returnedUsers.setOffset(offset);
//        returnedUsers.setTotalRecords(totalRecords);
//        returnedUsers.setUsers(users);
//
//        EasyMock
//        .expect(mockUserDao.getUsersByCustomerId(customerId, offset, limit))
//        .andReturn(returnedUsers);
//        EasyMock.replay(mockUserDao);
//
//        final Users returned = userService.getByCustomerId(customerId, offset, limit);
//
//        Assert.assertTrue(returned.getUsers().size() == 2);
//        EasyMock.verify(mockUserDao);
//    }

//    @Test
//    public void shouldSetUserPassword() {
//
//        final User user = getFakeUser();
//        final String customerId = user.getCustomerId();
//        final String username = user.getUsername();
//        final String tokenString = "aaldjfdj2231221";
//        final DateTime tokenExpiration = new DateTime();
//
//        getFakeClients();
//
//        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
//        token.setAccessTokenString(tokenString);
//        token.setAccessTokenExp(tokenExpiration.toDate());
//        token.setUsername(username);
//        token.setUserRCN(customerId);
//        token.setClientId("CLIENTID");
//
//        final boolean isRecovery = true;
//
//        final UserCredentials userCred = new UserCredentials();
//        final UserPassword currentPass = new UserPassword();
//        currentPass.setPassword("open-sesame");
//        userCred.setCurrentPassword(currentPass);
//
//        final UserPassword newPass = new UserPassword();
//        newPass.setPassword("close-sesame");
//        userCred.setNewPassword(newPass);
//
//        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);
//
//        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
//        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
//        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();
//
//        final boolean isSelfUpdate = token.getUsername().equals(username);
//        
//        mockUserDao.updateUser(user, isSelfUpdate);
//
//        EasyMock.replay(mockRackerDao);
//        EasyMock.replay(mockUserDao);
//        EasyMock.replay(mockClientService);
//
//        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
//
//        final User updatedUser = userService.getUser(username);
//
//        final Password updatedPassword = updatedUser.getPasswordObj();
//        final String updatedPasswordString = updatedPassword.getValue();
//
//        Assert.assertEquals(newPass.getPassword(), updatedPasswordString);
//    }

//    @Test(expected = BadRequestException.class)
//    public void shouldNotSetUserPasswordBecauseOriginalPasswordIsNull() {
//
//        final User user = getFakeUser();
//        final String customerId = user.getCustomerId();
//        final String username = user.getUsername();
//        final String tokenString = "aaldjfdj2231221";
//        final DateTime tokenExpiration = new DateTime();
//
//        getFakeClients();
//
//        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
//        token.setAccessTokenString(tokenString);
//        token.setAccessTokenExp(tokenExpiration.toDate());
//        token.setUsername(username);
//        token.setUserRCN(customerId);
//        token.setClientId("CLIENTID");
//
//        final boolean isRecovery = false;
//
//        final UserCredentials userCred = new UserCredentials();
//        final UserPassword currentPass = new UserPassword();
//        currentPass.setPassword(null);
//        userCred.setCurrentPassword(currentPass);
//
//        final UserPassword newPass = new UserPassword();
//        newPass.setPassword("close-sesame");
//        userCred.setNewPassword(newPass);
//
//        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);
//
//        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
//        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
//        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();
//
//        final boolean isSelfUpdate = token.getUsername().equals(username);
//
//        mockUserDao.updateUser(user, isSelfUpdate);
//
//        EasyMock.replay(mockRackerDao);
//        EasyMock.replay(mockUserDao);
//        EasyMock.replay(mockClientService);
//
//        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
//    }
//
//    @Test(expected = BadRequestException.class)
//    public void shouldNotSetUserPasswordBecauseOriginalPasswordIsBlank() {
//
//        final User user = getFakeUser();
//        final String customerId = user.getCustomerId();
//        final String username = user.getUsername();
//        final String tokenString = "aaldjfdj2231221";
//        final DateTime tokenExpiration = new DateTime();
//
//        getFakeClients();
//
//        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
//        token.setAccessTokenString(tokenString);
//        token.setAccessTokenExp(tokenExpiration.toDate());
//        token.setUsername(username);
//        token.setUserRCN(customerId);
//        token.setClientId("CLIENTID");
//
//        final boolean isRecovery = false;
//
//        final UserCredentials userCred = new UserCredentials();
//        final UserPassword currentPass = new UserPassword();
//        currentPass.setPassword(" ");
//        userCred.setCurrentPassword(currentPass);
//
//        final UserPassword newPass = new UserPassword();
//        newPass.setPassword("close-sesame");
//        userCred.setNewPassword(newPass);
//
//        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);
//
//        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
//        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
//        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();
//
//        final boolean isSelfUpdate = token.getUsername().equals(username);
//
//        mockUserDao.updateUser(user, isSelfUpdate);
//
//        EasyMock.replay(mockRackerDao);
//        EasyMock.replay(mockUserDao);
//        EasyMock.replay(mockClientService);
//
//        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
//    }
//
//    @Test(expected = NotAuthenticatedException.class)
//    public void shouldNotSetUserPasswordBecauseUserIsNotAuthenticated() {
//
//        final User user = getFakeUser();
//        final String customerId = user.getCustomerId();
//        final String username = user.getUsername();
//        final String tokenString = "aaldjfdj2231221";
//        final DateTime tokenExpiration = new DateTime();
//
//        getFakeClients();
//
//        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
//        token.setAccessTokenString(tokenString);
//        token.setAccessTokenExp(tokenExpiration.toDate());
//        token.setUsername(username);
//        token.setUserRCN(customerId);
//        token.setClientId("CLIENTID");
//
//        final boolean isRecovery = false;
//
//        final UserCredentials userCred = new UserCredentials();
//        final UserPassword currentPass = new UserPassword();
//        currentPass.setPassword("open-sesame");
//        userCred.setCurrentPassword(currentPass);
//
//        final UserPassword newPass = new UserPassword();
//        newPass.setPassword("close-sesame");
//        userCred.setNewPassword(newPass);
//
//        final UserAuthenticationResult userAuthenticationResult = new UserAuthenticationResult(user, false);
//
//        EasyMock.expect(mockUserDao.authenticate(username, userCred
//                .getCurrentPassword().getPassword())).andReturn(userAuthenticationResult);
//        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();
//
//        EasyMock.replay(mockRackerDao);
//        EasyMock.replay(mockUserDao);
//        EasyMock.replay(mockClientService);
//
//        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
//    }
//


    private Domain getFakeDomain() {
        final Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        return domain;
    }

    private User getFakeUser() {

        final UserHumanName name = new UserHumanName(firstname, middlename, lastname);
        final UserLocale pref = new UserLocale(new Locale(preferredLang), DateTimeZone.forID(timeZone));
        final UserCredential cred = new UserCredential(userpass, secretQuestion,  secretAnswer, secretQuestionId);
        final User user = new User(username, customerId, email, name, pref, cred);
        user.setMossoId(1);
        user.setDomainId("1");
        return user;
    }
    
    private UserScopeAccess getFakeUserScopeAccess() {
        UserScopeAccess usa = new UserScopeAccess();
        usa.setClientId(clientId);
        usa.setClientRCN(clientRCN);
        usa.setUsername(username);
        usa.setUserRCN(userRCN);
        usa.setUserRsId(userRsId);
        return usa;
    }
    
    String clientId = "clientId";
    String clientRCN = "clientRCN";
    String userRCN = "userRCN";
    String userRsId = "userRsId";

    private Customer getFakeCustomer() {
        Customer customer = new Customer();
        customer.setRcn(customerId);
        return customer;
    }

    private List<Application> getFakeClients() {

        final Application client1 = new Application();
        client1.setClientId("id1");
        final Application client2 = new Application();
        client2.setClientId("id2");

        final List<Application> clients = new ArrayList<Application>();
        clients.add(client1);
        clients.add(client2);

        return clients;
    }

    private UserAuthenticationResult getTrueAuthenticationResult() {
        return new UserAuthenticationResult(getFakeUser(), true);
    }

    private UserAuthenticationResult getFalseAuthenticationResult() {
        return new UserAuthenticationResult(null, false);
    }
}
