package com.rackspace.idm.domain.dao.impl;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserCredential;
import com.rackspace.idm.domain.entity.UserHumanName;
import com.rackspace.idm.domain.entity.UserLocale;
import com.rackspace.idm.domain.entity.UserStatus;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Modification;

public class LdapClientRepositoryTest {

    private LdapUserRepository userRepo;
    private LdapClientRepository repo;
    private LdapConnectionPools connPools;
    
    String userDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=people,o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";
    String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";
    
    @BeforeClass
    public static void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapClientRepository cleanUpRepo = getRepo(pools);
        Client deleteme = cleanUpRepo.getClientByClientId("DELETE_My_ClientId");
        if (deleteme != null) {
            cleanUpRepo.deleteClient(deleteme);
        }
        pools.close();
    }

    private static LdapClientRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapClientRepository(connPools, appConfig);
    }
    
    private static LdapUserRepository getUserRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapUserRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration()
            .getConfigFromClasspath()).connectionPools();
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
            repo.addClient(null, testCustomerDN);
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
            repo.getClientByInum("    ");
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
        Client client = repo.getClientByClientId("ABCDEF");
        Assert.assertNotNull(client);
        Assert.assertEquals("ABCDEF", client.getClientId());
    }

    @Test
    public void shouldNotFindClientThatDoesNotExist() {
        Client client = repo.getClientByClientname("hi. i don't exist.");
        Assert.assertNull(client);
    }

    @Test
    public void shouldRetrieveAllClientsThatExist() {
        List<Client> clients = repo.getAllClients();
        Assert.assertTrue(clients.size() >= 2);
    }

    @Test
    public void shouldAddNewClient() {
        Client newClient = addNewTestClient();
        Client checkClient = repo.getClientByClientId(newClient.getClientId());
        Assert.assertNotNull(checkClient);
        Assert.assertEquals("DELETE_My_Name", checkClient.getName());
        repo.deleteClient(newClient);
    }

    @Test
    public void shouldDeleteClient() {
        Client newClient = addNewTestClient();
        repo.deleteClient(newClient);
        Client idontexist = repo.getClientByClientname(newClient.getName());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldUpdateNonDnAttrOfClient() {
        Client newClient = addNewTestClient();
        String clientId = newClient.getClientId();

        // Update all non-DN attributes
        newClient.setClientSecretObj(ClientSecret.newInstance("My_New_Secret"));
        newClient.setStatus(ClientStatus.INACTIVE);

        try {
            repo.updateClient(newClient);
        } catch (IllegalStateException e) {
            repo.deleteClient(newClient);
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        Client changedClient = repo.getClientByClientId(clientId);
        Assert.assertTrue(changedClient.equals(newClient));

        repo.deleteClient(newClient);
    }
     
    @Test
    public void shouldAuthenticateForCorrectCredentials() {
        ClientAuthenticationResult authenticated = repo.authenticate("ABCDEF", "password");
        Assert.assertTrue(authenticated.isAuthenticated());
    }

    @Test
    public void shouldGenerateModifications() {
        Client client = createTestClientInstance();
        Client cClient = createTestClientInstance();
        cClient.setName("changed_client_name");
        cClient.setClientSecretObj(ClientSecret
            .newInstance("changed_client_secret"));
        cClient.setStatus(ClientStatus.INACTIVE);

        List<Modification> mods = repo.getModifications(client, cClient);

        Assert.assertEquals(2, mods.size());
        Assert.assertEquals("changed_client_secret", mods.get(0).getAttribute()
            .getValue());
        Assert.assertEquals(ClientStatus.INACTIVE.toString(), mods.get(1)
            .getAttribute().getValue());
    }

    @Test
    public void shouldGetUnusedClientInum() {
        String inum = repo
            .getUnusedClientInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        Assert.assertFalse(inum.equals("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!2222"));
    }

    @Test
    public void shouldSetAllClientLocked() {
        Client newClient = addNewTestClient();
        repo.setClientsLockedFlagByCustomerId(newClient.getCustomerId(), true);
        Client changedClient = repo.getClientByClientId(newClient.getClientId());
        Assert.assertEquals(changedClient.isLocked(), true);
        repo.deleteClient(newClient);
    }
    
    @Test
    public void shouldAddClientGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group, testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldUpdateClientGroup() {
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldGetClientGroups() {
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
        ClientGroup returnedGroup = repo.getClientGroup(testClient.getCustomerId(), testClient.getClientId(), "SOMEBADNAME");
        Assert.assertNull(returnedGroup);
        repo.deleteClient(testClient);
    }
    
    @Test
    public void shouldAddUserToClientGroup() {
        User user = addNewTestUser();
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
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
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
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
        Client testClient = addNewTestClient();
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
        List<Client> clients = repo.getAvailableScopes();
        Assert.assertTrue(clients.size() > 0);
    }
    

    @After
    public void tearDown() {
        connPools.close();
    }

    private Client addNewTestClient() {
        Client newClient = createTestClientInstance();
        repo.addClient(newClient, testCustomerDN);
        return newClient;
    }
    
    private ClientGroup createNewTestClientGroup(Client client) {
        return new ClientGroup (client.getClientId(), client.getCustomerId(), "New Group", "TYPE");
    }

    private Client createTestClientInstance() {
        Random ran = new Random();
        //int random = ran.nextInt();
        Client newClient = new Client("DELETE_My_ClientId", ClientSecret
            .newInstance("DELETE_My_Client_Secret"), "DELETE_My_Name", "inum",
            "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        newClient.setLocked(false);
        newClient.setSoftDeleted(false);
        return newClient;
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUniqueId(userDN);
        return user;
    }
    
    private User addNewTestUser() {
        User newUser = createTestUserInstance();
        userRepo.addUser(newUser, testCustomerDN);
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
}
