package com.rackspace.idm.domain.dao.impl;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Modification;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTimeZone;
import org.junit.*;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class LdapApplicationRepositoryIntegrationTest extends InMemoryLdapIntegrationTest{

    @Autowired
    private LdapUserRepository userRepo;
    @Autowired
    private LdapApplicationRepository repo;
    @Autowired
    private LdapConnectionPools connPools;
    
    String id = "XXXX";
    
    String userDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=users,o=rackspace,dc=rackspace,dc=com";
    private ClientGroup clientGroup;

    @Before
    public void preTestCleanUp() {
        Application deleteme = repo.getClientByClientId("DELETE_My_ClientId");
        User deleteme2 = userRepo.getUserById("XXXX");
        if (deleteme != null) {
            repo.deleteClient(deleteme);
        }
        if (deleteme2 != null) {
            userRepo.deleteUser(deleteme2.getUsername());
        }
    }

    @Test
    public void shouldNotAcceptNullOrBlankClientname() {
        try {
            repo.getClientByClientname(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getClientByClientname("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addClient(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            Assert.assertNull(repo.getClientByClientId("     "));
        } catch (IllegalArgumentException e) {
            Assert.fail("Should have returned null.");
        }

        try {
            repo.getClientById("    ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.updateClient(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneClientThatExists() {
        Application client = repo.getClientByClientId("ABCDEF");
        Assert.assertNotNull(client);
        Assert.assertEquals("ABCDEF", client.getClientId());
    }

    @Test
    public void shouldNotFindClientThatDoesNotExist() {
        Application client = repo.getClientByClientname("hi. i don't exist.");
        Assert.assertNull(client);
    }

    @Test
    public void shouldRetrieveAllClientsThatExist() {
        List<Application> clients = repo.getAllClients();
        Assert.assertTrue(clients.size() >= 2);
    }

    @Test
    public void shouldAddNewClient() {
        Application newClient = addNewTestClient();
        Application checkClient = repo.getClientByClientId(newClient.getClientId());
        Assert.assertNotNull(checkClient);
        Assert.assertEquals("DELETE_My_Name", checkClient.getName());
        repo.deleteClient(newClient);
    }

    @Test
    public void shouldDeleteClient() {
        Application newClient = addNewTestClient();
        repo.deleteClient(newClient);
        Application idontexist = repo.getClientByClientname(newClient.getName());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldUpdateNonDnAttrOfClient() {
    	String secret = "My_New_Secret";
        Application newClient = addNewTestClient();
        String clientId = newClient.getClientId();
        ClientSecret clientSecret = ClientSecret.newInstance(secret);

        // Update all non-DN attributes
        newClient.setClientSecretObj(clientSecret);

        try {
            repo.updateClient(newClient);
        } catch (IllegalStateException e) {
            repo.deleteClient(newClient);
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        Application changedClient = repo.getClientByClientId(clientId);

        repo.deleteClient(newClient);
        Assert.assertEquals(clientSecret, changedClient.getClientSecretObj());
    }
     
    @Ignore //TODO: use correct client id
    @Test
    public void shouldAuthenticateForCorrectCredentials() {
        ClientAuthenticationResult authenticated = repo.authenticate("18e7a7032733486cd32f472d7bd58f709ac0d221", "Password1");
        Assert.assertTrue(authenticated.isAuthenticated());
    }

    @Test
    public void shouldGenerateModifications() {
        Application client = createTestClientInstance();
        Application cClient = createTestClientInstance();
        cClient.setName("changed_client_name");
        cClient.setClientSecretObj(ClientSecret
            .newInstance("changed_client_secret"));

        List<Modification> mods = null;;
        try {
            mods = repo.getModifications(client, cClient);
        } catch (InvalidCipherTextException e) {
        } catch (GeneralSecurityException e) {
        }

        Assert.assertEquals(2, mods.size());
        Assert.assertEquals("changed_client_secret", mods.get(0).getAttribute()
            .getValue());
    }

    @Test
    public void shouldGetClientWithDefinedScopeAccess() {
        List<Application> clients = repo.getAvailableScopes();
        Assert.assertTrue(clients.size() > 0);
    }
    
    private Application addNewTestClient() {
        Application newClient = createTestClientInstance();
        repo.addClient(newClient);
        return newClient;
    }
    
    private Application createTestClientInstance() {
        Application newClient = new Application("DELETE_My_ClientId", ClientSecret
            .newInstance("DELETE_My_Client_Secret"), "DELETE_My_Name", "RCN-123-456-789");
        return newClient;
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUniqueId(userDN);
        return user;
    }
    
    private User addNewTestUser() {
        User newUser = createTestUserInstance();
        userRepo.addUser(newUser);
        return newUser;
    }

    private User createTestUserInstance() {
        // Password pwd = Password.newInstance("password_to_delete");
        Password pwd = Password.generateRandom(false);
        User newUser = new User("deleteme", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!","1235"));
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
}
