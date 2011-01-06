package com.rackspace.idm.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.config.LdapConfiguration;
import com.rackspace.idm.config.PropertyFileConfiguration;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.test.stub.StubLogger;
import com.unboundid.ldap.sdk.Modification;

public class LdapUserRepositoryTest {

    private LdapUserRepository repo;
    private LdapConnectionPools connPools;

    private final String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";

    @BeforeClass
    public static void cleanUpData() {
        LdapUserRepository cleanUpRepo = getRepo(getConnPools());
        User deleteme = cleanUpRepo.findByUsername("deleteme");
        if (deleteme != null) {
            cleanUpRepo.delete("deleteme");
        }
        User deleteme2 = cleanUpRepo.findByUsername("delete.me");
        if (deleteme2 != null) {
            cleanUpRepo.delete("delete.me");
        }
        User deleteme3 = cleanUpRepo.findByUsername("delete,me");
        if (deleteme3 != null) {
            cleanUpRepo.delete("delete,me");
        }
    }

    private static LdapUserRepository getRepo(LdapConnectionPools connPools) {
        return new LdapUserRepository(connPools, new StubLogger());
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath(),
            new StubLogger());
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
            repo.findByUsername(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByUsername("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByNastId(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByNastId("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByInum(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByInum("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.add(null, "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.delete("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.save(null);
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

        try {
            repo.getUnusedUserInum(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneUserThatExistsByUsername() {
        User user = repo.findByUsername("mkovacs");
        Assert.assertNotNull(user);
        Assert.assertEquals("Kovacs", user.getLastname());
    }

    @Test
    public void shouldFindOneUserThatExistsByNastId() {
        User newUser = addNewTestUser();
        User user = repo.findByNastId("TESTNASTID");
        Assert.assertNotNull(user);
        Assert.assertEquals("deleteme", user.getUsername());

        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldFindOneUserThatExistsByMossoId() {
        User newUser = addNewTestUser();
        User user = repo.findByMossoId(88888);
        Assert.assertNotNull(user);
        Assert.assertEquals("deleteme", user.getUsername());

        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldNotFindOneUserThatDoesNotExistsByNastId() {
        User user = repo.findByNastId("NOTAREALNASTID");
        Assert.assertNull(user);
    }

    @Test
    public void shouldNotFindOneUserThatDoesNotExistsByMossoId() {
        User user = repo.findByMossoId(0);
        Assert.assertNull(user);
    }

    @Test
    public void shouldFindOneUserThatExistsByEmail() {
        String testEmail = "test.user@rackspace.com";
        User user = repo.findByEmail(testEmail);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.getEmail(), testEmail);
    }

    @Test
    public void shouldNotFindUserThatDoesNotExistsByEmail() {
        User user = repo.findByEmail("bademail-shouldnotexist@example.com");
        Assert.assertNull(user);
    }

    @Test
    public void shouldNotFindUserThatDoesNotExistByUsername() {
        User user = repo.findByUsername("hi. i don't exist.");
        Assert.assertNull(user);
    }

    @Test
    public void shouldFindOneUserThatExistsByInum() {
        User user = repo.findByInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111");
        Assert.assertNotNull(user);
        Assert.assertEquals("Kovacs", user.getLastname());
    }

    @Test
    public void shouldNotFindUserThatDoesNotExistByInum() {
        User user = repo.findByUsername("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1112");
        Assert.assertNull(user);
    }

    @Test
    public void shouldAddNewUser() {
        User newUser = addNewTestUser();
        User checkuser = repo.findByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        cleanUpData();
    }

    @Test
    public void shouldGetUnusedUserInum() {
        String inum = repo.getUnusedUserInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        Assert.assertFalse(inum.equals("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111"));
    }

    @Test
    public void shouldDeleteUser() {
        User newUser = addNewTestUser();
        repo.delete(newUser.getUsername());
        User idontexist = repo.findByUsername(newUser.getUsername());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldAddDeleteRestoreUser() {

        User newUser = addNewTestUser();

        repo.delete(newUser.getUsername());

        Map<String, String> userStatusMap = new HashMap<String, String>();
        userStatusMap.put(GlobalConstants.ATTR_SOFT_DELETED, "FALSE");

        User deletedUser = repo.findUser(newUser.getCustomerId(),
            newUser.getUsername(), userStatusMap);
    }

    @Test
    public void shouldReturnTrueForIsUsernameUnique() {
        boolean isUnique = repo.isUsernameUnique("ThisUserNameDoesNotExist");
        Assert.assertTrue(isUnique);
    }

    @Test
    public void shouldReturnFalseForIsUsernameUnique() {
        boolean isUnique = repo.isUsernameUnique("mkovacs");
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

        try {
            repo.save(newUser);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        User changedUser = repo.findByUsername(userName);
        Assert.assertNotNull(changedUser);
        Assert.assertFalse(changedUser.getPasswordObj().isNew());

        // Update only one attribute
        String firstname = "anickname";
        newUser.setFirstname(firstname);

        try {
            repo.save(newUser);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        changedUser = repo.findByUsername(userName);
        Assert.assertEquals(newUser.getFirstname(), changedUser.getFirstname());

        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldUpdateUserPassword() {

        // create new user
        User newUser = addNewTestUser();
        String userName = newUser.getUsername();

        try {
            repo.save(newUser);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        // get user
        User changedUser = repo.findByUsername(userName);
        Assert.assertFalse(changedUser.getPasswordObj().isNew());

        // update password
        Password newpass = Password.newInstance("newpass");
        newUser.setPasswordObj(newpass);
        Assert.assertTrue(newUser.getPasswordObj().isNew());

        // save user
        try {
            repo.save(changedUser);
        } catch (IllegalStateException e) {
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        changedUser = repo.findByUsername(userName);
        Assert.assertNotNull(changedUser);

        // delete test user
        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldGenerateUserDn() {
        String dn = repo.getUserDnByUsername("mkovacs");
        Assert
            .assertEquals(
                "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=people,o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com",
                dn);
    }

    @Test
    public void shouldGenerateModifications() {
        User user = createTestUserInstance();
        User cUser = createTestUserInstance();
        cUser.setEmail("changed@deleteme.com");
        cUser.setFirstname("changed_first_name");
        cUser.setLastname("changed_last_name");
        cUser.setPasswordObj(Password.newInstance("newpassword!"));

        List<Modification> mods = repo.getModifications(user, cUser);

        Assert.assertEquals(4, mods.size());
        Assert.assertEquals("changed_first_name", mods.get(0).getAttribute()
            .getValue());
        Assert.assertEquals("changed@deleteme.com", mods.get(1).getAttribute()
            .getValue());
        Assert.assertEquals("changed_last_name", mods.get(2).getAttribute()
            .getValue());
        Assert.assertEquals("newpassword!", mods.get(3).getAttribute()
            .getValue());
    }

    @Test
    public void shouldRetrieveAllRecords() {
        User user = addNewTestUser();
        List<User> users = repo.findAll();
        Assert.assertTrue(users.size() > 1);
        repo.delete(user.getUsername());
    }

    // @Test
    public void shouldAuthenticateForCorrectCredentials() {
        boolean authenticated = repo.authenticate("mkovacs", "password");
        Assert.assertTrue(authenticated);
    }

    @Test
    public void shouldAuthenricateManyTimes() {
        for (int i = 0; i < 99; i++) {
            shouldAuthenticateForCorrectCredentials();
            System.out.println("auth count: " + (i + 1));
        }
    }

    @Test
    public void shouldAuthenticateByAPIKey() {
        boolean authenticated = repo.authenticateByAPIKey("mkovacs",
            "1234567890");
        Assert.assertTrue(authenticated);
    }

    @Test
    public void shouldAuthenticateByNastIdAndAPIKey() {
        User newUser = addNewTestUser();
        boolean authenticated = repo.authenticateByNastIdAndAPIKey(
            "TESTNASTID", "XXX");
        Assert.assertTrue(authenticated);
        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldAuthenticateByMossoIdAndAPIKey() {
        User newUser = addNewTestUser();
        boolean authenticated = repo.authenticateByMossoIdAndAPIKey(88888,
            "XXX");
        Assert.assertTrue(authenticated);
        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldNotAuthenticateForBadCredentials() {
        boolean authenticated = repo.authenticate("mkovacs", "bad password!");
        Assert.assertFalse(authenticated);
    }

    @Test
    public void shouldNotAuthenticateWithBadApiKey() {
        boolean authenticated = repo.authenticateByAPIKey("mkovacs",
            "BadApiKey");
        Assert.assertFalse(authenticated);
    }

    @Test
    public void shouldAddNewUserWithPeriod() {
        User newUser = createTestUserInstanceWithPeriod();
        repo.add(newUser, testCustomerDN);
        User checkuser = repo.findByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        cleanUpData();
    }

    @Test
    public void shouldAddNewUserWithComma() {
        User newUser = createTestUserInstanceWithComma();
        repo.add(newUser, testCustomerDN);
        User checkuser = repo.findByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        cleanUpData();
    }

    @Test
    public void shouldSetAllUsersLocked() {
        User newUser = addNewTestUser();
        repo.setAllUsersLocked(newUser.getCustomerId(), true);

        User changedUser = repo.findByUsername(newUser.getUsername());
        Assert.assertEquals(changedUser.getIsLocked(), true);

        repo.delete(newUser.getUsername());
    }

    @Test
    public void shouldFindByCustomerID() {
        Users users = repo.findByCustomerId("RACKSPACE", 0, 200);

        Assert.assertTrue(users.getLimit() == 200);
        Assert.assertTrue(users.getOffset() == 0);

        Assert.assertTrue(users.getTotalRecords() >= 1);
        Assert.assertTrue(users.getUsers().size() >= 1);
    }

    private User addNewTestUser() {
        User newUser = createTestUserInstance();
        repo.add(newUser, testCustomerDN);
        return newUser;
    }

    private User createTestUserInstance() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("deleteme", "RCN-DELETE-ME_NOW",
            "bademail@example.com", new UserHumanName("delete_my_firstname",
                "delete_my_middlename", "delete_my_lastname"), new UserLocale(
                Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd,
                "What is your favourite colur?", "Yellow. No, Blue! Arrrrgh!"));
        newUser.setApiKey("XXX");
        newUser.setCountry("USA");
        newUser.setPersonId("RPN-111-222-333");
        newUser.setDisplayName("MY DISPLAY NAME");
        newUser.setIname("@Rackspace.TestCustomer*deleteme");
        newUser.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5555");
        newUser.setOrgInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        newUser.setSeeAlso("inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setRegion("ORD");
        newUser.setSoftDeleted(false);
        newUser.setDefaults();
        newUser.setNastId("TESTNASTID");
        newUser.setMossoId(88888);
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
            "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE", "RPN-111-222-333");
        newUser.setDefaults();
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
        newUser.setIname("@Rackspace.TestCustomer*delete.me");
        newUser.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5557");
        newUser.setOrgInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        newUser.setSeeAlso("inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setDefaults();
        return newUser;
    }

    @After
    public void tearDown() {
        connPools.close();
    }
}