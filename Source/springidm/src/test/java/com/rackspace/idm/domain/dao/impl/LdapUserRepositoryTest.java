package com.rackspace.idm.domain.dao.impl;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.configuration.Configuration;
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
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Password;
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

    private final String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";

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
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapUserRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath());
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
            repo.getUserByInum(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getUserByInum("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addUser(null, "");
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

        try {
            repo.getUnusedUserInum(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneUserThatExistsByUsername() {
        User user = repo.getUserByUsername("mkovacs");
        Assert.assertNotNull(user);
        Assert.assertEquals("Kovacs", user.getLastname());
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
    public void shouldFindOneUserThatExistsByInum() {
        User mkovacs = repo.getUserByUsername("mkovacs");
        User user = repo.getUserByInum(mkovacs.getInum());
        Assert.assertNotNull(user);
        Assert.assertEquals("Kovacs", user.getLastname());
    }

    @Test
    public void shouldNotFindUserThatDoesNotExistByInum() {
        User user = repo.getUserByUsername("BADINUM");
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
    public void shouldGetUnusedUserInum() {
        String inum = repo.getUnusedUserInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        Assert.assertFalse(inum.equals("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111"));
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
        User notFound = repo.getUserByCustomerIdAndUsername(newUser.getCustomerId(), newUser.getUsername());

        Assert.assertNotNull(deletedUser);
        Assert.assertNull(notFound);

        deletedUser.setSoftDeleted(false);

        repo.updateUser(deletedUser, false);

        User restoredUser = repo.getUserByCustomerIdAndUsername(deletedUser.getCustomerId(), deletedUser.getUsername());
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

        User unSoftDeletedUser = repo.getUserByCustomerIdAndUsername(newUser.getCustomerId(), newUser.getUsername());

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

        newUser.setPasswordObj(Password.existingInstance("password", new DateTime(), false));

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
    public void shouldGenerateModifications() throws GeneralSecurityException, InvalidCipherTextException {
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
        String expectedPasswordValue = new Modification(ModificationType.REPLACE,
            LdapRepository.ATTR_PASSWORD, "newpassword!", newPassword.getValue()).getAttribute().getValue();
        Assert.assertEquals(expectedPasswordValue, mods.get(2).getAttribute().getValue());
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
        UserAuthenticationResult result = repo.authenticate("mkovacs", "P@$$w0rd");
        Assert.assertTrue(result.isAuthenticated());
    }

    @Test
    public void shouldAuthenticateByAPIKey() {
        UserAuthenticationResult authenticated = repo.authenticateByAPIKey("mkovacs", "1234567890");
        Assert.assertTrue(authenticated.isAuthenticated());
    }

    @Test
    public void shouldAuthenticateByNastIdAndAPIKey() {
        User newUser = addNewTestUser();
        UserAuthenticationResult authenticated = repo.authenticateByNastIdAndAPIKey("TESTNASTID", "XXX");
        Assert.assertTrue(authenticated.isAuthenticated());
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldAuthenticateByMossoIdAndAPIKey() {
        User newUser = addNewTestUser();
        UserAuthenticationResult authenticated = repo.authenticateByMossoIdAndAPIKey(88888, "XXX");
        Assert.assertTrue(authenticated.isAuthenticated());
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldNotAuthenticateForBadCredentials() {
        UserAuthenticationResult result = repo.authenticate("mkovacs", "bad password");
        Assert.assertFalse(result.isAuthenticated());
    }

    @Test
    public void shouldNotAuthenticateWithBadApiKey() {
        UserAuthenticationResult authenticated = repo.authenticateByAPIKey("mkovacs", "BadApiKey");
        Assert.assertFalse(authenticated.isAuthenticated());
    }

    @Test
    public void shouldAddNewUserWithPeriod() {
        User newUser = createTestUserInstanceWithPeriod();
        repo.addUser(newUser, testCustomerDN);
        User checkuser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldAddNewUserWithComma() {
        User newUser = createTestUserInstanceWithComma();
        repo.addUser(newUser, testCustomerDN);
        User checkuser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertNotNull(checkuser);
        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldSetAllUsersLocked() {
        User newUser = addNewTestUser();
        repo.setUsersLockedFlagByCustomerId(newUser.getCustomerId(), true);

        User changedUser = repo.getUserByUsername(newUser.getUsername());
        Assert.assertEquals(changedUser.isLocked(), true);

        repo.deleteUser(newUser.getUsername());
    }

    @Test
    public void shouldFindByCustomerID() {
        Users users = repo.getUsersByCustomerId("RACKSPACE", 0, 200);

        Assert.assertTrue(users.getLimit() == 200);
        Assert.assertTrue(users.getOffset() == 0);

        Assert.assertTrue(users.getTotalRecords() >= 1);
        Assert.assertTrue(users.getUsers().size() >= 1);
    }

    @Test
    public void shouldReturnTrueForMaxLoginFailures() {
        User newUser = addNewTestUser();

        for (int x = 1; x <= 10; x++) {
            Password password = Password.generateRandom(false);
            repo.authenticate(newUser.getUsername(), password.getValue());
        }
        newUser = repo.getUserByUsername(newUser.getUsername());
        repo.deleteUser(newUser.getUsername());

        Assert.assertTrue(newUser.isMaxLoginFailuresExceded());
    }

    @Test(expected = PasswordSelfUpdateTooSoonException.class)
    public void shouldNotAllowPasswordSelfUpdateWithin24Hours() {
        User newUser = addNewTestUser();
        Password pwd0 = Password.newInstance("newPassword0");
        newUser.setPasswordObj(pwd0);
        repo.updateUser(newUser, true);
        Password pwd1 = Password.newInstance("newPassword1");
        newUser.setPasswordObj(pwd1);
        repo.updateUser(newUser, true);
    }

    private User addNewTestUser() {
        User newUser = createTestUserInstance();
        repo.addUser(newUser, testCustomerDN);
        return newUser;
    }

    private User createTestUserInstance() {
        // Password pwd = Password.newInstance("password_to_delete");
        Password pwd = Password.generateRandom(false);
        User newUser = new User("deleteme", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!"));
        newUser.setApiKey("XXX");
        newUser.setCountry("USA");
        newUser.setPersonId("RPN-111-222-333");
        newUser.setDisplayName("MY DISPLAY NAME");
        newUser.setIname("@Rackspace.TestCustomer*deleteme");
        newUser.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5555");
        newUser.setOrgInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
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
        User newUser = new User("delete.me", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!"), "USA", "MY DISPLAY NAME", "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5556",
            "@Rackspace.TestCustomer*delete.me", "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE", "XXX", UserStatus.ACTIVE,
            "RPN-111-222-333");
        newUser.setDefaults();
        return newUser;
    }

    private User createTestUserInstanceWithComma() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("delete,me", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!"));
        newUser.setApiKey("XXX");
        newUser.setCountry("USA");
        newUser.setPersonId("RPN-111-222-333");
        newUser.setDisplayName("MY DISPLAY NAME");
        newUser.setIname("@Rackspace.TestCustomer*delete.me");
        newUser.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5557");
        newUser.setOrgInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setDefaults();
        return newUser;
    }

    @After
    public void tearDown() {
        connPools.close();
    }
}
