package com.rackspace.idm.domain.dao.impl;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.Racker;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;

public class LdapUserRepositoryTest {

    private LdapUserRepository repo;
    private LdapConnectionPools connPools;
    
    String rackerId = "racker";
    
    String id = "XXXX";

    private final String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,ou=customers,o=rackspace,dc=rackspace,dc=com";

    @BeforeClass
    public static void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapUserRepository cleanUpRepo = getRepo(pools);
        User deleteme = cleanUpRepo.getUserByUsername("deleteme");
        if (deleteme != null) {
            cleanUpRepo.deleteUser("deleteme");
        }
        User deleteme2 = cleanUpRepo.getUserByUsername("delete.me");
        if (deleteme2 != null) {
            cleanUpRepo.deleteUser("delete.me");
        }
        User deleteme3 = cleanUpRepo.getUserByUsername("delete,me");
        if (deleteme3 != null) {
            cleanUpRepo.deleteUser("delete,me");
        }
        pools.close();
    }

    private static LdapUserRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapUserRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        LdapConfiguration config = new LdapConfiguration(appConfig);
        return config.connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
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
            repo.getUserByNastId(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getUserByNastId("     ");
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
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.authenticateByAPIKey(null, "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.authenticateByNastIdAndAPIKey(null, "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
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
    public void shouldFindOneUserThatExistsByNastId() {
        User newUser = addNewTestUser();
        User user = repo.getUserByNastId("TESTNASTID");
        Assert.assertNotNull(user);
        Assert.assertEquals("deleteme", user.getUsername());

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldFindOneUserThatExistsByMossoId() {
        User newUser = addNewTestUser();
        User user = repo.getUserByMossoId(88888);
        Assert.assertNotNull(user);
        Assert.assertEquals("deleteme", user.getUsername());

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
        User user = repo.getUserByNastId("NOTAREALNASTID");
        Assert.assertNull(user);
    }

    @Test
    public void shouldNotFindOneUserThatDoesNotExistsByMossoId() {
        User user = repo.getUserByMossoId(0);
        Assert.assertNull(user);
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

    @Test
    public void shouldAddDeleteRestoreUser() {

        User newUser = addNewTestUser();

        newUser.setSoftDeleted(true);

        repo.updateUser(newUser, false);

        User deletedUser = repo.getUserByUsername(newUser.getUsername());
        User notFound = repo.getUserByCustomerIdAndUsername(
            newUser.getCustomerId(), newUser.getUsername());

        Assert.assertNotNull(deletedUser);
        Assert.assertNull(notFound);

        deletedUser.setSoftDeleted(false);

        repo.updateUser(deletedUser, false);

        User restoredUser = repo.getUserByCustomerIdAndUsername(
            deletedUser.getCustomerId(), deletedUser.getUsername());
        Assert.assertNotNull(restoredUser);

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldAddTimeStampWhenUserIsSoftDeleted() {

        User newUser = addNewTestUser();
        newUser.setSoftDeleted(true);

        repo.updateUser(newUser, false);

        User softDeletedUser = repo.getUserByUsername(newUser.getUsername());

        Assert.assertNotNull(softDeletedUser.getSoftDeleteTimestamp());

        softDeletedUser.setSoftDeleted(false);

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
        Users users = repo.getAllUsers(0, 100);
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
    public void shouldAuthenticateByNastIdAndAPIKey() {
        User newUser = addNewTestUser();
        UserAuthenticationResult authenticated = repo
            .authenticateByNastIdAndAPIKey("TESTNASTID", "XXX");
        Assert.assertTrue(authenticated.isAuthenticated());
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldAuthenticateByMossoIdAndAPIKey() {
        User newUser = addNewTestUser();
        UserAuthenticationResult authenticated = repo
            .authenticateByMossoIdAndAPIKey(88888, "XXX");
        Assert.assertTrue(authenticated.isAuthenticated());
        repo.deleteUser(newUser.getUsername());
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
    public void shouldSetAllUsersLocked() {
        User newUser = addNewTestUser();
        repo.setUsersLockedFlagByCustomerId(newUser.getCustomerId(), true);
        User changedUser = repo.getUserByUsername(newUser.getUsername());        
        repo.setUsersLockedFlagByCustomerId(newUser.getCustomerId(), false);
        User ReChangedUser = repo.getUserByUsername(newUser.getUsername());
        repo.deleteUser(newUser.getUsername());
        
        Assert.assertEquals(changedUser.isLocked(), true);
        Assert.assertEquals(ReChangedUser.isLocked(), false);
    }

    @Test
    public void shouldFindByCustomerID() {
        User user = addNewTestUser();
        Users users = repo.getUsersByCustomerId("RACKSPACE", 0, 200);
        repo.deleteUser(user.getUsername());
        Assert.assertTrue(users.getLimit() == 200);
        Assert.assertTrue(users.getOffset() == 0);

        Assert.assertTrue(users.getTotalRecords() >= 1);
        Assert.assertTrue(users.getUsers().size() >= 1);
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
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setRegion("ORD");
        newUser.setSoftDeleted(false);
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
            "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE", "XXX", UserStatus.ACTIVE,
            "RPN-111-222-333");
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
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setDefaults();
        newUser.setId(id);
        return newUser;
    }

    @After
    public void tearDown() {
        connPools.close();
    }
}
