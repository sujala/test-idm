package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateUsernameException;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class LdapUserRepositoryIntegrationTest extends InMemoryLdapIntegrationTest{

    private static LdapUserRepository repo;
    private static LdapConnectionPools connPools;

    String rackerId = "racker";
    
    String id = "XXXX";

    private static LdapUserRepository getRepo(LdapConnectionPools connPools) {
        return new LdapUserRepository(connPools, new PropertyFileConfiguration().getConfig());
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(new PropertyFileConfiguration().getConfig());
        return config.connectionPools();
    }

    @BeforeClass
    public static void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }

    @Before
    public void preTestSetUp(){
        User deleteme = repo.getUserByUsername("deleteme");
        if (deleteme != null) {
            repo.deleteUser("deleteme");
        }
        User deleteme2 = repo.getUserByUsername("delete.me");
        if (deleteme2 != null) {
            repo.deleteUser("delete.me");
        }
        User deleteme3 = repo.getUserByUsername("delete,me");
        if (deleteme3 != null) {
            repo.deleteUser("delete,me");
        }
    }

    @AfterClass
    public static void tearDown() {
        connPools.close();
    }

    @Test
    public void shouldNotAcceptNullOrBlankUsernameOrInum() {
        try {
            repo.addRacker(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            repo.deleteRacker(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            repo.getRackerByRackerId(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            repo.getUserByUsername(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getUserByUsername("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getUserById(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getUserById("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addUser(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteUser("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.updateUser(null, false);
            Assert.fail("Should have thrown an exception!");
        } catch (BadRequestException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.authenticateByAPIKey(null, "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

    }

    @Test (expected = IllegalArgumentException.class)
    public void addRacker_rackerIsNull_throwsIllegalArgument() throws Exception {
        repo.addRacker(null);
    }
    
    @Test
    public void shouldAddFindDeleteRacker() {
        Racker racker = new Racker();
        racker.setRackerId(rackerId);
        
        repo.addRacker(racker);
        
        Racker gotRacker = repo.getRackerByRackerId(rackerId);
        
        repo.deleteRacker(rackerId);
        
        Assert.assertNotNull(gotRacker);
    }

    @Test
    public void shouldFindOneUserThatExistsByUsername() {
        User user = addNewTestUser();
        User returned = repo.getUserByUsername(user.getUsername());
        repo.deleteUser(user.getUsername());
        Assert.assertNotNull(user);
        Assert.assertEquals(returned.getLastname(), user.getLastname());
    }

    @Test
    public void shouldFindOneUsersThatExistsByNastId() {
        User newUser = addNewTestUser();
        Users users = repo.getUsersByNastId("TESTNASTID");
        Assert.assertNotNull(users);
        Assert.assertEquals("deleteme", users.getUsers().get(0).getUsername());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldFindOneUserThatExistsByMossoId() {
        User newUser = addNewTestUser();
        Users users = repo.getUsersByMossoId(88888);
        Assert.assertNotNull(users);
        Assert.assertEquals("deleteme", users.getUsers().get(0).getUsername());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldFindOneUserThatExistsByRPN() {
        User newUser = addNewTestUser();
        User user = repo.getUserByRPN(newUser.getPersonId());
        Assert.assertNotNull(user);
        Assert.assertEquals("deleteme", user.getUsername());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldNotFindOneUserThatDoesNotExistsByNastId() {
        Users users = repo.getUsersByNastId("NOTAREALNASTID");
        Assert.assertEquals(0, users.getUsers().size());
    }

    @Test
    public void shouldNotFindOneUserThatDoesNotExistsByMossoId() {
        Users users = repo.getUsersByMossoId(0);
        Assert.assertEquals(0, users.getUsers().size());
    }

    @Test
    public void shouldNotFindUserThatDoesNotExistByUsername() {
        User user = repo.getUserByUsername("hi. i don't exist.");
        Assert.assertNull(user);
    }

    @Test
    public void shouldAddNewUser() {
        User newUser = addNewTestUser();
        User checkuser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldDeleteUser() {
        User newUser = addNewTestUser();
        repo.deleteUser(newUser.getUsername());
        User idontexist = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNull(idontexist);
    }

    @Ignore("need to redo tests with new soft delete method")
    @Test
    public void shouldAddDeleteRestoreUser() {

        User newUser = addNewTestUser();

        repo.updateUser(newUser, false);

        User deletedUser = repo.getUserByUsername(newUser.getUsername());
        User notFound = repo.getUserByCustomerIdAndUsername(
            newUser.getCustomerId(), newUser.getUsername());

        Assert.assertNotNull(deletedUser);
        Assert.assertNull(notFound);

        repo.updateUser(deletedUser, false);

        User restoredUser = repo.getUserByCustomerIdAndUsername(
            deletedUser.getCustomerId(), deletedUser.getUsername());
        Assert.assertNotNull(restoredUser);

        repo.deleteUser(newUser.getUsername());
    }

    @Ignore("need to redo tests with new soft delete method")
    @Test
    public void shouldAddTimeStampWhenUserIsSoftDeleted() {

        User newUser = addNewTestUser();

        repo.updateUser(newUser, false);

        User softDeletedUser = repo.getUserByUsername(newUser.getUsername());

        Assert.assertNotNull(softDeletedUser.getSoftDeleteTimestamp());

        repo.updateUser(softDeletedUser, false);

        User unSoftDeletedUser = repo.getUserByCustomerIdAndUsername(
            newUser.getCustomerId(), newUser.getUsername());

        Assert.assertNull(unSoftDeletedUser.getSoftDeleteTimestamp());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldReturnTrueForIsUsernameUnique() {
        boolean isUnique = repo.isUsernameUnique("ThisUserNameDoesNotExist");
        Assert.assertTrue(isUnique);
    }

    @Test
    public void shouldReturnFalseForIsUsernameUnique() {
        User user = addNewTestUser();
        boolean isUnique = repo.isUsernameUnique(user.getUsername());
        repo.deleteUser(user.getUsername());
        Assert.assertFalse(isUnique);
    }

    @Test
    public void shouldUpdateNonDnAttrOfUser() {
        User newUser = addNewTestUser();
        String userName = newUser.getUsername();

        // Update all non-DN attributes
        newUser.setEmail("new.email@deleteme.com");
        newUser.setFirstname("new_first_name");
        newUser.setLastname("new_last_name");

        newUser.setPasswordObj(Password.existingInstance("password",
            new DateTime(), false));

        try {
            repo.updateUser(newUser, false);
        } catch (IllegalStateException e) {
            repo.deleteUser(newUser.getUsername());
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        User changedUser = repo.getUserByUsername(userName);
        Assert.assertNotNull(changedUser);
        Assert.assertFalse(changedUser.getPasswordObj().isNew());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldUpdateUserPassword() {

        // create new user
        User newUser = addNewTestUser();
        String userName = newUser.getUsername();

        // get user
        User changedUser = repo.getUserByUsername(userName);
        Assert.assertFalse(changedUser.getPasswordObj().isNew());

        // update password
        Password newpass = Password.newInstance("newpass");
        changedUser.setPasswordObj(newpass);
        Assert.assertTrue(changedUser.getPasswordObj().isNew());

        // save user
        try {
            repo.updateUser(changedUser, true);
        } catch (IllegalStateException e) {
            repo.deleteUser(newUser.getUsername());
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        changedUser = repo.getUserByUsername(userName);
        Assert.assertNotNull(changedUser);

        // delete test user
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldGenerateModifications() throws GeneralSecurityException,
        InvalidCipherTextException {
        User user = createTestUserInstance();
        User cUser = createTestUserInstance();
        cUser.setEmail("changed@deleteme.com");
        cUser.setFirstname("changed_first_name");
        cUser.setLastname("changed_last_name");
        Password newPassword = Password.newInstance("newpassword!");
        cUser.setPasswordObj(newPassword);

        List<Modification> mods = repo.getModifications(user, cUser, false);

        Assert.assertEquals(7, mods.size()); // 5 if the password hadn't been
                                             // changed
        String expectedPasswordValue = new Modification(
            ModificationType.REPLACE, LdapRepository.ATTR_PASSWORD,
            "newpassword!", newPassword.getValue()).getAttribute().getValue();
        Assert.assertEquals(expectedPasswordValue, mods.get(2).getAttribute()
                .getValue());
    }

    @Test
    @Ignore
    public void shouldRetrieveAllRecords() {
        User user = addNewTestUser();
        Users users = repo.getAllUsers(null, 0, 100);
        repo.deleteUser(user.getUsername());
        Assert.assertTrue(users.getUsers().size() > 1);
    }

    @Test
    public void shouldAuthenticateForCorrectCredentials() {
        User user = addNewTestUser();
        UserAuthenticationResult result = repo.authenticate(user.getUsername(),
                user.getPassword());
        repo.deleteUser(user.getUsername());
        Assert.assertTrue(result.isAuthenticated());
    }

    @Test
    public void shouldAuthenticateByAPIKey() {
        User user = addNewTestUser();
        UserAuthenticationResult authenticated = repo.authenticateByAPIKey(
                user.getUsername(), user.getApiKey());
        repo.deleteUser(user.getUsername());
        Assert.assertTrue(authenticated.isAuthenticated());
    }

    @Test
    public void shouldNotAuthenticateForBadCredentials() {
        User newUser = addNewTestUser();
        UserAuthenticationResult result = repo.authenticate(
                newUser.getUsername(), "bad password");
        repo.deleteUser(newUser.getUsername());
        Assert.assertFalse(result.isAuthenticated());
    }

    @Test
    public void shouldNotAuthenticateWithBadApiKey() {
        User newUser = addNewTestUser();
        UserAuthenticationResult authenticated = repo.authenticateByAPIKey(
                newUser.getUsername(), "BadApiKey");
        repo.deleteUser(newUser.getUsername());
        Assert.assertFalse(authenticated.isAuthenticated());
    }

    @Test
    public void shouldAddNewUserWithPeriod() {
        User newUser = createTestUserInstanceWithPeriod();
        repo.addUser(newUser);
        User checkuser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldAddNewUserWithComma() {
        User newUser = createTestUserInstanceWithComma();
        repo.addUser(newUser);
        User checkuser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        repo.deleteUser(newUser.getUsername());
    }
    
    @Test
    public void shouldFindBySecureID() {
        User user = addNewTestUser();
        user.setSecureId("XXX");
        repo.updateUser(user, false);
        
        User returnedUser = repo.getUserBySecureId("XXX");
        repo.deleteUser(user.getUsername());
        
        Assert.assertNotNull(returnedUser);
    }

    @Ignore //Unsuported by inmemoryldapserver
    @Test
    public void shouldReturnTrueForMaxLoginFailures() {
        User newUser = addNewTestUser();

        for (int x = 1; x <= 14; x++) {
            Password password = Password.generateRandom(false);
            repo.authenticate(newUser.getUsername(), password.getValue());
        }
        newUser = repo.getUserByUsername(newUser.getUsername());
        repo.deleteUser(newUser.getUsername());

        Assert.assertTrue(newUser.isMaxLoginFailuresExceded());
    }

    @Test
    public void shouldNotAllowPasswordSelfUpdateWithin24Hours() {
        User newUser = addNewTestUser();
        Password pwd0 = Password.newInstance("newPassword0");
        newUser.setPasswordObj(pwd0);
        repo.updateUser(newUser, true);
        Password pwd1 = Password.newInstance("newPassword1");
        newUser.setPasswordObj(pwd1);
        try {
            repo.updateUser(newUser, true);
        } catch (PasswordSelfUpdateTooSoonException ex) {
            repo.deleteUser(newUser.getUsername());
        }
    }
    
    @Test
    public void shouldSoftDeleteUser() {
        User newUser = addNewTestUser();
        repo.softDeleteUser(newUser);
        User notExists = repo.getUserById(newUser.getId());
        User softDeleted = repo.getSoftDeletedUserById(newUser.getId());
        repo.unSoftDeleteUser(softDeleted);
        User exists = repo.getUserById(newUser.getId());
        repo.deleteUser(newUser.getUsername());
        
        Assert.assertNull(notExists);
        Assert.assertNotNull(softDeleted);
        Assert.assertNotNull(exists);
    }

    private User addNewTestUser() {
        User newUser = createTestUserInstance();
        repo.addUser(newUser);
        return newUser;
    }

    private User createTestUserInstance() {
        // Password pwd = Password.newInstance("password_to_delete");
        Password pwd = Password.generateRandom(false);
        User newUser = new User("deleteme", "RCN-DELETE-ME_NOW",
            "bademail@example.com", new UserHumanName("delete_my_firstname",
                "delete_my_middlename", "delete_my_lastname"), new UserLocale(
                Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd,
                "What is your favourite colur?", "Yellow. No, Blue! Arrrrgh!"));
        newUser.setApiKey("XXX");
        newUser.setCustomerId("RACKSPACE");
        newUser.setCountry("USA");
        newUser.setPersonId("RPN-111-222-333");
        newUser.setDisplayName("MY DISPLAY NAME");
        newUser.setRegion("ORD");
        newUser.setDefaults();
        newUser.setNastId("TESTNASTID");
        newUser.setMossoId(88888);
        newUser.setId(id);
        return newUser;
    }

    private User createTestUserInstanceWithPeriod() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("delete.me", "RCN-DELETE-ME_NOW",
            "bademail@example.com", new UserHumanName("delete_my_firstname",
                "delete_my_middlename", "delete_my_lastname"), new UserLocale(
                Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd,
                "What is your favourite colur?", "Yellow. No, Blue! Arrrrgh!"),
            "USA", "MY DISPLAY NAME", "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5556",
            "@Rackspace.TestCustomer*delete.me",
            "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE", "XXX", "RPN-111-222-333");
        newUser.setDefaults();
        newUser.setId(id);
        return newUser;
    }

    private User createTestUserInstanceWithComma() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("delete,me", "RCN-DELETE-ME_NOW",
            "bademail@example.com", new UserHumanName("delete_my_firstname",
                "delete_my_middlename", "delete_my_lastname"), new UserLocale(
                Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd,
                "What is your favourite colur?", "Yellow. No, Blue! Arrrrgh!"));
        newUser.setApiKey("XXX");
        newUser.setCountry("USA");
        newUser.setPersonId("RPN-111-222-333");
        newUser.setDisplayName("MY DISPLAY NAME");
        newUser.setDefaults();
        newUser.setId(id);
        return newUser;
    }

}
