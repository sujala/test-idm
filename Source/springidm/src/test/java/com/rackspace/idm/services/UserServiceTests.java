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
import com.rackspace.idm.domain.entity.AccessToken;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.BaseClient;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientGroup;
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
import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.rackspace.idm.domain.service.AccessTokenService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.domain.service.EmailService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.jaxb.CustomParamsList;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.jaxb.UserCredentials;
import com.rackspace.idm.jaxb.UserPassword;

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
    AccessTokenService mockAccessTokenService;

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
        User user = getFakeUser();
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
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.isUsernameUnique(user.getUsername()))
            .andReturn(false);
        EasyMock.replay(mockUserDao);

        userService.addUser(user);
    }

    @Test
    public void shouldGetUser() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUser(username);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByNastId() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByNastId(nastId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByNastId(nastId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldGetUserByMossoId() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByMossoId(mossoId)).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByMossoId(mossoId);

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }
    
    @Test
    public void shouldGetUserByRPN() {
        User user = getFakeUser();

        EasyMock.expect(mockUserDao.getUserByRPN(user.getPersonId())).andReturn(user);
        EasyMock.replay(mockUserDao);

        User retrievedUser = userService.getUserByRPN(user.getPersonId());

        Assert.assertTrue(retrievedUser.getUsername().equals(username));
        EasyMock.verify(mockUserDao);
    }   
    
    @Test
    public void shouldDeleteUser() {
        mockUserDao.deleteUser(username);
        EasyMock.replay(mockUserDao);

        
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        ClientGroup group = new ClientGroup("tempClient", customerId, "tempClient", "tempClient");
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
        User user = getFakeUser();
        mockUserDao.updateUser(user, false);
        EasyMock.replay(mockUserDao);

        userService.updateUser(user, false);
    }
    
    @Test
    public void testingDateTime() {
     
        DateTime d = new DateTime();
        DateTime newD = d.plusDays(1000);
        
        String ds = d.toString();
        String newDs = newD.toString();
        
        
        //Assert.assertEquals(year, passwordExpirationDate.getYear());
        //Assert.assertEquals(monthOfYear, passwordExpirationDate.getMonthOfYear());
        //Assert.assertEquals(dayOfMonth + 10, passwordExpirationDate.getDayOfMonth());
    }
    
    

    @Test
    public void shouldAuthenticateUser() {
        EasyMock.expect(mockUserDao.authenticate(username, password)).andReturn(
            this.getTrueAuthenticationResult());
            
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(null);
        EasyMock.replay(mockClientService);
        
        EasyMock.replay(mockUserDao);
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
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);
        
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

        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);
        
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
        
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>());
        EasyMock.replay(mockClientService);  
        
        UserAuthenticationResult authenticated = userService
            .authenticateWithMossoIdAndApiKey(mossoId, apiKey);

        Assert.assertTrue(authenticated.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusActive() {

        String statusStr = "active";
        User user = getFakeUser();

        mockUserDao.updateUser(user,false);
        EasyMock.replay(mockUserDao);

        userService.updateUserStatus(user, statusStr);
        EasyMock.verify(mockUserDao);
    }

    @Test
    public void shouldUpdateUserStatusInActive() {

        String statusStr = "inactive";
        User user = getFakeUser();

        mockUserDao.updateUser(user, false);
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
            .expect(mockUserDao.getUsersByCustomerId(customerId, offset, limit))
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
    
    @Test
    public void shouldSetUserPassword() {
        
        User user = getFakeUser();
        String customerId = user.getCustomerId();
        String username = user.getUsername();
        String tokenString = "aaldjfdj2231221";
        DateTime tokenExpiration = new DateTime();
        
        List<Client> clients = getFakeClients();
        
        AccessToken token = new AccessToken(tokenString, tokenExpiration,
            user, clients.get(0), IDM_SCOPE.SET_PASSWORD);
        
        boolean isRecovery = true;
        
        UserCredentials userCred = new UserCredentials();
        UserPassword currentPass = new UserPassword();
        currentPass.setPassword("open-sesame");
        userCred.setCurrentPassword(currentPass);
        
        UserPassword newPass = new UserPassword();
        newPass.setPassword("close-sesame");
        userCred.setNewPassword(newPass);
        
        EasyMock.expect(mockRackerDao.authenticate(username, tokenString)).andReturn(true);
        
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
        EasyMock.expect(mockUserDao.getUserByCustomerIdAndUsername(customerId, username)).andReturn(user);
        EasyMock.expect(mockClientService.getClientGroupsForUser(username)).andReturn(new ArrayList<ClientGroup>()).atLeastOnce();
        
        boolean isSelfUpdate = token.getOwner().equals(username);
        
        mockUserDao.updateUser(user, isSelfUpdate);
        
        EasyMock.replay(mockRackerDao);
        EasyMock.replay(mockUserDao);
        EasyMock.replay(mockClientService);

        userService.setUserPassword(customerId, username, userCred, token, isRecovery);
        
        User updatedUser = userService.getUser(username);
        
        Password updatedPassword = updatedUser.getPasswordObj();
        String updatedPasswordString = updatedPassword.getValue();
        
        Assert.assertEquals(newPass.getPassword(), updatedPasswordString);
    }


    @Test(expected = IllegalStateException.class)
    public void shouldThrowErrorIfInumNullCreateAccessTokenForUser() {
        User user = getFakeUser();
        user.setInum("");

        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(user);
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
