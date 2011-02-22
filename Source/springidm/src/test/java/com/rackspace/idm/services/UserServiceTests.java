package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.rackspace.idm.entities.*;
import junit.framework.Assert;

import org.apache.commons.mail.EmailException;
import org.easymock.EasyMock;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.AuthDao;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.jaxb.CustomParamsList;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.test.stub.StubLogger;

public class UserServiceTests {

    UserDao mockUserDao;
    CustomerDao mockCustomerDao;
    AccessTokenDao mockTokenDao;
    RefreshTokenDao mockRefreshTokenDao;
    ClientDao mockClientDao;
    UserService userService;
    UserService trustedUserService;
    EmailService mockEmailService;
    AuthDao mockRackerDao;
    ClientService mockClientService;

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
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";

    String customerDN = "o=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA,o=rackspace,dc=rackspace,dc=com";

    String tokenString = "XXXX";
    String callbackUrl = "www.cp.com";

    String nastId = "nastId";
    int mossoId = 6676;

    int limit = 100;
    int offset = 1;
    int totalRecords = 0;

    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockEmailService = EasyMock.createMock(EmailService.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);
        mockClientService = EasyMock.createMock(ClientService.class);

        userService = new DefaultUserService(mockUserDao, mockRackerDao,
            mockCustomerDao, 
            mockEmailService, mockClientService, false, new StubLogger());

