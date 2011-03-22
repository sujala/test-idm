package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.apache.commons.mail.EmailException;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.RefreshTokenDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.EmailService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.jaxb.CustomParamsList;
import com.rackspace.idm.jaxb.PasswordRecovery;

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
    
    Customer testCustomer;
    User testUser;

    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockCustomerDao = EasyMock.createMock(CustomerDao.class);
        mockEmailService = EasyMock.createMock(EmailService.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);
        mockClientService = EasyMock.createMock(ClientService.class);

        userService = new DefaultUserService(mockUserDao, mockRackerDao,
            mockCustomerDao, 
            mockEmailService, mockClientService, false);

        trustedUserService = new DefaultUserService(mockUserDao, mockRackerDao,
            mockCustomerDao, 
            mockEmailService, mockClientService, true);
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
    public void shouldGetUserByRPN() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.findByRPN(user.getPersonId())).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByRPN(user.getPersonId());

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
        mockUserDao.save(user, false);
        EasyMock.replay(mockUserDao);

        userService.updateUser(user, false);
    }
    
    @Test
    public void shouldGetPasswordExpiryDate1() {
     
        User user = new User();
        user.setUsername(username);
        user.setPasswordRotationDuration(10);
 
        int year = 2011;
        int monthOfYear = 3;
        int dayOfMonth = 1;
        int hourOfDay = 1;
        int minuteOfHour = 1;
        int secondOfMinute = 1;
        int millisOfSecond = 1;
         
        DateTime lastUpdated = new DateTime(year, monthOfYear, dayOfMonth,
        hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
         
        Password passwordToUse = Password.existingInstance("testPass", lastUpdated, false);
         
        user.setPasswordObj(passwordToUse);
        user.setLastPasswordUpdateTimeStamp(lastUpdated);
       
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, false);
        DateTime passwordExpirationDate = uaResult.getPasswordExpirationDate();
        
        Assert.assertEquals(year, passwordExpirationDate.getYear());
        Assert.assertEquals(monthOfYear, passwordExpirationDate.getMonthOfYear());
        Assert.assertEquals(dayOfMonth + 10, passwordExpirationDate.getDayOfMonth());
    }
    
    @Test
    public void shouldGetPasswordExpiryDate2() {
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordRotationDuration(366);
 
        int year = 2011;
        int monthOfYear = 3;
        int dayOfMonth = 1;
        int hourOfDay = 1;
        int minuteOfHour = 1;
        int secondOfMinute = 1;
        int millisOfSecond = 1;
         
        DateTime lastUpdated = new DateTime(year, monthOfYear, dayOfMonth,
        hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
         
        Password passwordToUse = Password.existingInstance("testPass", lastUpdated, false);
         
        user.setPasswordObj(passwordToUse);
        user.setLastPasswordUpdateTimeStamp(lastUpdated);
       
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, false);
        DateTime passwordExpirationDate = uaResult.getPasswordExpirationDate();
        
        Assert.assertEquals(2012, passwordExpirationDate.getYear());
        Assert.assertEquals(monthOfYear, passwordExpirationDate.getMonthOfYear());
        Assert.assertEquals(dayOfMonth, passwordExpirationDate.getDayOfMonth());
    } 
    
    @Test
    public void shouldGetPasswordExpiryDate3() {
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordRotationDuration(365);
 
        int year = 2011;
        int monthOfYear = 3;
        int dayOfMonth = 1;
        int hourOfDay = 1;
        int minuteOfHour = 1;
        int secondOfMinute = 1;
        int millisOfSecond = 1;
         
        DateTime lastUpdated = new DateTime(year, monthOfYear, dayOfMonth,
        hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
         
        Password passwordToUse = Password.existingInstance("testPass", lastUpdated, false);
         
        user.setPasswordObj(passwordToUse);
        user.setLastPasswordUpdateTimeStamp(lastUpdated);
       
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, false);
        DateTime passwordExpirationDate = uaResult.getPasswordExpirationDate();
        
        Assert.assertEquals(2012, passwordExpirationDate.getYear());
        Assert.assertEquals(monthOfYear - 1, passwordExpirationDate.getMonthOfYear());
        Assert.assertEquals(29, passwordExpirationDate.getDayOfMonth());
    }
    
    @Test
    public void shouldGetPasswordExpiryDate4() {
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordRotationDuration(1827);
 
        int year = 2011;
        int monthOfYear = 3;
        int dayOfMonth = 1;
        int hourOfDay = 1;
        int minuteOfHour = 1;
        int secondOfMinute = 1;
        int millisOfSecond = 1;
         
        DateTime lastUpdated = new DateTime(year, monthOfYear, dayOfMonth,
        hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
         
        Password passwordToUse = Password.existingInstance("testPass", lastUpdated, false);
         
        user.setPasswordObj(passwordToUse);
        user.setLastPasswordUpdateTimeStamp(lastUpdated);
       
        UserAuthenticationResult uaResult = new UserAuthenticationResult(user, false);
        DateTime passwordExpirationDate = uaResult.getPasswordExpirationDate();
        
        Assert.assertEquals(2016, passwordExpirationDate.getYear());
        Assert.assertEquals(monthOfYear, passwordExpirationDate.getMonthOfYear());
        Assert.assertEquals(dayOfMonth, passwordExpirationDate.getDayOfMonth());
    }    

    @Test
    public void shouldAuthenticateUser() {
        EasyMock.expect(mockUserDao.authenticate(username, password)).andReturn(
            this.getTrueAuthenticationResult());
        
        setupUserAndCustomerWithPasswordRotationInfo(username);
        
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
        
        setupUserAndCustomerWithPasswordRotationInfo("racker");
        
        UserAuthenticationResult uaResult = trustedUserService.authenticate("racker", password);
        Assert.assertTrue(uaResult.isAuthenticated());
        EasyMock.verify(mockRackerDao);
    }

    @Test
    public void shouldAuthenticateUserByApiKey() {
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
            .andReturn(getTrueAuthenticationResult());
        
        setupUserAndCustomerWithPasswordRotationInfo(username);

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
        
        setupUserAndCustomerWithPasswordRotationInfo(username);

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
        
        setupUserAndCustomerWithPasswordRotationInfo(username);
        
        UserAuthenticationResult authenticated = userService
            .authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusActive() {

        String statusStr = "active";
        User user = getFakeUser();

        mockUserDao.save(user,false);
        EasyMock.replay(mockUserDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusInActive() {

        String statusStr = "inactive";
        User user = getFakeUser();

        mockUserDao.save(user, false);
        EasyMock.replay(mockUserDao);

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
    
    private void setupUserAndCustomerWithPasswordRotationInfo(String passedUsername) {
        testUser = new User();
        testCustomer = new Customer();
        
        testCustomer.setCustomerId("RACKSPACE");
        testCustomer.setPasswordRotationDuration(10);
        
        EasyMock.expect(mockCustomerDao.findByCustomerId(testUser.getCustomerId())).andReturn(testCustomer);
        EasyMock.replay(mockCustomerDao);
        EasyMock.expect(mockUserDao.findByUsername(passedUsername)).andReturn(testUser);
        EasyMock.replay(mockUserDao);
    }
}
