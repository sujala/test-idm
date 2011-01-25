package com.rackspace.idm.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.config.LdapConfiguration;
import com.rackspace.idm.config.PropertyFileConfiguration;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.test.stub.StubLogger;

public class RefreshTokenRepositoryTest {

    LdapRefreshTokenRepository repo;
    private LdapConnectionPools connPools;

    String owner = "someowner";
    private String requestor = "somerequestor";

    @BeforeClass
    public static void cleanUpData() {
        LdapRefreshTokenRepository cleanUpRepo = getRepo(getConnPools());
        RefreshToken deleteme = cleanUpRepo
            .findByTokenString("DELETE_My_Token");
        if (deleteme != null) {
            cleanUpRepo.delete("DELETE_My_Token");
        }
    }

    private static LdapRefreshTokenRepository getRepo(
        LdapConnectionPools connPools) {
        return new LdapRefreshTokenRepository(connPools, new StubLogger());
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration()
            .getConfigFromClasspath(), new StubLogger()).connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }

    @Test
    public void shouldNotAcceptNullOrBlankTokenString() {
        try {
            repo.findByTokenString(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByTokenString("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.delete("");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findTokenForOwner(null, "something", new DateTime());
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findTokenForOwner("something", null, new DateTime());
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneTokenThatExists() {
        RefreshToken addedToken = addNewTestToken();
        RefreshToken token = repo
            .findByTokenString(addedToken.getTokenString());
        Assert.assertNotNull(token);
        Assert
            .assertEquals(addedToken.getTokenString(), token.getTokenString());
    }

    @Test
    public void shouldNotFindTokenThatDoesNotExist() {
        RefreshToken token = repo.findByTokenString("hi. i don't exist.");
        Assert.assertNull(token);
    }

    @Test
    public void shouldRetrieveAllRecords() {
        RefreshToken token = addNewTestToken();
        List<RefreshToken> tokens = repo.findAll();

        Assert.assertTrue(tokens.size() > 0);

        repo.delete(token.getTokenString());
    }

    @Test
    public void shouldAddNewToken() {
        RefreshToken newToken = addNewTestToken();
        RefreshToken checkToken = repo.findByTokenString(newToken
            .getTokenString());
        Assert.assertNotNull(checkToken);
        Assert.assertEquals("DELETE_My_Token", checkToken.getTokenString());
        cleanUpData();
    }

    @Test
    public void shouldNotAcceptNullToken() {
        try {
            repo.save(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldDeleteToken() {
        RefreshToken newToken = addNewTestToken();
        repo.delete(newToken.getTokenString());
        RefreshToken idontexist = repo.findByTokenString(newToken
            .getTokenString());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldDeleteAllTokensForUser() {

        String clientId1 = "DELETE_THIS_TOKEN_CLIENT_1";
        String clientId2 = "DELETE_THIS_TOKEN_CLIENT_2";

        RefreshToken newToken1 = createTestTokenInstance();
        newToken1.setTokenString("DELETE_THIS_TOKEN_1");
        newToken1.setRequestor(clientId1);
        repo.save(newToken1);

        RefreshToken newToken2 = createTestTokenInstance();
        newToken2.setTokenString("DELETE_THIS_TOKEN_2");
        newToken2.setRequestor(clientId2);
        repo.save(newToken2);

        repo.deleteAllTokensForUser(owner);

        DateTime validAfter = new DateTime();
        RefreshToken token1 = repo.findTokenForOwner(owner, clientId1,
            validAfter);
        RefreshToken token2 = repo.findTokenForOwner(owner, clientId2,
            validAfter);

        Assert.assertNull(token1);
        Assert.assertNull(token2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotDeleteAllForUserIfEmptyUsername() {

        repo.deleteAllTokensForUser(StringUtils.EMPTY);
    }

    @Test
    public void shouldGenerateTokenDn() {
        RefreshToken addedToken = addNewTestToken();
        String dn = repo.getTokenDnByTokenstring(addedToken.getTokenString());
        String expectedDn = String.format("o=%s,ou=Tokens,dc=rackspace,dc=com",
            addedToken.getTokenString());
        Assert.assertEquals(dn, expectedDn);
    }

    @Test
    public void shouldFindUnexpiredToken() {
        RefreshToken addedToken = addNewTestToken();
        DateTime goodDate = new DateTime(2020, 6, 1, 11, 0, 0, 0);
        RefreshToken token = repo.findTokenForOwner(owner, requestor, goodDate);
        Assert.assertNotNull(token);
        repo.delete(addedToken.getTokenString());
    }

    @Test
    public void shouldNotFindExpiredToken() {
        DateTime badDate = new DateTime(2030, 6, 1, 11, 0, 0, 0);
        RefreshToken token = repo.findTokenForOwner(owner, requestor, badDate);
        Assert.assertNull(token);
    }

    @Test
    public void shouldUpdateToken() {

        RefreshToken token = addNewTestToken();

        String editedOwner = owner + "_UPDATED";
        String editedRequestor = requestor + "_UPDATED";
        DateTime editedExpiration = token.getExpirationTime().plusDays(1);

        token.setOwner(editedOwner);
        token.setRequestor(editedRequestor);
        token.setExpirationTime(editedExpiration);

        repo.updateToken(token);

        RefreshToken updatedToken = repo.findByTokenString(token
            .getTokenString());

        Assert.assertEquals(editedOwner, updatedToken.getOwner());
        Assert.assertEquals(editedRequestor, updatedToken.getRequestor());
        Assert.assertEquals(editedExpiration, updatedToken.getExpirationTime());

        repo.delete(token.getTokenString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateIfNull() {
        RefreshToken token = null;
        repo.updateToken(token);
    }

    @Test
    public void shouldNotUpdateIfNoChanges() {

        RefreshToken token = addNewTestToken();

        String editedOwner = token.getOwner();
        String editedRequestor = token.getRequestor();
        DateTime editedExpiration = token.getExpirationTime();

        token.setOwner(editedOwner);
        token.setRequestor(editedRequestor);
        token.setExpirationTime(editedExpiration);

        repo.updateToken(token);

        RefreshToken updatedToken = repo.findByTokenString(token
            .getTokenString());

        Assert.assertEquals(token, updatedToken);

        repo.delete(token.getTokenString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateIfNotFound() {

        RefreshToken testToken = new RefreshToken("I_DONT_EXIST",
            new DateTime(), owner, requestor);

        repo.updateToken(testToken);
    }

    @After
    public void tearDown() {
        cleanUpData();
        connPools.close();
    }

    protected RefreshToken addNewTestToken() {
        RefreshToken newToken = createTestTokenInstance();
        repo.save(newToken);
        return newToken;
    }

    private RefreshToken createTestTokenInstance() {
        RefreshToken newToken = new RefreshToken("DELETE_My_Token",
            LdapRefreshTokenRepository.DATE_PARSER
                .parseDateTime(("20201231210627.3Z")), owner, requestor);
        return newToken;
    }
}