        trustedUserService = new DefaultUserService(mockUserDao, mockRackerDao,
            mockCustomerDao, 
            mockEmailService, mockClientService, true, new StubLogger());
    }

    @Test
    public void shouldAddUserToExistingCustomer() throws DuplicateException {
        User user = getFakeUser();
        Customer customer = getFakeCustomer();
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId))
            .andReturn(customer);
        EasyMock.expect(mockCustomerDao.getCustomerDnByCustomerId(customerId))
            .andReturn(customerDN);
        EasyMock.replay(mockCustomerDao);
        EasyMock.expect(
            mockUserDao.getUnusedUserInum((String) EasyMock.anyObject()))
            .andReturn("@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA!3333.3333");
        EasyMock
            .expect(
                mockUserDao
                    .findByInum("@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA!3333.3333"))
            .andReturn(user);
        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
            .andReturn(true);
        EasyMock.expect(mockUserDao.findByEmail(user.getEmail())).andReturn(
            null);
        mockUserDao.add((User) EasyMock.anyObject(), EasyMock.eq(customerDN));
        EasyMock.replay(mockUserDao);
        userService.addUser(user);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAddUserToNonExistentCustomer()
        throws DuplicateException {
        User user = getFakeUser();
        EasyMock.expect(mockCustomerDao.findByCustomerId(customerId))
            .andReturn(null);
        EasyMock.replay(mockCustomerDao);
        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
            .andReturn(true);
        EasyMock.expect(mockUserDao.findByEmail(user.getEmail())).andReturn(
            null);
        EasyMock.replay(mockUserDao);
        userService.addUser(user);
    }

    @Test(expected = DuplicateException.class)
    public void shouldNotAddDuplicateUser() throws DuplicateException {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
            .andReturn(false);
        EasyMock.replay(mockUserDao);

        userService.addUser(user);
    }

    @Test
    public void shouldGetUser() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUser(username);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByNastId() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.findByNastId(nastId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByNastId(nastId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByMossoId() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.findByMossoId(mossoId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByMossoId(mossoId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldDeleteUser() {
        mockUserDao.delete(username);
        userService.deleteUser(username);
    }

    @Test
    public void shouldUpdateUser() {
        User user = getFakeUser();
        EasyMock.expect(mockUserDao.findByEmail(user.getEmail())).andReturn(
            user);
        mockUserDao.save(user);
        EasyMock.replay(mockUserDao);

        userService.updateUser(user);
    }

    @Test
    public void shouldAuthenticateUser() {
        EasyMock.expect(mockUserDao.authenticate(username, password)).andReturn(
            this.getTrueAuthenticationResult());
        EasyMock.replay(mockUserDao);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(null);
        EasyMock.replay(mockClientService);
        UserAuthenticationResult uaResult = userService.authenticate(username, password);
        Assert.assertTrue(uaResult.isAuthenticated());
        EasyMock.verify(mockUserDao);
        EasyMock.verify(mockClientService);
    }

    @Test
    public void shouldNotAuthenticateUserThatDoestNotExist() {
        String badUsername = "badusername";
        
        EasyMock.expect(mockUserDao.authenticate(badUsername, password)).andReturn(
            this.getFalseAuthenticationResult());
        EasyMock.replay(mockUserDao);

        UserAuthenticationResult uaResult = userService.authenticate(badUsername, password);
        Assert.assertFalse(uaResult.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldAuthenticateRacker() {
        EasyMock.expect(mockRackerDao.authenticate("racker", password))
            .andReturn(true);
        EasyMock.replay(mockRackerDao);
        UserAuthenticationResult uaResult = trustedUserService.authenticate("racker", password);
        Assert.assertTrue(uaResult.isAuthenticated());
        EasyMock.verify(mockRackerDao);
    }

    @Test
    public void shouldAuthenticateUserByApiKey() {
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
            .andReturn(getTrueAuthenticationResult());
        EasyMock.replay(mockUserDao);

        UserAuthenticationResult authenticated = userService
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

        UserAuthenticationResult authenticated = userService
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

        UserAuthenticationResult authenticated = userService
            .authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusActive() {

        String statusStr = "active";
        User user = getFakeUser();

        mockUserDao.save(user);
        EasyMock.replay(mockUserDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusInActive() {

        String statusStr = "inactive";
        User user = getFakeUser();

        mockUserDao.save(user);
        EasyMock.replay(mockUserDao);

        List<Client> clients = getFakeClients();
        EasyMock.expect(mockClientDao.findAll()).andReturn(clients);
        EasyMock.replay(mockClientDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUsersByCustomerId() {
        List<User> users = new ArrayList<User>();
        users.add(getFakeUser());
        users.add(getFakeUser());

        Users returnedUsers = new Users();
        returnedUsers.setLimit(limit);
        returnedUsers.setOffset(offset);
        returnedUsers.setTotalRecords(totalRecords);
        returnedUsers.setUsers(users);

        EasyMock
            .expect(mockUserDao.findByCustomerId(customerId, offset, limit))
            .andReturn(returnedUsers);
        EasyMock.replay(mockUserDao);

        Users returned = userService.getByCustomerId(customerId, offset, limit);

        Assert.assertTrue(returned.getUsers().size() == 2);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldSendRecoveryEmail() {
        List<String> recipients = new ArrayList<String>();
        recipients.add(email);
        String from = "NoReply@rackspace.com";
        String link = String.format("%s?username=%s&token=%s", callbackUrl,
            username, tokenString);
        String message = String.format("Here's your recovery link: %s", link);
        String subject = "Password Recovery";
        try {
            mockEmailService.sendEmail(recipients, from, subject, message);
        } catch (EmailException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        PasswordRecovery recoveryParam = new PasswordRecovery();
        recoveryParam.setCallbackUrl(callbackUrl);
        recoveryParam.setCustomParams(new CustomParamsList());
        recoveryParam.setFrom(from);
        recoveryParam.setReplyTo(from);
        recoveryParam.setSubject(subject);
        recoveryParam.setTemplateUrl("");
        userService.sendRecoveryEmail(subject, email, recoveryParam,
            tokenString);
    }


    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorIfInumNullCreateAccessTokenForUser() {
        User user = getFakeUser();
        user.setInum("");

        EasyMock.expect(mockUserDao.findByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);
        userService.getUser(username);
    }

    private User getFakeUser() {

        UserHumanName name = new UserHumanName(firstname, middlename, lastname);
        UserLocale pref = new UserLocale(new Locale(preferredLang),
            DateTimeZone.forID(timeZone));
        UserCredential cred = new UserCredential(userpass, secretQuestion,
            secretAnswer);
        User user = new User(username, customerId, email, name, pref, cred);
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

        Client client1 = new Client();
        client1.setInum("client1inum");
        Client client2 = new Client();
        client2.setInum("client2inum");

        List<Client> clients = new ArrayList<Client>();
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
