package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Modification;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTimeZone;
import org.junit.*;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;

public class LdapApplicationRepositoryTest {

    private LdapUserRepository userRepo;
    private LdapApplicationRepository repo;
    private LdapConnectionPools connPools;
    
    String id = "XXXX";
    
    String userDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=users,o=rackspace,dc=rackspace,dc=com";
    String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,ou=customers,o=rackspace,dc=rackspace,dc=com";
    
    @Before
    public void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapApplicationRepository cleanUpRepo = getRepo(pools);
        LdapUserRepository cleanUpRepo2 = getUserRepo(pools);
        Application deleteme = cleanUpRepo.getClientByClientId("DELETE_My_ClientId");
        User deleteme2 = cleanUpRepo2.getUserById("XXXX");
        if (deleteme != null) {
            cleanUpRepo.deleteClient(deleteme);
        }
        if (deleteme2 != null) {
            cleanUpRepo2.deleteUser(deleteme2.getUsername());
        }
        pools.close();
    }

    private static LdapApplicationRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapApplicationRepository(connPools, appConfig);
    }
    
    private static LdapUserRepository getUserRepo(LdapConnectionPools connPools) {
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
        return new LdapConfiguration(appConfig).connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
        userRepo = getUserRepo(connPools);
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
            repo.getClientByClientId("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
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
     
    @Test
    public void shouldAuthenticateForCorrectCredentials() {
        ClientAuthenticationResult authenticated = repo.authenticate("18e7a7032733486cd32f472d7bd58f709ac0d221", "password1");
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Assert.assertEquals(2, mods.size());
        Assert.assertEquals("changed_client_secret", mods.get(0).getAttribute()
            .getValue());
    }

    @Test
    public void shouldAddClientGroup() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group, testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldUpdateClientGroup() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        group = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        group.setType("My New Type");
        repo.updateClientGroup(group);
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
        
        Assert.assertNotNull(returnedGroup);
        Assert.assertTrue(returnedGroup.getType().equalsIgnoreCase("My New Type"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateClientGroup() {
        repo.updateClientGroup(null);
    }
    
    @Test
    public void shouldNotAddClientGroupForDuplicate() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        try {
            repo.addClientGroup(group,testClient.getUniqueId());
            Assert.fail("Shouldn't Have added Group");
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof DuplicateClientGroupException);
        }
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldDeleteClientGroup() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group);
        returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNull(returnedGroup);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldGetClientGroup() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldGetClientGroups() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        group.setName("NEWGROUPNAME");
        repo.addClientGroup(group,testClient.getUniqueId());
        List<ClientGroup> groups = repo.getClientGroupsByClientId(testClient.getClientId());
        Assert.assertTrue(groups.size() == 2);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test 
    public void shouldThowNotFoundErrorForNonExistentClient() {
        try {
        repo.getClientGroupsByClientId("SOMEBADCLIENTID");
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof NotFoundException);
        }
    }
    
    @Test 
    public void shouldNotGetClientGroupForNonExistenClient() {
        ClientGroup returnedGorup = repo.getClientGroup("RACKSPACE", "BADCLIENTNAME", "name");
        Assert.assertNull(returnedGorup);
    }
    
    @Test
    public void shouldReturnNullForGetClientGroupForNonExistentGroup() {
        Application testClient = addNewTestClient();
        ClientGroup returnedGroup = repo.getClientGroup(testClient.getRCN(), testClient.getClientId(), "SOMEBADNAME");
        Assert.assertNull(returnedGroup);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldAddUserToClientGroup() {
        User user = addNewTestUser();
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        boolean inGroup = repo.isUserInClientGroup(user.getUsername(), group.getUniqueId());
        repo.deleteClientGroup(group);
        userRepo.removeUsersFromClientGroup(group);
        repo.deleteClient(testClient);
        userRepo.deleteUser(user.getUsername());
        Assert.assertTrue(inGroup);
    }
    
    @Test
    public void shouldNotAddUserToClientGroupIfAlreadyInGroup() {
        User user = addNewTestUser();
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        try {
            repo.addUserToClientGroup(user.getUniqueId(), group);
            Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof DuplicateException);
        }
        repo.deleteClientGroup(group);
        userRepo.removeUsersFromClientGroup(group);
        repo.deleteClient(testClient);
        userRepo.deleteUser(user.getUsername());
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForNullUser() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.addUserToClientGroup(null, group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForUserWithBlankUniqueID() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.addUserToClientGroup(new User().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForNullGroup() {
        Application testClient = addNewTestClient();
        try {
        	repo.addUserToClientGroup(createTestUser().getUniqueId(), null);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForGroupWithBlankUniqueID() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        group.setUniqueId(null);
        try {
        repo.addUserToClientGroup(createTestUser().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldRemoveUserFromClientGroup() {

        User user = addNewTestUser();
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        repo.removeUserFromGroup(user.getUniqueId(), group);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
        userRepo.deleteUser(user.getUsername());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupIfUserNotInGroup() {

        User user = addNewTestUser();
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        try {
        repo.removeUserFromGroup(user.getUniqueId(), group);
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof NotFoundException);
        }
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
        userRepo.deleteUser(user.getUsername());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForNullUser() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.removeUserFromGroup(null, group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForUserWithBlankUniqueID() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.removeUserFromGroup(new User().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForNullGroup() {
        Application testClient = addNewTestClient();
        try {
        repo.removeUserFromGroup(createTestUser().getUniqueId(), null);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForGroupWithBlankUniqueID() {
        Application testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        group.setUniqueId(null);
        try {
        repo.removeUserFromGroup(createTestUser().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldGetClientWithDefinedScopeAccess() {
        List<Application> clients = repo.getAvailableScopes();
        Assert.assertTrue(clients.size() > 0);
    }
    

    @After
    public void tearDown() {
        connPools.close();
    }

    private Application addNewTestClient() {
        Application newClient = createTestClientInstance();
        repo.addClient(newClient);
        return newClient;
    }
    
    private ClientGroup createNewTestClientGroup(Application client) {
        return new ClientGroup (client.getClientId(), client.getRCN(), "New Group", "TYPE");
    }

    private Application createTestClientInstance() {
        Application newClient = new Application("DELETE_My_ClientId", ClientSecret
            .newInstance("DELETE_My_Client_Secret"), "DELETE_My_Name", "RCN-123-456-789", ClientStatus.ACTIVE);
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
            "Yellow. No, Blue! Arrrrgh!"));
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
