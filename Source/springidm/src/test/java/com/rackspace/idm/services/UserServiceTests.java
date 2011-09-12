package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.api.idm.v1.UserCredentials;
import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotAuthenticatedException;

public class UserServiceTests {

    UserDao mockUserDao;
    CustomerDao mockCustomerDao;
    ClientDao mockClientDao;
    UserService userService;
    UserService trustedUserService;
    AuthDao mockRackerDao;
    ClientService mockClientService;
    ScopeAccessService mockScopeAccessService;
    ScopeAccessDao mockScopeAccessObjectDao;

    String customerId = "123456";
    String username = "testuser";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";
    String apiKey = "1234567890";

    String uniqueId = "uniqueId";
    String middlename = "middle";
    String secretQuestion = "question";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    
    String secureId = "XXX";

    String customerDN = "o=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA,o=rackspace,dc=rackspace,dc=com";

    String userUniqueId = "uniqueId";

    String tokenString = "XXXX";
    String callbackUrl = "www.cp.com";

    String nastId = "nastId";
    int mossoId = 6676;

    int limit = 100;
    int offset = 1;
    int totalRecords = 0;

    Customer testCustomer;
    User testUser;

    String rackerId = "rackerId";

    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);
        mockClientService = EasyMock.createMock(ClientService.class);
        mockScopeAccessObjectDao = EasyMock.createMock(ScopeAccessDao.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        appConfig.setProperty("ldap.server.trusted", false);

        userService = new DefaultUserService(mockUserDao, mockRackerDao,
                mockCustomerDao,mockScopeAccessObjectDao,
                mockClientService, appConfig);
        
        Configuration appConfig2 = new PropertyFileConfiguration().getConfigFromClasspath();
        
        appConfig2.setProperty("ldap.server.trusted", true);
        trustedUserService = new DefaultUserService(mockUserDao, mockRackerDao,
                mockCustomerDao,mockScopeAccessObjectDao,
                mockClientService, appConfig2);
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
    public void shouldAddUserToExistingCustomer() throws DuplicateException {
        final User user = getFakeUser();
        final Customer customer = getFakeCustomer();
        customer.setUniqueId(customerDN);
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
        .andReturn(customer);
        EasyMock.replay(mockCustomerDao);
        EasyMock.expect(
                mockUserDao.getUnusedUserInum((String) EasyMock.anyObject()))
                .andReturn("@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA!3333.3333");
        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
        .andReturn(true);
        mockUserDao.addUser((User) EasyMock.anyObject(), EasyMock.eq(customerDN));
        EasyMock.replay(mockUserDao);
        userService.addUser(user);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAddUserToNonExistentCustomer()
    throws DuplicateException {
        final User user = getFakeUser();
        EasyMock.expect(mockCustomerDao.getCustomerByCustomerId(customerId))
        .andReturn(null);
        EasyMock.replay(mockCustomerDao);
        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
        .andReturn(true);
        EasyMock.replay(mockUserDao);
        userService.addUser(user);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddDuplicateUser() throws DuplicateException {
        final User user = getFakeUser();

        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
        .andReturn(false);
        EasyMock.replay(mockUserDao);

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

        EasyMock.expect(mockUserDao.getUserByNastId(nastId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        final User retrievedUser = userService.getUserByNastId(nastId);

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

        EasyMock.expect(mockUserDao.getUserByMossoId(mossoId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        final User retrievedUser = userService.getUserByMossoId(mossoId);

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
    public void shouldDeleteUser() {
        mockUserDao.deleteUser(username);
        EasyMock.replay(mockUserDao);


        final List<ClientGroup> groups = new ArrayList<ClientGroup>();
        final ClientGroup group = new ClientGroup("tempClient", customerId, "tempClient", "tempClient");
        groups.add(group);


        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(groups);
        mockClientService.removeUserFromClientGroup(username, group);
        EasyMock.replay(mockClientService);
        userService.deleteUser(username);
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientService);
    }

    @Test
    public void shouldUpdateUser() {
        final User user = getFakeUser();
        mockUserDao.updateUser(user, false);
        EasyMock.replay(mockUserDao);

        userService.updateUser(user, false);
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

    @Test
    public void shouldAuthenticateRacker() {

        final Racker racker = new Racker();
        racker.setRackerId(rackerId);
        racker.setUniqueId(uniqueId);
        EasyMock.expect(mockRackerDao.authenticate(rackerId, password))
        .andReturn(true);
        EasyMock.replay(mockRackerDao);

        EasyMock.expect(mockUserDao.getRackerByRackerId(rackerId)).andReturn(racker);
        EasyMock.replay(mockUserDao);


        final UserAuthenticationResult uaResult = trustedUserService.authenticateRacker(rackerId, password);
        Assert.assertTrue(uaResult.isAuthenticated());
        EasyMock.verify(mockRackerDao, mockUserDao);
    }

    @Test
    public void shouldAuthenticateUserByApiKey() {
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
        .andReturn(getTrueAuthenticationResult());



        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);

        final UserAuthenticationResult authenticated = userService
        .authenticateWithApiKey(username, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldAuthenticateUserByNastIdApiKey() {
        EasyMock.expect(
                mockUserDao.authenticateByNastIdAndAPIKey(nastId, apiKey))
                .andReturn(getTrueAuthenticationResult());



        EasyMock.replay(mockUserDao);

        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);

        final UserAuthenticationResult authenticated = userService
        .authenticateWithNastIdAndApiKey(nastId, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldAuthenticateUserByMossoIdApiKey() {
        EasyMock.expect(
                mockUserDao.authenticateByMossoIdAndAPIKey(mossoId, apiKey))
                .andReturn(getTrueAuthenticationResult());


        EasyMock.replay(mockUserDao);

        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);

        final UserAuthenticationResult authenticated = userService
        .authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusActive() {

        final String statusStr = "active";
        final User user = getFakeUser();

        mockUserDao.updateUser(user,false);
        EasyMock.replay(mockUserDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusInActive() {

        final String statusStr = "inactive";
        final User user = getFakeUser();

        mockUserDao.updateUser(user, false);
        EasyMock.replay(mockUserDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUsersByCustomerId() {
        final List<User> users = new ArrayList<User>();
        users.add(getFakeUser());
        users.add(getFakeUser());

        final Users returnedUsers = new Users();
        returnedUsers.setLimit(limit);
        returnedUsers.setOffset(offset);
        returnedUsers.setTotalRecords(totalRecords);
        returnedUsers.setUsers(users);

        EasyMock
        .expect(mockUserDao.getUsersByCustomerId(customerId, offset, limit))
        .andReturn(returnedUsers);
        EasyMock.replay(mockUserDao);

        final Users returned = userService.getByCustomerId(customerId, offset, limit);

        Assert.assertTrue(returned.getUsers().size() == 2);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldSetUserPassword() {

        final User user = getFakeUser();
        final String customerId = user.getCustomerId();
        final String username = user.getUsername();
        final String tokenString = "aaldjfdj2231221";
        final DateTime tokenExpiration = new DateTime();

        getFakeClients();

        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
        token.setAccessTokenString(tokenString);
        token.setAccessTokenExp(tokenExpiration.toDate());
        token.setUsername(username);
        token.setUserRCN(customerId);
        token.setClientId("CLIENTID");

        final boolean isRecovery = true;

        final UserCredentials userCred = new UserCredentials();
        final UserPassword currentPass = new UserPassword();
        currentPass.setPassword("open-sesame");
        userCred.setCurrentPassword(currentPass);

        final UserPassword newPass = new UserPassword();
        newPass.setPassword("close-sesame");
        userCred.setNewPassword(newPass);

        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();

        final boolean isSelfUpdate = token.getUsername().equals(username);
        
        mockUserDao.updateUser(user, isSelfUpdate);

        EasyMock.replay(mockRackerDao);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        userService.setUserPassword(customerId, username, userCred, token, isRecovery);

        final User updatedUser = userService.getUser(username);

        final Password updatedPassword = updatedUser.getPasswordObj();
        final String updatedPasswordString = updatedPassword.getValue();

        Assert.assertEquals(newPass.getPassword(), updatedPasswordString);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotSetUserPasswordBecauseOriginalPasswordIsNull() {

        final User user = getFakeUser();
        final String customerId = user.getCustomerId();
        final String username = user.getUsername();
        final String tokenString = "aaldjfdj2231221";
        final DateTime tokenExpiration = new DateTime();

        getFakeClients();

        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
        token.setAccessTokenString(tokenString);
        token.setAccessTokenExp(tokenExpiration.toDate());
        token.setUsername(username);
        token.setUserRCN(customerId);
        token.setClientId("CLIENTID");

        final boolean isRecovery = false;

        final UserCredentials userCred = new UserCredentials();
        final UserPassword currentPass = new UserPassword();
        currentPass.setPassword(null);
        userCred.setCurrentPassword(currentPass);

        final UserPassword newPass = new UserPassword();
        newPass.setPassword("close-sesame");
        userCred.setNewPassword(newPass);

        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();

        final boolean isSelfUpdate = token.getUsername().equals(username);

        mockUserDao.updateUser(user, isSelfUpdate);

        EasyMock.replay(mockRackerDao);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotSetUserPasswordBecauseOriginalPasswordIsBlank() {

        final User user = getFakeUser();
        final String customerId = user.getCustomerId();
        final String username = user.getUsername();
        final String tokenString = "aaldjfdj2231221";
        final DateTime tokenExpiration = new DateTime();

        getFakeClients();

        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
        token.setAccessTokenString(tokenString);
        token.setAccessTokenExp(tokenExpiration.toDate());
        token.setUsername(username);
        token.setUserRCN(customerId);
        token.setClientId("CLIENTID");

        final boolean isRecovery = false;

        final UserCredentials userCred = new UserCredentials();
        final UserPassword currentPass = new UserPassword();
        currentPass.setPassword(" ");
        userCred.setCurrentPassword(currentPass);

        final UserPassword newPass = new UserPassword();
        newPass.setPassword("close-sesame");
        userCred.setNewPassword(newPass);

        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();

        final boolean isSelfUpdate = token.getUsername().equals(username);

        mockUserDao.updateUser(user, isSelfUpdate);

        EasyMock.replay(mockRackerDao);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
    }

    @Test(expected = NotAuthenticatedException.class)
    public void shouldNotSetUserPasswordBecauseUserIsNotAuthenticated() {

        final User user = getFakeUser();
        final String customerId = user.getCustomerId();
        final String username = user.getUsername();
        final String tokenString = "aaldjfdj2231221";
        final DateTime tokenExpiration = new DateTime();

        getFakeClients();

        final PasswordResetScopeAccess token = new PasswordResetScopeAccess();
        token.setAccessTokenString(tokenString);
        token.setAccessTokenExp(tokenExpiration.toDate());
        token.setUsername(username);
        token.setUserRCN(customerId);
        token.setClientId("CLIENTID");

        final boolean isRecovery = false;

        final UserCredentials userCred = new UserCredentials();
        final UserPassword currentPass = new UserPassword();
        currentPass.setPassword("open-sesame");
        userCred.setCurrentPassword(currentPass);

        final UserPassword newPass = new UserPassword();
        newPass.setPassword("close-sesame");
        userCred.setNewPassword(newPass);

        final UserAuthenticationResult userAuthenticationResult = new UserAuthenticationResult(user, false);

        EasyMock.expect(mockUserDao.authenticate(username, userCred
                .getCurrentPassword().getPassword())).andReturn(userAuthenticationResult);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();

        EasyMock.replay(mockRackerDao);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
    }

    private User getFakeUser() {

        final UserHumanName name = new UserHumanName(firstname, middlename, lastname);
        final UserLocale pref = new UserLocale(new Locale(preferredLang),
                DateTimeZone.forID(timeZone));
        final UserCredential cred = new UserCredential(userpass, secretQuestion,
                secretAnswer);
        final User user = new User(username, customerId, email, name, pref, cred);
        user.setInum("inum=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA.1234");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Customer getFakeCustomer() {
        return new Customer(customerId, "@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA",
                "@Rackspace.Testing", CustomerStatus.ACTIVE,
                "inum=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA",
        "inum=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA");
    }

    private List<Client> getFakeClients() {

        final Client client1 = new Client();
        client1.setInum("client1inum");
        final Client client2 = new Client();
        client2.setInum("client2inum");

        final List<Client> clients = new ArrayList<Client>();
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
